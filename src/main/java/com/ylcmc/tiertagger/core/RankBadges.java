package com.ylcmc.tiertagger.core;

/**
 * 积分等级徽章。
 *
 * <p>阈值与排行榜站点 {@code config/site.php} 的 {@code rank_titles} 完全一致（按 min 降序）：
 * 400 Combat Grandmaster / 250 Combat Master / 100 Combat Ace / 50 Combat Specialist /
 * 20 Combat Cadet / 10 Combat Novice / 0 Rookie。
 *
 * <p>图标是位图字体里的私用区码位 E70A ~ E710，素材取自站点
 * {@code card-icons/badge-*.png}，在 {@code assets/minecraft/font/default.json} 里注册。
 */
public final class RankBadges {

    private static final int[] MIN = {400, 250, 100, 50, 20, 10, 0};

    private static final char[] ICON = {
            '\uE70A', '\uE70B', '\uE70C', '\uE70D', '\uE70E', '\uE70F', '\uE710'
    };

    private RankBadges() {
    }

    /** 按积分取对应的等级徽章码位。 */
    public static char iconFor(int points) {
        for (int i = 0; i < MIN.length; i++) {
            if (points >= MIN[i]) {
                return ICON[i];
            }
        }
        return ICON[ICON.length - 1];
    }
}
