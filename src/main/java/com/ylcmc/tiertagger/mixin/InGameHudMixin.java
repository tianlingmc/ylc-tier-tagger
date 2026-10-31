package com.ylcmc.tiertagger.mixin;

import com.ylcmc.tiertagger.gui.CrosshairCard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 HUD 最后画一层「准星目标段位卡」。
 *
 * <p>注入点只写方法名 {@code render}、不写描述符：{@code InGameHud.render} 的第二个
 * 参数在 1.20.4 是 {@code float}、1.21.11 换成了 {@code RenderTickCounter}，
 * 写死描述符会导致其中一个版本注入失败；只按名字匹配则两版都能命中。
 * 处理函数的第一个参数按位置对上 {@link DrawContext}（两版都是第一个参数），
 * 后面跟 {@link CallbackInfo}，后面那些版本相关的参数不取就行。
 *
 * <p>整段包了 try/catch：画卡片出任何问题都不能影响原版 HUD。
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void ylcTierTaggerCrosshairCard(DrawContext context, CallbackInfo ci) {
        try {
            CrosshairCard.render(context, MinecraftClient.getInstance());
        } catch (Throwable ignored) {
            // 静默忽略：HUD 装饰不该成为崩溃源
        }
    }
}
