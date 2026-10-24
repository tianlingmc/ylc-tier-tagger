package com.ylcmc.tiertagger.gui;

import com.ylcmc.tiertagger.core.TierConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 自绘的设置界面，不依赖 cloth-config。
 *
 * <p>用 /ylctier 打开。每个按钮直接显示当前值，点击即切换并立即写回配置文件。
 */
public class ConfigScreen extends Screen {

    private static final int BUTTON_W = 150;
    private static final int BUTTON_H = 20;
    private static final int GAP = 4;

    /** 点击颜色按钮时循环切换的预设色。 */
    private static final int[] PALETTE = {
            0xe8ba3a, 0xd5b355, 0xc4d3e7, 0xa0a7b2, 0xf89f5a, 0xc67b42,
            0x81749a, 0x655b79, 0x8f82a8, 0xffffff,
            0xff5555, 0x55dd55, 0x5555ff, 0xffaa00, 0x00dddd
    };

    public ConfigScreen(Text title) {
        super(title);
    }

    @Override
    protected void init() {
        TierConfig cfg = TierConfig.get();

        int x = this.width / 2 - BUTTON_W / 2;
        int y = 40;

        addDrawableChild(ButtonWidget.builder(
                        Text.of("总开关：" + (cfg.enabled ? "开" : "关")),
                        button -> {
                            cfg.enabled = !cfg.enabled;
                            cfg.save();
                            clearAndInit();
                        })
                .dimensions(x, y, BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + GAP;

        addDrawableChild(ButtonWidget.builder(
                        Text.of("Tab 列表显示：" + (cfg.showOnTab ? "开" : "关")),
                        button -> {
                            cfg.showOnTab = !cfg.showOnTab;
                            cfg.save();
                            clearAndInit();
                        })
                .dimensions(x, y, BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + GAP;

        addDrawableChild(ButtonWidget.builder(
                        Text.of("显示位置：" + cfg.renderLocation.label),
                        button -> {
                            cfg.renderLocation = cfg.renderLocation == TierConfig.RenderLocation.LEFT
                                    ? TierConfig.RenderLocation.RIGHT
                                    : TierConfig.RenderLocation.LEFT;
                            cfg.save();
                            clearAndInit();
                        })
                .dimensions(x, y, BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + GAP;

        addDrawableChild(ButtonWidget.builder(
                        Text.of("优先模式：" + cfg.selectedMode.label + "  ▸"),
                        button -> {
                            if (client != null) {
                                client.setScreen(new ModeSelectScreen(Text.of("选择 tier 模式")));
                            }
                        })
                .dimensions(x, y, BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + GAP;

        addDrawableChild(ButtonWidget.builder(
                        Text.of("显示规则：" + cfg.displayRule.label),
                        button -> {
                            TierConfig.DisplayRule[] values = TierConfig.DisplayRule.values();
                            cfg.displayRule = values[(cfg.displayRule.ordinal() + 1) % values.length];
                            cfg.save();
                            clearAndInit();
                        })
                .dimensions(x, y, BUTTON_W, BUTTON_H)
                .build());
        y += BUTTON_H + GAP + 4;

        // 段位颜色，两列排布
        int left = this.width / 2 - BUTTON_W - GAP / 2;
        int right = this.width / 2 + GAP / 2;
        for (int i = 0; i < TierConfig.TIER_NAMES.length; i++) {
            final String tier = TierConfig.TIER_NAMES[i];
            int bx = (i % 2 == 0) ? left : right;
            int by = y + (i / 2) * (BUTTON_H + GAP);
            addDrawableChild(ButtonWidget.builder(
                            colorLabel(tier, cfg),
                            button -> {
                                cycleColor(tier, cfg);
                                cfg.save();
                                clearAndInit();
                            })
                    .dimensions(bx, by, BUTTON_W, BUTTON_H)
                    .build());
        }

        addDrawableChild(ButtonWidget.builder(Text.of("完成"), button -> {
                    cfg.save();
                    close();
                })
                .dimensions(x, this.height - 32, BUTTON_W, BUTTON_H)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawText(textRenderer, this.title, 8, 8, 0xffffff, true);
        context.drawText(textRenderer,
                Text.of("数据来源：pvptier.ylcmc.top　当前 " + com.ylcmc.tiertagger.client.YlcTierTaggerClient.SERVICE.size() + " 人"),
                8, 20, 0xaaaaaa, false);
        context.drawText(textRenderer,
                Text.of("显示规则为「只用选中的模式」时，只有优先模式有段位才会显示。"),
                8, 32, 0xaaaaaa, false);
    }

    /** 段位名加一个色块。 */
    private static Text colorLabel(String tier, TierConfig cfg) {
        MutableText label = Text.of(tier + "  ").copy();
        label.append(Text.of("■").copy().styled(s -> s.withColor(cfg.colorOf(tier))));
        return label;
    }

    /** 在预设色板里循环。 */
    private static void cycleColor(String tier, TierConfig cfg) {
        int current = cfg.colorOf(tier);
        int index = -1;
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == current) {
                index = i;
                break;
            }
        }
        cfg.setColor(tier, PALETTE[(index + 1) % PALETTE.length]);
    }
}
