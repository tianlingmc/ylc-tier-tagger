package com.ylcmc.tiertagger.mixin;

import com.ylcmc.tiertagger.gui.ConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在游戏选项界面加一个设置入口：填「鸣谢…」右边、「辅助功能…」下边的那个空格子。
 *
 * <p>选项页是 5 行 2 列的网格：左列第 5 行是「鸣谢…」，右列第 4 行是「辅助功能…」，
 * 右列第 5 行正好空着，就是我们要放的位置。所以直接以「辅助功能…」为基准——
 * 取它的列（x 与宽度），行往下挪一格，不用去猜两列之间的间距。
 *
 * <p>定位优先用翻译键（不受语言影响），键对不上时再退回按按钮文字匹配；
 * 两个都找不到才退回左下角。
 *
 * <p>注意「鸣谢」的键是 options.credits_and_attribution，不是 options.credits。
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin {

    private static final String BUTTON_LABEL = "YLCTierTagger 配置";

    private static final String KEY_ACCESSIBILITY = "options.accessibility";
    private static final String KEY_CREDITS = "options.credits_and_attribution";

    @Inject(method = "init", at = @At("RETURN"))
    private void ylcTierTaggerAddButton(CallbackInfo ci) {
        OptionsScreen self = (OptionsScreen) (Object) this;

        // 窗口尺寸变化会重新 init，避免重复添加
        for (Element element : self.children()) {
            if (element instanceof ButtonWidget button
                    && BUTTON_LABEL.equals(button.getMessage().getString())) {
                return;
            }
        }

        ClickableWidget accessibility = findButton(self, KEY_ACCESSIBILITY, "辅助功能");
        ClickableWidget credits = findButton(self, KEY_CREDITS, "鸣谢");

        int h = 20;
        int w = 150;
        int x;
        int y;

        if (accessibility != null) {
            // 右列、辅助功能的正下方一格
            x = accessibility.getX();
            w = Math.max(w, accessibility.getWidth());
            y = accessibility.getY() + accessibility.getHeight() + 6;
        } else if (credits != null) {
            x = credits.getX() + credits.getWidth() + 4;
            w = Math.max(w, credits.getWidth());
            y = credits.getY();
        } else {
            x = 8;
            y = self.height - 28;
        }

        // 目标格子万一被别的模组占了，沿这一列往下找第一个不重叠的位置
        boolean moved;
        do {
            moved = false;
            for (Element e : self.children()) {
                if (e instanceof ClickableWidget cw) {
                    boolean overlapX = cw.getX() < x + w && cw.getX() + cw.getWidth() > x;
                    boolean overlapY = cw.getY() < y + h && cw.getY() + cw.getHeight() > y;
                    if (overlapX && overlapY) {
                        y = cw.getY() + cw.getHeight() + 6;
                        moved = true;
                    }
                }
            }
        } while (moved && y + h <= self.height - 8);

        if (x + w > self.width - 8) {
            x = Math.max(8, self.width - 8 - w);
        }
        if (y + h > self.height - 8) {
            y = Math.max(8, self.height - 8 - h);
        }

        ButtonWidget button = ButtonWidget.builder(Text.literal(BUTTON_LABEL), b -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null) {
                        client.setScreen(new ConfigScreen(Text.literal(BUTTON_LABEL), self));
                    }
                })
                .dimensions(x, y, w, h)
                .build();
        ((ScreenAddChildInvoker) self).invokeAddDrawableChild(button);
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
