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
 * 准星指向玩家时，在准星旁显示对方的段位卡。
 *
 * <p>用准星当指针：Tab 列表打开时鼠标仍被游戏捕获、屏幕上只有准星，
 * 所以想要「看谁显示谁」只能靠准星命中。命中判定用原版的
 * {@code MinecraftClient.crosshairTarget}，也就是生存模式约三格、创造模式约五格，
 * 正好覆盖近身对峙的场景。
 *
 * <p>卡片内容最多三行：名字/名次/积分、按段位强度排序的前几个模式、称号。
 */
public final class CrosshairCard {

    /** 卡片最多列出几个模式，避免横着铺满半个屏幕。 */
    private static final int MAX_MODES = 4;

    private static final int PAD = 4;
    private static final int LINE_H = 10;
    private static final int BG = 0xC0101010;

    private CrosshairCard() {
    }

    /** 在 HUD 上绘制；不满足条件时什么都不做。 */
    public static void render(DrawContext context, MinecraftClient client) {
        TierConfig cfg = TierConfig.get();
        if (!cfg.enabled || !cfg.showCrosshairCard) {
            return;
        }
        if (client == null || client.player == null || client.world == null) {
            return;
        }
        // 打开了界面（背包、聊天等）时不画，免得盖在界面上
        if (client.currentScreen != null) {
            return;
        }
        if (!(client.crosshairTarget instanceof EntityHitResult hit)) {
            return;
        }
        if (!(hit.getEntity() instanceof PlayerEntity target)) {
            return;
        }

        PlayerEntry entry = YlcTierTaggerClient.SERVICE.find(PlayerNames.of(target.getGameProfile()));
        if (entry == null) {
            return;
        }

        List<Text> lines = buildLines(entry, cfg);
        if (lines.isEmpty()) {
            return;
        }

        TextRenderer textRenderer = client.textRenderer;
        int textW = 0;
        for (Text line : lines) {
            textW = Math.max(textW, textRenderer.getWidth(line));
        }

        int screenW = client.getWindow().getScaledWidth();
        int screenH = client.getWindow().getScaledHeight();
        int cx = screenW / 2;
        int cy = screenH / 2;

        int boxW = textW + PAD * 2;
        int boxH = lines.size() * LINE_H + PAD * 2;

        // 默认放准星右侧偏下；右边放不下就翻到左边
        int x = cx + 12;
        int y = cy - 2;
        if (x + boxW > screenW - 4) {
            x = cx - 12 - boxW;
        }
        if (x < 4) {
            x = 4;
        }
        if (y + boxH > screenH - 4) {
            y = screenH - 4 - boxH;
        }
        if (y < 4) {
            y = 4;
        }

        context.fill(x, y, x + boxW, y + boxH, BG);

        int textY = y + PAD;
        for (Text line : lines) {
            // Text 自带样式，这里给个不透明的底色色值即可
            context.drawText(textRenderer, line, x + PAD, textY, 0xFFFFFFFF, true);
            textY += LINE_H;
        }
    }

    /** 组装卡片内容：名字/名次/积分、模式段位、称号。 */
    private static List<Text> buildLines(PlayerEntry entry, TierConfig cfg) {
        List<Text> lines = new ArrayList<>();

        MutableText head = Text.literal(entry.name == null ? "?" : entry.name).copy()
                .styled(s -> s.withColor(Formatting.WHITE));
        if (entry.rank > 0) {
            head.append(Text.literal("  #" + entry.rank).copy().styled(s -> s.withColor(Formatting.GRAY)));
        }
        if (entry.points > 0) {
            head.append(Text.literal("  " + entry.points).copy().styled(s -> s.withColor(Formatting.YELLOW)));
        }
        lines.add(head);

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
            for (int i = 0; i < tiers.size() && i < MAX_MODES; i++) {
                TierEntry t = tiers.get(i);
                if (i > 0) {
                    row.append(Text.literal("  ").copy());
                }
                TierRegistry.Mode mode = TierRegistry.get(t.mode);
                String label = mode == null
                        ? (t.mode_label == null ? t.mode : t.mode_label)
                        : mode.label;
                row.append(Text.literal(label + " ").copy().styled(s -> s.withColor(Formatting.GRAY)));
                row.append(Text.literal(t.tier).copy()
                        .styled(s -> s.withColor(0xFF000000 | cfg.colorOf(t.tier))));
            }
            lines.add(row);
        }

        if (entry.title != null && !entry.title.isEmpty()) {
            lines.add(Text.literal(entry.title).copy().styled(s -> s.withColor(Formatting.GOLD)));
        }

        return lines;
    }
}
