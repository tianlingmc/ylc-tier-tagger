package com.ylcmc.tiertagger.mixin;

import com.ylcmc.tiertagger.gui.TierHudPanel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 HUD 上叠加段位面板。
 *
 * <p>为什么挂在 {@code renderStatusBars} 而不是 {@code render}：
 * Mixin 要求处理函数<b>完整</b>声明目标方法的参数、再跟 {@link CallbackInfo}，
 * 不能只取前几个。而 {@code InGameHud.render} 的第二个参数在 1.20.4 是 {@code float}、
 * 1.21.11 是 {@code RenderTickCounter}——签名写死必有一版不匹配，按名字注入虽然能找到方法，
 * 但处理函数照样对不上，会在 apply 阶段抛 {@code InvalidInjectionException}
 * （1.2.0 启动崩溃就是这么来的）。
 *
 * <p>{@code renderStatusBars(DrawContext)} 在两个版本里同名同签名（都是私有、只收一个
 * DrawContext），处理函数写 {@code (DrawContext, CallbackInfo)} 正好匹配。
 * 它在每帧的 HUD 渲染里都会跑到（旁观模式与 F1 隐藏 HUD 时不会，这也符合预期）。
 *
 * <p>{@code require = 0}：万一将来签名再变，这里只是不生效，不会让游戏启动崩掉。
 *
 * <p>整段包了 try/catch：画面板出任何问题都不能影响原版 HUD。
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderStatusBars", at = @At("TAIL"), require = 0)
    private void ylcTierTaggerHud(DrawContext context, CallbackInfo ci) {
        try {
            TierHudPanel.render(context, MinecraftClient.getInstance());
        } catch (Throwable ignored) {
            // 静默忽略：HUD 装饰不该成为崩溃源
        }
    }
}
