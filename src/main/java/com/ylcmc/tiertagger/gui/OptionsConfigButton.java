package com.ylcmc.tiertagger.gui;

import com.ylcmc.tiertagger.mixin.ScreenAddChildInvoker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;

/**
 * 选项页里那个设置按钮的创建与定位。
 *
 * <p>目标位置是选项网格右列最后一行的空格——也就是「鸣谢…」右边、「辅助功能…」下边。
 *
 * <p><b>为什么位置要反复校正</b>：1.21.x 的 {@code OptionsScreen} 有
 * {@code refreshWidgetPositions()}，它在 {@code init()} 之后才把按钮摆到最终位置。
 * 所以只在 {@code init} 末尾量一次坐标，量到的还不是最终布局，表现就是
 * 换界面缩放后按钮位置漂移。这里改成每 tick 按当前真实坐标重算一次，自然就稳了；
 * 1.20.x 没有那个方法、init 里坐标就是最终的，重算结果也一致。
 *
 * <p>定位优先用翻译键（不受语言影响），键对不上再按按钮文字兜底。
 * 注意「鸣谢」的键是 options.credits_and_attribution，不是 options.credits。
 */
public final class OptionsConfigButton {

    public static final String LABEL = "YLCTierTagger 配置";

    private static final String KEY_ACCESSIBILITY = "options.accessibility";
    private static final String KEY_CREDITS = "options.credits_and_attribution";

    private OptionsConfigButton() {
    }

    /** 找已经加过的按钮，没加过返回 null。 */
    public static ButtonWidget find(OptionsScreen screen) {
        for (Element e : screen.children()) {
            if (e instanceof ButtonWidget bw && LABEL.equals(bw.getMessage().getString())) {
                return bw;
            }
        }
        return null;
    }

    /** 创建并挂到选项页上。 */
    public static void add(OptionsScreen screen) {
        ButtonWidget button = ButtonWidget.builder(Text.literal(LABEL), b -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null) {
                        client.setScreen(new ConfigScreen(Text.literal(LABEL), screen));
                    }
                })
                .dimensions(8, screen.height - 28, 150, 20)
                .build();
        ((ScreenAddChildInvoker) screen).invokeAddDrawableChild(button);
    }

    /** 按当前真实布局重新定位；按钮不存在就直接返回。 */
    public static void reposition(OptionsScreen screen) {
        ButtonWidget mine = find(screen);
        if (mine == null) {
            return;
        }

        ClickableWidget accessibility = findButton(screen, KEY_ACCESSIBILITY, "辅助功能");
        ClickableWidget credits = findButton(screen, KEY_CREDITS, "鸣谢");

        int h = 20;
        int w = 150;
        int x;
        int y;

        if (accessibility != null) {
            // 右列、辅助功能正下方那一格
            x = accessibility.getX();
            w = Math.max(w, accessibility.getWidth());
            y = accessibility.getY() + accessibility.getHeight() + 6;
        } else if (credits != null) {
            x = credits.getX() + credits.getWidth() + 4;
            w = Math.max(w, credits.getWidth());
            y = credits.getY();
        } else {
            x = 8;
            y = screen.height - 28;
        }

        // 目标格万一被别的模组占了，沿这一列往下找第一个不重叠的位置
        boolean moved;
        do {
            moved = false;
            for (Element e : screen.children()) {
                if (e instanceof ClickableWidget cw && cw != mine) {
                    boolean overlapX = cw.getX() < x + w && cw.getX() + cw.getWidth() > x;
                    boolean overlapY = cw.getY() < y + h && cw.getY() + cw.getHeight() > y;
                    if (overlapX && overlapY) {
                        y = cw.getY() + cw.getHeight() + 6;
                        moved = true;
                    }
                }
            }
        } while (moved && y + h <= screen.height - 8);

        if (x + w > screen.width - 8) {
            x = Math.max(8, screen.width - 8 - w);
        }
        if (y + h > screen.height - 8) {
            y = Math.max(8, screen.height - 8 - h);
        }

        mine.setDimensionsAndPosition(x, y, w, h);
    }

    /** 先按翻译键找，找不到再按按钮文字里是否包含 hint（应对键改名或被引擎改写）。 */
    private static ClickableWidget findButton(OptionsScreen screen, String key, String hint) {
        for (Element e : screen.children()) {
            if (e instanceof ClickableWidget cw) {
                TextContent content = cw.getMessage().getContent();
                if (content instanceof TranslatableTextContent tt && key.equals(tt.getKey())) {
                    return cw;
                }
            }
        }
        if (hint != null) {
            for (Element e : screen.children()) {
                if (e instanceof ClickableWidget cw && cw.getMessage().getString().contains(hint)) {
                    return cw;
                }
            }
        }
        return null;
    }
}
