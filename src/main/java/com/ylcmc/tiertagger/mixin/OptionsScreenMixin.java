package com.ylcmc.tiertagger.mixin;

import com.ylcmc.tiertagger.gui.OptionsConfigButton;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在游戏选项界面加一个设置入口。
 *
 * <p>创建按钮在这里做一次；真正的定位交给 {@link OptionsConfigButton#reposition}，
 * 每 tick 按布局完成后的真实坐标校正（1.21.x 的 refreshWidgetPositions 在 init 之后
 * 才跑，只量一次会漂移）。
 *
 * <p>模组是零前置的，没有 ModMenu 集成，所以给游戏自己的界面加一个入口，
 * 免得玩家只能靠记命令。
 */
@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin {

    @Inject(method = "init", at = @At("RETURN"))
    private void ylcTierTaggerAddButton(CallbackInfo ci) {
        OptionsScreen self = (OptionsScreen) (Object) this;

        // 窗口尺寸变化会重新 init，避免重复添加
        if (OptionsConfigButton.find(self) == null) {
            OptionsConfigButton.add(self);
        }
        OptionsConfigButton.reposition(self);
    }
}
