package com.ylcmc.tiertagger.core;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * 负责把「模式图标 + 段位」拼到玩家名字旁边。
 */
public final class TierLabel {

    /** 选中的模式与段位。 */
    public static final class Selected {
        public final String modeKey;
        public final String tier;

        Selected(String modeKey, String tier) {
            this.modeKey = modeKey;
            this.tier = tier;
        }
    }

    private TierLabel() {
    }

    /**
     * 按配置规则给一个玩家选出要显示的模式与段位。
     *
     * @return 没得选时返回 null
     */
    public static Selected pick(PlayerEntry entry, TierConfig cfg) {
        if (entry == null || entry.tiers == null) {
            return null;
        }

        switch (cfg.displayRule) {
            case SELECTED_ONLY: {
                String tier = entry.tierOf(cfg.selectedMode.key);
                return TierRegistry.isValidTier(tier) ? new Selected(cfg.selectedMode.key, tier) : null;
            }
            case MIXED: {
                String tier = entry.tierOf(cfg.selectedMode.key);
                if (TierRegistry.isValidTier(tier)) {
                    return new Selected(cfg.selectedMode.key, tier);
                }
                return pickHighest(entry);
            }
            case HIGHEST_ONLY:
            default:
                return pickHighest(entry);
        }
    }

    /** 选该玩家所有模式里段位最强的一个（强度序值最小）。 */
    private static Selected pickHighest(PlayerEntry entry) {
        String bestMode = null;
        String bestTier = null;
        int bestStrength = Integer.MAX_VALUE;

        List<TierEntry> tiers = entry.tiers;
        for (TierEntry t : tiers) {
            if (t == null || !TierRegistry.isValidTier(t.tier)) {
                continue;
            }
            int strength = TierRegistry.strengthOf(t.tier);
            if (strength < bestStrength) {
                bestStrength = strength;
                bestMode = t.mode;
                bestTier = t.tier;
            }
        }
        return bestMode == null ? null : new Selected(bestMode, bestTier);
    }

    /**
     * 生成「图标 + 段位」这一小段标签。
     *
     * @return 模式不在注册表里时返回 null
     */
    public static MutableText badge(Selected selected, TierConfig cfg) {
        if (selected == null) return null;
        TierRegistry.Mode mode = TierRegistry.get(selected.modeKey);
        if (mode == null) return null;

        MutableText badge = Text.literal(String.valueOf(mode.icon)).copy()
                .styled(s -> s.withColor(Formatting.WHITE));
        badge.append(Text.literal(selected.tier).copy()
                .styled(s -> s.withColor(cfg.colorOf(selected.tier))));
        return badge;
    }

    /**
     * 把段位标签拼到原始名字上。
     *
     * @param original 原始玩家名
     * @return 拼好后的文本；没有段位时原样返回
     */
    public static Text apply(Text original, PlayerEntry entry, TierConfig cfg) {
        Selected selected = pick(entry, cfg);
        if (selected == null) {
            return original;
        }

        TierRegistry.Mode mode = TierRegistry.get(selected.modeKey);
        if (mode == null) {
            return original;
        }

        MutableText icon = Text.literal(String.valueOf(mode.icon)).copy()
                .styled(s -> s.withColor(Formatting.WHITE));
        MutableText tier = Text.literal(selected.tier).copy()
                .styled(s -> s.withColor(cfg.colorOf(selected.tier)));
        MutableText separator = Text.literal(" | ").copy().styled(s -> s.withColor(Formatting.GRAY));
        MutableText space = Text.literal(" ").copy();

        if (cfg.renderLocation == TierConfig.RenderLocation.LEFT) {
            return icon.copy().append(space).append(tier).append(separator).append(original);
        }
        return original.copy().append(separator).append(tier).append(space).append(icon);
    }
}
