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
 * <p><b>定位时机</b>：1.21.x 的 {@code OptionsScreen.init()} 自己就在末尾调了
 * {@code refreshWidgetPositions()}，所以在 {@code init} 的 RETURN 点量坐标，
 * 量到的已经是最终布局，不需要再事后校正（曾经试过每 tick 校正，反而出问题，已去掉）。
 * 1.20.x 没有那个方法，init 里的坐标同样就是最终的。
 *
 * <p>定位优先用翻译键（不受语言影响），键对不上再按按钮文字兜底。
 * 注意「鸣谢」的键是 options.credits_and_attribution，不是 options.credits。
 */
public final class OptionsConfigButton {

    public static final String LABEL = "YLCTierTagger 配置";

    private static final String KEY_ACCESSIBILITY = "options.accessibility";
    private static final String KEY_CREDITS = "options.credits_and_attribution";

    /** 判定「这是一个普通按钮而不是布局容器」的尺寸上限。 */
    private static final int MAX_REF_HEIGHT = 30;
    private static final int MAX_REF_WIDTH = 320;

    /**
     * 按钮宽度的上下限。
     *
     * <p>以原版按钮的实际宽度为准（这样一定和网格里的格子一样大，不会伸出去盖到
     * 旁边的按钮），只在离谱时才夹到这个区间。注意不要设硬性最小宽度——
     * 原版按钮比它窄的话，我们的按钮反而会比格子宽。
     */
    private static final int MIN_WIDTH = 90;
    private static final int MAX_WIDTH = 220;

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
            // 右列、辅助功能正下方那一格；宽度直接跟原版按钮一致
            x = accessibility.getX();
            w = clampWidth(accessibility.getWidth());
            y = accessibility.getY() + accessibility.getHeight() + 6;
        } else if (credits != null) {
            x = credits.getX() + credits.getWidth() + 4;
            w = clampWidth(credits.getWidth());
            y = credits.getY();
        } else {
            x = 8;
            y = screen.height - 28;
        }

        // 右边界：不要超出屏幕
        w = Math.min(w, Math.max(MIN_WIDTH, screen.width - 16));

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

        // 注意：不要用 setDimensionsAndPosition —— 它的参数顺序是 (width, height, x, y)，
        // 尺寸在前、坐标在后，按 (x, y, w, h) 传会把按钮撑成一大片盖住原版界面。
        mine.setDimensions(w, h);
        mine.setX(x);
        mine.setY(y);
    }

    /** 宽度跟随原版按钮，只在离谱时夹到 [MIN_WIDTH, MAX_WIDTH]。 */
    private static int clampWidth(int referenceWidth) {
        return Math.min(Math.max(referenceWidth, MIN_WIDTH), MAX_WIDTH);
    }

    /** 先按翻译键找，找不到再按按钮文字里是否包含 hint（应对键改名或被引擎改写）。 */
    private static ClickableWidget findButton(OptionsScreen screen, String key, String hint) {
        for (Element e : screen.children()) {
            if (e instanceof ClickableWidget cw && isButtonSized(cw)) {
                TextContent content = cw.getMessage().getContent();
                if (content instanceof TranslatableTextContent tt && key.equals(tt.getKey())) {
                    return cw;
                }
            }
        }
        if (hint != null) {
            for (Element e : screen.children()) {
                if (e instanceof ClickableWidget cw && isButtonSized(cw)
                        && cw.getMessage().getString().contains(hint)) {
                    return cw;
                }
            }
        }
        return null;
    }

    /**
     * 只认真正的按钮，排除布局容器。
     *
     * <p>children() 里除了按钮还可能有 ThreePartsLayoutWidget / GridWidget 这类容器，
     * 它们的尺寸是整个网格的大小，一旦被当成基准，我们的按钮就会被撑成一片大面板。
     */
    private static boolean isButtonSized(ClickableWidget widget) {
        return widget.getHeight() <= MAX_REF_HEIGHT && widget.getWidth() <= MAX_REF_WIDTH;
    }
}
