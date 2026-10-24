package com.ylcmc.tiertagger.core;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * 模组配置。装了 cloth-config 之后，游戏里用 /config 命令打开。
 */
@Config(name = "ylc-tier-tagger")
public class TierConfig implements ConfigData {

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

    @ConfigEntry.ColorPicker
    public int HT1 = 0xe8ba3a;
    @ConfigEntry.ColorPicker
    public int LT1 = 0xd5b355;
    @ConfigEntry.ColorPicker
    public int HT2 = 0xc4d3e7;
    @ConfigEntry.ColorPicker
    public int LT2 = 0xa0a7b2;
    @ConfigEntry.ColorPicker
    public int HT3 = 0xf89f5a;
    @ConfigEntry.ColorPicker
    public int LT3 = 0xc67b42;
    @ConfigEntry.ColorPicker
    public int HT4 = 0x81749a;
    @ConfigEntry.ColorPicker
    public int LT4 = 0x655b79;
    @ConfigEntry.ColorPicker
    public int HT5 = 0x8f82a8;
    @ConfigEntry.ColorPicker
    public int LT5 = 0x655b79;

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

    public enum RenderLocation {
        LEFT("名字左侧"),
        RIGHT("名字右侧");

        private final String label;

        RenderLocation(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public enum DisplayRule {
        /** 只用选定模式；该模式没档位就不显示。 */
        SELECTED_ONLY("只用选中的模式"),
        /** 只显示该玩家档位最高的模式。 */
        HIGHEST_ONLY("只显示最高的档位"),
        /** 优先选定模式，没有则用最高的。 */
        MIXED("优先选中的模式");

        private final String label;

        DisplayRule(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
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

        @Override
        public String toString() {
            return label;
        }
    }
}
