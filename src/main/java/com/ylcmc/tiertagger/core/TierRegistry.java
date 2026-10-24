package com.ylcmc.tiertagger.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模式注册表：把站点接口的 mode key 映射到游戏内显示用的名字与图标码位。
 *
 * <p>图标是位图字体里的私用区字符，通过 assets/minecraft/font/default.json 注册。
 */
public final class TierRegistry {

    /** 模式 key -> 显示名 + 图标码位。key 与 pvptierweb 的 config/site.php 保持一致。 */
    private static final Map<String, Mode> MODES = new LinkedHashMap<>();

    public static final class Mode {
        public final String label;
        public final char icon;

        Mode(String label, char icon) {
            this.label = label;
            this.icon = icon;
        }
    }

    static {
        MODES.put("axe", new Mode("斧", '\uE701'));
        MODES.put("mace", new Mode("锤", '\uE702'));
        MODES.put("nethop", new Mode("合金", '\uE703'));
        MODES.put("pot", new Mode("药水", '\uE704'));
        MODES.put("smp", new Mode("SMP", '\uE705'));
        MODES.put("sword", new Mode("剑", '\uE706'));
        MODES.put("uhc", new Mode("UHC", '\uE707'));
        MODES.put("vanilla", new Mode("水晶", '\uE708'));
    }

    private TierRegistry() {
    }

    public static Mode get(String key) {
        return key == null ? null : MODES.get(key);
    }

    public static Map<String, Mode> all() {
        return MODES;
    }

    /**
     * 段位强度序，值小者强。
     * 与站点 modeStrength() 一致：HT1 最强，LT5 最弱。
     */
    private static final String[] STRENGTH_ORDER = {
            "HT1", "LT1", "HT2", "LT2", "HT3", "LT3", "HT4", "LT4", "HT5", "LT5"
    };

    /** 返回段位的强度序号，未知段位返回 Integer.MAX_VALUE（视为最弱）。 */
    public static int strengthOf(String tier) {
        if (tier == null) return Integer.MAX_VALUE;
        String t = tier.trim().toUpperCase();
        for (int i = 0; i < STRENGTH_ORDER.length; i++) {
            if (STRENGTH_ORDER[i].equals(t)) return i;
        }
        return Integer.MAX_VALUE;
    }

    /** 段位是否合法（HT1..HT5 / LT1..LT5）。 */
    public static boolean isValidTier(String tier) {
        if (tier == null) return false;
        String t = tier.trim().toUpperCase();
        for (String s : STRENGTH_ORDER) {
            if (s.equals(t)) return true;
        }
        return false;
    }
}
