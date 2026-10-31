package com.ylcmc.tiertagger.gui;

import com.ylcmc.tiertagger.client.YlcTierTaggerClient;
import com.ylcmc.tiertagger.core.PlayerEntry;
import com.ylcmc.tiertagger.core.PlayerNames;
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
import net.minecraft.util.hit.EntityHitResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 段位 HUD 面板。
 *
 * <p>内容分四部分：名字（带总名次）、优先模式的段位与地区、各模式段位（每行四个）、积分与称号。
 *
 * <p>显示谁：准星指向玩家时显示对方，否则显示自己——这样 HUD 平时也有用，
 * 不会只在贴脸时才冒出来。位置来自配置里的比例坐标（{@link TierConfig#hudX} /
 * {@link TierConfig#hudY}），由 {@link HudPositionScreen} 拖拽设定。
 *
 * <p>位置按屏幕宽高的比例存，换分辨率或界面缩放都不会跑位。
 */
public final class TierHudPanel {

    private static final int PAD = 5;
    private static final int LINE_H = 10;
    /** 各模式一行放几个。 */
    private static final int MODES_PER_ROW = 4;
    private static final int MARGIN = 2;

    private static final int BG = 0xC0101010;
    private static final int BORDER_IDLE = 0x60FFFFFF;
    private static final int BORDER_TARGET = 0xFFFFC24A;

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

        boolean aiming = false;
        PlayerEntry entry = null;
        if (client.crosshairTarget instanceof EntityHitResult hit
                && hit.getEntity() instanceof PlayerEntity target) {
            entry = YlcTierTaggerClient.SERVICE.find(PlayerNames.of(target.getGameProfile()));
            aiming = entry != null;
        }
        if (entry == null) {
            entry = YlcTierTaggerClient.SERVICE.find(PlayerNames.of(client.player.getGameProfile()));
        }
        if (entry == null) {
            return;
        }

        List<Text> lines = buildLines(entry, cfg);
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

        drawPanel(context, textRenderer, lines, x, y, boxW, boxH, aiming);
    }

    /** 画一块面板（拖拽界面也用它画预览）。 */
    public static void drawPanel(DrawContext context, TextRenderer textRenderer, List<Text> lines,
                                 int x, int y, int boxW, int boxH, boolean aiming) {
        context.fill(x, y, x + boxW, y + boxH, BG);
        int border = aiming ? BORDER_TARGET : BORDER_IDLE;
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

    /** 组装面板内容。 */
    public static List<Text> buildLines(PlayerEntry entry, TierConfig cfg) {
        List<Text> lines = new ArrayList<>();

        // 名字 + 总名次
        MutableText head = Text.literal(entry.name == null ? "?" : entry.name).copy()
                .styled(s -> s.withColor(Formatting.WHITE));
        if (entry.rank > 0) {
            head.append(Text.literal("   #" + entry.rank).copy().styled(s -> s.withColor(Formatting.GRAY)));
        }
        lines.add(head);

        // 优先模式段位 · 地区
        MutableText second = Text.literal("");
        String preferred = entry.tierOf(cfg.selectedMode.key);
        TierRegistry.Mode preferredMode = TierRegistry.get(cfg.selectedMode.key);
        String preferredLabel = preferredMode == null ? cfg.selectedMode.label : preferredMode.label;
        if (TierRegistry.isValidTier(preferred)) {
            second.append(Text.literal(preferredLabel + " ").copy()
                    .styled(s -> s.withColor(Formatting.GRAY)));
            second.append(Text.literal(preferred).copy()
                    .styled(s -> s.withColor(0xFF000000 | cfg.colorOf(preferred))));
        } else {
            second.append(Text.literal(preferredLabel + " 未定级").copy()
                    .styled(s -> s.withColor(Formatting.DARK_GRAY)));
        }
        if (entry.region != null && !entry.region.isEmpty()) {
            second.append(Text.literal(" · " + entry.region).copy()
                    .styled(s -> s.withColor(Formatting.GRAY)));
        }
        lines.add(second);

        // 各模式段位，按强度降序，每行四个
        List<TierEntry> tiers = new ArrayList<>();
        if (entry.tiers != null) {
            for (TierEntry t : entry.tiers) {
                if (t != null && TierRegistry.isValidTier(t.tier)) {
                    tiers.add(t);
                }
            }
        }
        if (!tiers.isEmpty()) {
            tiers.sort(Comparator.comparingInt((TierEntry t) -> TierRegistry.strengthOf(t.tier)).reversed());
            MutableText row = Text.literal("");
            int inRow = 0;
            for (TierEntry t : tiers) {
                if (inRow == MODES_PER_ROW) {
                    lines.add(row);
                    row = Text.literal("");
                    inRow = 0;
                }
                if (inRow > 0) {
                    row.append(Text.literal("   ").copy());
                }
                TierRegistry.Mode mode = TierRegistry.get(t.mode);
                String label = mode == null
                        ? (t.mode_label == null ? t.mode : t.mode_label)
                        : mode.label;
                row.append(Text.literal(label + " ").copy().styled(s -> s.withColor(Formatting.GRAY)));
                row.append(Text.literal(t.tier).copy()
                        .styled(s -> s.withColor(0xFF000000 | cfg.colorOf(t.tier))));
                inRow++;
            }
            lines.add(row);
        }

        // 积分 · 称号
        MutableText foot = Text.literal(entry.points + " 分").copy()
                .styled(s -> s.withColor(Formatting.YELLOW));
        if (entry.title != null && !entry.title.isEmpty()) {
            foot.append(Text.literal(" · " + entry.title).copy()
                    .styled(s -> s.withColor(Formatting.GOLD)));
        }
        lines.add(foot);

        return lines;
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }
}
