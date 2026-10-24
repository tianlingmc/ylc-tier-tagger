package com.ylcmc.tiertagger.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 模组配置。自研存储，不依赖 cloth-config。
 *
 * <p>配置以 JSON 存在 config/ylc-tier-tagger/settings.json。
 */
public class TierConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TierConfig instance = new TierConfig();

    private Path file;

    /** 总开关。 */
    public boolean enabled = true;

    /** 是否在 Tab 玩家列表里显示段位。 */
    public boolean showOnTab = true;

    /** 段位显示在名字左边还是右边。 */
    public RenderLocation renderLocation = RenderLocation.LEFT;

    /** 优先使用的模式。 */
    public Mode selectedMode = Mode.AXE;

    /** 档位选取规则。 */
    public DisplayRule displayRule = DisplayRule.HIGHEST_ONLY;

    public int HT1 = 0xe8ba3a;
    public int LT1 = 0xd5b355;
    public int HT2 = 0xc4d3e7;
    public int LT2 = 0xa0a7b2;
    public int HT3 = 0xf89f5a;
    public int LT3 = 0xc67b42;
    public int HT4 = 0x81749a;
    public int LT4 = 0x655b79;
    public int HT5 = 0x8f82a8;
    public int LT5 = 0x655b79;

    public static TierConfig get() {
        return instance;
    }

    /** 读取配置文件；读不到就用默认值。 */
    public static void load() {
        TierConfig cfg = new TierConfig();
        cfg.file = FabricLoader.getInstance()
                .getConfigDir()
                .resolve("ylc-tier-tagger")
                .resolve("settings.json");
        try {
            if (Files.isRegularFile(cfg.file)) {
                try (Reader reader = Files.newBufferedReader(cfg.file, StandardCharsets.UTF_8)) {
                    TierConfig loaded = GSON.fromJson(reader, TierConfig.class);
                    if (loaded != null) {
                        loaded.file = cfg.file;
                        instance = loaded;
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ylc-tier-tagger] 读取配置失败，改用默认值：" + e);
        }
        instance = cfg;
    }

    /** 写回配置文件。 */
    public void save() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (Exception e) {
            System.err.println("[ylc-tier-tagger] 写入配置失败：" + e);
        }
    }

    /** 取段位对应的颜色。 */
    public int colorOf(String tier) {
        if (tier == null) return 0xffffff;
        switch (tier.trim().toUpperCase()) {
            case "HT1": return HT1;
            case "LT1": return LT1;
            case "HT2": return HT2;
            case "LT2": return LT2;
            case "HT3": return HT3;
            case "LT3": return LT3;
            case "HT4": return HT4;
            case "LT4": return LT4;
            case "HT5": return HT5;
            case "LT5": return LT5;
            default: return 0xffffff;
        }
    }

    /** 写入某个段位的颜色。 */
    public void setColor(String tier, int rgb) {
        switch (tier.trim().toUpperCase()) {
            case "HT1": HT1 = rgb; break;
            case "LT1": LT1 = rgb; break;
            case "HT2": HT2 = rgb; break;
            case "LT2": LT2 = rgb; break;
            case "HT3": HT3 = rgb; break;
            case "LT3": LT3 = rgb; break;
            case "HT4": HT4 = rgb; break;
            case "LT4": LT4 = rgb; break;
            case "HT5": HT5 = rgb; break;
            case "LT5": LT5 = rgb; break;
            default: break;
        }
    }

    /** 全部段位的名字，界面用。 */
    public static final String[] TIER_NAMES = {
            "HT1", "LT1", "HT2", "LT2", "HT3", "LT3", "HT4", "LT4", "HT5", "LT5"
    };

    public enum RenderLocation {
        LEFT("名字左侧"),
        RIGHT("名字右侧");

        public final String label;

        RenderLocation(String label) {
            this.label = label;
        }
    }

    public enum DisplayRule {
        SELECTED_ONLY("只用选中的模式"),
        HIGHEST_ONLY("只显示最高的档位"),
        MIXED("优先选中的模式");

        public final String label;

        DisplayRule(String label) {
            this.label = label;
        }
    }

    /** 模式枚举，名称与站点接口的 mode key 一一对应。 */
    public enum Mode {
        AXE("axe", "斧"),
        MACE("mace", "锤"),
        NETHOP("nethop", "合金"),
        POT("pot", "药水"),
        SMP("smp", "SMP"),
        SWORD("sword", "剑"),
        UHC("uhc", "UHC"),
        VANILLA("vanilla", "水晶");

        public final String key;
        public final String label;

        Mode(String key, String label) {
            this.key = key;
            this.label = label;
        }
    }
}
