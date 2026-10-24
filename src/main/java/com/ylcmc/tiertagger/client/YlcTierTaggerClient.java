package com.ylcmc.tiertagger.client;

import com.mojang.brigadier.CommandDispatcher;
import com.ylcmc.tiertagger.core.TierConfig;
import com.ylcmc.tiertagger.core.TierService;
import com.ylcmc.tiertagger.modmenu.ClothConfigBootstrap;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 客户端入口：读配置、启动时拉一次排行榜。
 *
 * <p>每 tick 的处理放在 {@link #onTick(MinecraftClient)}，由 MinecraftClientMixin 调用，
 * 这样不需要依赖 fabric-api 的事件总线。
 */
public class YlcTierTaggerClient implements ClientModInitializer {

    /** 全局唯一的排行榜数据服务。 */
    public static final TierService SERVICE = new TierService();

    /** 每 60 秒检查一次缓存是否过期。 */
    private static final int REFRESH_CHECK_TICKS = 1200;

    /** 同步超过这么多个 tick（10 秒）还没结果，就先提示一次「正在同步」。 */
    private static final int LOADING_HINT_TICKS = 200;

    private static int tickCounter = 0;
    private static int loadingTicks = 0;

    /** 上一次已经提示过的状态，用来避免重复刷屏。 */
    private static TierService.Status lastNotified = null;

    @Override
    public void onInitializeClient() {
        // 可选集成：装了 cloth-config 就注册一份，共用同一个配置文件
        ClothConfigBootstrap.register();
        TierConfig.load();
        // 进程启动即拉一次最新数据，本地缓存只作兜底
        SERVICE.bootstrap();
    }

    /** 每客户端 tick 调用一次。 */
    public static void onTick(MinecraftClient client) {
        registerCommand(client);

        // 玩家可能在 cloth-config 的界面上改了设置，跟着同步一次
        TierConfig.reloadIfChanged();

        if (client.player == null) {
            // 还在主菜单，聊天消息发不出去，等进了游戏再提示
            return;
        }

        checkStatus(client);

        tickCounter++;
        if (tickCounter < REFRESH_CHECK_TICKS) {
            return;
        }
        tickCounter = 0;
        if (SERVICE.isExpired()) {
            SERVICE.refreshAsync();
        }
    }

    /**
     * 状态变化就提示玩家。
     *
     * <p>同步成功、同步失败都会提示；失败时除了右上角提示，还在聊天栏里发一条，
     * 免得提示一闪而过看不见。
     */
    private static void checkStatus(MinecraftClient client) {
        TierService.Status status = SERVICE.status();

        if (status == TierService.Status.LOADING) {
            loadingTicks++;
            if (loadingTicks == LOADING_HINT_TICKS) {
                notifyStatus(client, status);
            }
            return;
        }

        if (status == TierService.Status.IDLE || status == lastNotified) {
            return;
        }

        lastNotified = status;
        loadingTicks = 0;
        notifyStatus(client, status);
    }

    /**
     * 客户端命令注册到游戏自带的 dispatcher 上。
     *
     * <p>不能只按「换没换网络处理器」判断：服务端每次下发命令树（进服、切服、权限刷新）
     * 都会通过 {@code onCommandTree} 把 dispatcher 换成一个新实例，之前注册的指令会被
     * 整个冲掉，表现就是输入 /ylctier 提示指令不存在。所以这里直接看根结点上有没有
     * 我们的指令，缺了就重新注册一次。
     */
    private static void registerCommand(MinecraftClient client) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null) {
            return;
        }
        try {
            CommandDispatcher<?> dispatcher = handler.getCommandDispatcher();
            if (dispatcher == null) {
                return;
            }
            if (dispatcher.getRoot().getChild(YlcTierCommand.NAME) != null) {
                return;
            }
            dispatcher.register(YlcTierCommand.build());
        } catch (Exception e) {
            System.err.println("[ylc-tier-tagger] 注册客户端命令失败：" + e);
        }
    }

    /** 把当前数据状态告诉玩家。 */
    private static void notifyStatus(MinecraftClient client, TierService.Status status) {
        String title = "PVPTIER 榜单";

        switch (status) {
            case OK: {
                long age = SERVICE.dataAgeMinutes();
                String body = "已同步 " + SERVICE.size() + " 名玩家的段位"
                        + (age > 0 ? "（数据来自 " + age + " 分钟前的缓存）" : "");
                showToast(client, title, body);
                break;
            }
            case FAILED: {
                String reason = SERVICE.lastError() == null ? "未知原因" : SERVICE.lastError();
                showToast(client, title, "同步失败：" + reason);
                chat(client, Text.literal("[PVPTIER] 排行榜同步失败：" + reason)
                        .copy().styled(s -> s.withColor(Formatting.RED)));
                chat(client, Text.literal("  " + cacheHint())
                        .copy().styled(s -> s.withColor(Formatting.GRAY)));
                break;
            }
            case LOADING:
                showToast(client, title, "正在同步排行榜，网络较慢时可稍后用 /ylctier retrieve 重试");
                break;
            default:
                break;
        }
    }

    /** 失败时说明现在是什么情况、可以怎么办。 */
    private static String cacheHint() {
        String base;
        if (SERVICE.hasData()) {
            long age = SERVICE.dataAgeMinutes();
            base = "正在使用本地缓存（" + SERVICE.size() + " 人"
                    + (age >= 0 ? "，" + age + " 分钟前" : "") + "）";
        } else {
            base = "没有可用的本地缓存，段位标签暂时不会显示";
        }
        return base + "。可输入 /ylctier retrieve 手动重试。";
    }

    /**
     * 弹一个系统提示。
     *
     * <p>1.20.5 之前没有 toast 系统，那时退回聊天栏消息。
     */
    private static void showToast(MinecraftClient client, String title, String body) {
        try {
            client.getToastManager().add(new SystemToast(
                    SystemToast.Type.PERIODIC_NOTIFICATION,
                    Text.literal(title),
                    Text.literal(body)));
        } catch (Throwable t) {
            chat(client, Text.literal("[" + title + "] " + body));
        }
    }

    /** 往聊天栏发一条消息。 */
    private static void chat(MinecraftClient client, Text text) {
        if (client.player != null) {
            client.player.sendMessage(text, false);
        }
    }
}
