package com.ylcmc.tiertagger.mixin;

import com.ylcmc.tiertagger.gui.ConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在游戏选项界面左下角加一个设置入口。
 *
 * <p>模组是零前置的，没有 ModMenu 集成，所以给游戏自己的界面加一个入口，
 * 免得玩家只能靠记命令。
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin {

    private static final String BUTTON_LABEL = "PVPTIER 榜单设置";

    /** Screen#addDrawableChild 是 protected，借 shadow 出来调用。 */
    @Shadow
    protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T element);

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

        this.addDrawableChild(ButtonWidget.builder(Text.literal(BUTTON_LABEL), b -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null) {
                        client.setScreen(new ConfigScreen(Text.literal(BUTTON_LABEL)));
                    }
                })
                .dimensions(8, self.height - 28, 140, 20)
                .build());
    }
}
