package com.ylcmc.tiertagger.gui;

import com.ylcmc.tiertagger.client.YlcTierTaggerClient;
import com.ylcmc.tiertagger.core.PlayerEntry;
import com.ylcmc.tiertagger.core.PlayerNames;
import com.ylcmc.tiertagger.core.RankBadges;
import com.ylcmc.tiertagger.core.TierConfig;
import com.ylcmc.tiertagger.core.TierEntry;
import com.ylcmc.tiertagger.core.TierRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 段位 HUD 面板。
 *
 * <p>显示对象是「30 米内离你最近的玩家」，没有这样的玩家时显示自己。
 *
 * <p>排版（一行一项，段位每行一个并带模式图标）：
 * <pre>
 * 当前距离你 12 米
 * EntityO   #1
 * 地区 / 台湾
 * [徽章] 210（Combat Ace）
 * OVERALL / 210 Points
 * [斧] 斧 HT2
 * [水晶] 水晶 LT4
 * ...
 * </pre>
 *
 * <p>位置来自配置里的比例坐标（{@link TierConfig#hudX} / {@link TierConfig#hudY}），
 * 由 {@link HudPositionScreen} 拖拽设定；按屏幕比例存，换分辨率或界面缩放都不会跑位。
 */
public final class TierHudPanel {

    private static final int PAD = 5;
    private static final int LINE_H = 10;
    private static final int MARGIN = 2;

    /** 找最近玩家的半径（米 / 格）。 */
    private static final double NEAR_RADIUS = 30.0;

    private static final int BG = 0xC0101010;
    private static final int BORDER_IDLE = 0x60FFFFFF;
    private static final int BORDER_NEAR = 0xFFFFC24A;

    /** 积分图标：位图字体 \uE709，素材取自排行榜站点的 card-icons/points.png。 */
    private static final String POINTS_ICON = "\uE709";

    private TierHudPanel() {
    }

    /** 在 HUD 上绘制；不满足条件时什么都不做。 */
    public static void render(DrawContext context, MinecraftClient client) {
        TierConfig cfg = TierConfig.get();
        if (!cfg.enabled || !cfg.showHud || client == null) {
            return;
        }
        if (client.player == null || client.world == null || client.currentScreen != null) {
            return;
        }

        PlayerEntity nearest = findNearest(client);
        PlayerEntry entry = null;
        Double distance = null;
        if (nearest != null) {
            entry = YlcTierTaggerClient.SERVICE.find(PlayerNames.of(nearest.getGameProfile()));
            if (entry != null) {
                distance = Math.sqrt(nearest.squaredDistanceTo(client.player));
            }
        }
        if (entry == null) {
            entry = YlcTierTaggerClient.SERVICE.find(PlayerNames.of(client.player.getGameProfile()));
        }
        if (entry == null) {
            return;
        }

        List<Text> lines = buildLines(entry, cfg, distance);
        TextRenderer textRenderer = client.textRenderer;

        int textW = 0;
        for (Text line : lines) {
            textW = Math.max(textW, textRenderer.getWidth(line));
        }
        int boxW = textW + PAD * 2;
        int boxH = lines.size() * LINE_H + PAD * 2;

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int x = clamp(Math.round(cfg.hudX * screenW), MARGIN, Math.max(MARGIN, screenW - boxW - MARGIN));
        int y = clamp(Math.round(cfg.hudY * screenH), MARGIN, Math.max(MARGIN, screenH - boxH - MARGIN));

        drawPanel(context, textRenderer, lines, x, y, boxW, boxH, nearest != null);
    }

    /** 找 30 米内离自己最近的玩家（不含自己）。 */
    private static PlayerEntity findNearest(MinecraftClient client) {
        PlayerEntity me = client.player;
        PlayerEntity best = null;
        double bestSq = NEAR_RADIUS * NEAR_RADIUS;
        for (PlayerEntity other : client.world.getPlayers()) {
            if (other == null || other == me || !other.isAlive()) {
                continue;
            }
            double sq = other.squaredDistanceTo(me);
            if (sq <= bestSq) {
                bestSq = sq;
                best = other;
            }
        }
        return best;
    }

    /** 画一块面板（拖拽界面也用它画预览）。 */
    public static void drawPanel(DrawContext context, TextRenderer textRenderer, List<Text> lines,
                                 int x, int y, int boxW, int boxH, boolean highlight) {
        context.fill(x, y, x + boxW, y + boxH, BG);
        int border = highlight ? BORDER_NEAR : BORDER_IDLE;
        context.fill(x, y, x + boxW, y + 1, border);
        context.fill(x, y + boxH - 1, x + boxW, y + boxH, border);
        context.fill(x, y, x + 1, y + boxH, border);
        context.fill(x + boxW - 1, y, x + boxW, y + boxH, border);

        int textY = y + PAD;
        for (Text line : lines) {
            context.drawText(textRenderer, line, x + PAD, textY, 0xFFFFFFFF, true);
            textY += LINE_H;
        }
    }

    /** 组装面板内容。{@code distance} 为 null 表示当前显示的是自己。 */
    public static List<Text> buildLines(PlayerEntry entry, TierConfig cfg, Double distance) {
        List<Text> lines = new ArrayList<>();

        // 1. 距离（只在对最近玩家显示时出现）
        if (distance != null) {
            lines.add(Text.literal("当前距离你 " + Math.round(distance) + " 米").copy()
                    .styled(s -> s.withColor(Formatting.GRAY)));
        }

        // 2. 名字 + 总名次
        MutableText head = Text.literal(entry.name == null ? "?" : entry.name).copy()
                .styled(s -> s.withColor(Formatting.WHITE));
        if (entry.rank > 0) {
            head.append(Text.literal("   #" + entry.rank).copy().styled(s -> s.withColor(Formatting.GRAY)));
        }
        lines.add(head);

        // 3. 地区
        if (entry.region != null && !entry.region.isEmpty()) {
            lines.add(Text.literal("地区 / " + entry.region).copy()
                    .styled(s -> s.withColor(Formatting.GRAY)));
        }

        // 4. 等级徽章 + 积分 + 称号。徽章本身是彩色的，用白色样式才不会被染色
        MutableText points = Text.literal(String.valueOf(RankBadges.iconFor(entry.points))).copy()
                .styled(s -> s.withColor(Formatting.WHITE));
        points.append(Text.literal(" ").copy());
        points.append(Text.literal(String.valueOf(entry.points)).copy()
                .styled(s -> s.withColor(Formatting.YELLOW)));
        if (entry.title != null && !entry.title.isEmpty()) {
            points.append(Text.literal("（" + entry.title + "）").copy()
                    .styled(s -> s.withColor(Formatting.GOLD)));
        }
        lines.add(points);

        // 5. OVERALL 汇总行
        MutableText overall = Text.literal(POINTS_ICON).copy()
                .styled(s -> s.withColor(Formatting.WHITE));
        overall.append(Text.literal(" OVERALL / " + entry.points + " Points").copy()
                .styled(s -> s.withColor(Formatting.DARK_GRAY)));
        lines.add(overall);

        // 6. 各模式段位：一行一个，带模式图标，按段位强度降序
        List<TierEntry> tiers = new ArrayList<>();
        if (entry.tiers != null) {
            for (TierEntry t : entry.tiers) {
                if (t != null && TierRegistry.isValidTier(t.tier)) {
                    tiers.add(t);
                }
            }
        }
        tiers.sort(Comparator.comparingInt((TierEntry t) -> TierRegistry.strengthOf(t.tier)).reversed());
        for (TierEntry t : tiers) {
            TierRegistry.Mode mode = TierRegistry.get(t.mode);
            String label = mode == null
                    ? (t.mode_label == null ? t.mode : t.mode_label)
                    : mode.label;

            MutableText row = Text.literal("");
            if (mode != null) {
                row.append(Text.literal(String.valueOf(mode.icon)).copy()
                        .styled(s -> s.withColor(Formatting.WHITE)));
                row.append(Text.literal(" ").copy());
            }
            row.append(Text.literal(label + " ").copy().styled(s -> s.withColor(Formatting.GRAY)));
            row.append(Text.literal(t.tier).copy()
                    .styled(s -> s.withColor(0xFF000000 | cfg.colorOf(t.tier))));
            lines.add(row);
        }

        return lines;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
