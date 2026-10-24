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
 * 在游戏选项界面加一个设置入口：放在「鸣谢…」右边、「辅助功能…」下边。
 *
 * <p>定位靠翻译键（options.credits / options.accessibility）而不是按钮文字，
 * 这样换语言也不会找错位置；两个键都找不到时才退回左下角。
 *
 * <p>模组是零前置的，没有 ModMenu 集成，所以给游戏自己的界面加一个入口，
 * 免得玩家只能靠记命令。
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin {

    private static final String BUTTON_LABEL = "YLCTierTagger 配置";

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

        ClickableWidget credits = findByKey(self, "options.credits");
        ClickableWidget accessibility = findByKey(self, "options.accessibility");

        int w = 150;
        int h = 20;
        int x;
        int y;

        if (credits != null) {
            x = credits.getX() + credits.getWidth() + 4;
            y = credits.getY();
            // 同时满足「在辅助功能下边」：鸣谢本来就更低时不用动，否则往下推
            if (accessibility != null) {
                int belowAccessibility = accessibility.getY() + accessibility.getHeight() + 4;
                if (y < belowAccessibility) {
                    y = belowAccessibility;
                }
            }
        } else {
            x = 8;
            y = self.height - 28;
        }

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

    /** 按翻译键找按钮，找不到返回 null。 */
    private static ClickableWidget findByKey(OptionsScreen screen, String key) {
        for (Element e : screen.children()) {
            if (e instanceof ClickableWidget cw) {
                TextContent content = cw.getMessage().getContent();
                if (content instanceof TranslatableTextContent tt && key.equals(tt.getKey())) {
                    return cw;
                }
            }
        }
        return null;
    }
}
