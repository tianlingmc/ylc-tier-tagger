package com.ylcmc.tiertagger.core;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 从 PVPTIER 排行榜拉取玩家段位，并缓存在本地。
 *
 * <p>数据源是 pvptierweb 的 GET /api/list。接口一次最多返回 200 条，这里按 offset 翻页拉全量。
 */
public final class TierService {

    /** 排行榜接口（overall 综合榜）。 */
    private static final String API_URL = "https://pvptier.ylcmc.top/api/list?mode=overall&limit=200&offset=";

    private static final int PAGE_LIMIT = 200;
    private static final long CACHE_TTL_MS = 10L * 60L * 1000L;
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    private static final int HTTP_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 30_000L;

    private static final Gson GSON = new Gson();
    private static final Type CACHE_TYPE = new TypeToken<CacheFile>() {
    }.getType();

    /** 玩家名（小写）-> 记录。 */
    private final Map<String, PlayerEntry> players = new ConcurrentHashMap<>();

    private final Path cacheFile;
    private final ExecutorService executor;
    private final AtomicBoolean updating = new AtomicBoolean(false);

    private volatile long lastUpdate = 0L;

    /** 最近一次拉取的结果状态。 */
    public enum Status {
        /** 还没开始拉。 */
        IDLE,
        /** 正在拉。 */
        LOADING,
        /** 拉取成功。 */
        OK,
        /** 拉取失败。 */
        FAILED
    }

    private volatile Status status = Status.IDLE;
    private volatile String lastError = null;

    public TierService() {
        this.cacheFile = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("ylc-tier-tagger")
                .resolve("tier_cache.json");

        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "ylc-tier-tagger-fetch");
            t.setDaemon(true);
            return t;
        };
        this.executor = Executors.newSingleThreadExecutor(factory);
    }

    /* ─────────── 对外接口 ─────────── */

    /** 按玩家名查记录，不区分大小写；查不到返回 null。 */
    public PlayerEntry find(String name) {
        if (name == null) return null;
        return players.get(name.toLowerCase(Locale.ROOT));
    }

    /** 本地是否已有数据。 */
    public boolean hasData() {
        return !players.isEmpty();
    }

    public int size() {
        return players.size();
    }

    /** 缓存是否过期。 */
    public boolean isExpired() {
        return System.currentTimeMillis() - lastUpdate > CACHE_TTL_MS;
    }

    /** 最近一次拉取的状态。 */
    public Status status() {
        return status;
    }

    /** 最近一次拉取失败的原因，没有则为 null。 */
    public String lastError() {
        return lastError;
    }

    /**
     * 进程启动时调用：先读本地缓存兜底，再强制拉一次最新数据。
     *
     * <p>无论本地缓存是否还有效，这次都会重新请求接口。
     */
    public void bootstrap() {
        loadCache();
        refreshAsync();
    }

    /**
     * 触发一次后台刷新。同一时间只允许一个刷新任务在跑。
     *
     * @return 是否真的发起了刷新
     */
    public boolean refreshAsync() {
        if (!updating.compareAndSet(false, true)) {
            return false;
        }
        status = Status.LOADING;
        executor.submit(() -> {
            try {
                List<PlayerEntry> fresh = fetchAll();
                if (fresh == null) {
                    status = Status.FAILED;
                    lastError = "接口无响应或返回异常";
                    return;
                }
                players.clear();
                for (PlayerEntry p : fresh) {
                    if (p != null && p.name != null) {
                        players.put(p.name.toLowerCase(Locale.ROOT), p);
                    }
                }
                lastUpdate = System.currentTimeMillis();
                lastError = null;
                status = Status.OK;
                saveCache(fresh);
            } catch (Exception e) {
                status = Status.FAILED;
                lastError = String.valueOf(e.getMessage());
                System.err.println("[ylc-tier-tagger] 拉取排行榜失败：" + e);
            } finally {
                updating.set(false);
            }
        });
        return true;
    }

    /* ─────────── 网络 ─────────── */

    /** 翻页拉全量；任一页失败则整体返回 null（保留旧数据）。 */
    private List<PlayerEntry> fetchAll() {
        List<PlayerEntry> all = new ArrayList<>();
        int offset = 0;
        int total = Integer.MAX_VALUE;

        while (offset < total) {
            ApiResponse resp = request(API_URL + offset);
            if (resp == null || resp.data == null) {
                return null;
            }
            all.addAll(resp.data);
            total = resp.meta != null ? resp.meta.total : all.size();

            if (resp.data.size() < PAGE_LIMIT) {
                break;
            }
            offset += PAGE_LIMIT;
        }
        return all;
    }

    /** 单页请求，带重试；失败返回 null。 */
    private ApiResponse request(String url) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();

        for (int attempt = 1; attempt <= HTTP_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() != 200) {
                    throw new IOException("HTTP " + response.statusCode());
                }
                ApiResponse parsed = GSON.fromJson(response.body(), ApiResponse.class);
                if (parsed == null || !parsed.ok) {
                    throw new IOException("接口返回异常");
                }
                return parsed;
            } catch (Exception e) {
                System.err.println("[ylc-tier-tagger] 请求失败（第 " + attempt + " 次）：" + e.getMessage());
                if (attempt < HTTP_ATTEMPTS) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /* ─────────── 本地缓存 ─────────── */

    private static class CacheFile {
        long timestamp;
        List<PlayerEntry> players;
    }

    private void loadCache() {
        if (!Files.isRegularFile(cacheFile)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(cacheFile, StandardCharsets.UTF_8)) {
            CacheFile cache = GSON.fromJson(reader, CACHE_TYPE);
            if (cache == null || cache.players == null) {
                return;
            }
            for (PlayerEntry p : cache.players) {
                if (p != null && p.name != null) {
                    players.put(p.name.toLowerCase(Locale.ROOT), p);
                }
            }
            lastUpdate = cache.timestamp;
        } catch (Exception e) {
            System.err.println("[ylc-tier-tagger] 读取本地缓存失败：" + e);
        }
    }

    private void saveCache(List<PlayerEntry> data) {
        try {
            Files.createDirectories(cacheFile.getParent());
            CacheFile cache = new CacheFile();
            cache.timestamp = System.currentTimeMillis();
            cache.players = data;
            try (Writer writer = Files.newBufferedWriter(cacheFile, StandardCharsets.UTF_8)) {
                GSON.toJson(cache, writer);
            }
        } catch (Exception e) {
            System.err.println("[ylc-tier-tagger] 写入本地缓存失败：" + e);
        }
    }
}
