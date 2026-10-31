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
 * <p>为什么挂在 {@code renderMountHealth}：
 * <ul>
 *   <li>{@code render} 的第二个参数两版不同（{@code float} / {@code RenderTickCounter}），
 *       而 Mixin 要求处理函数<b>完整</b>声明目标方法的全部参数，不能只取前几个，
 *       所以不能直接挂 {@code render}。</li>
 *   <li>{@code renderStatusBars} 虽然两版签名一致，但它是<b>有条件</b>调用的：
 *       {@code renderMainHud} 里是 {@code if (interactionManager.hasStatusBars())} 才调，
 *       创造模式与旁观模式下为 false——1.2.0 的「HUD 不显示」就是这个原因。</li>
 *   <li>{@code renderMountHealth(DrawContext)} 两版同名同签名（私有、单参），且在
 *       {@code renderMainHud} 里是<b>无条件</b>调用的，所以每帧都会跑到。</li>
 * </ul>
 *
 * <p>注入点用 {@code HEAD} 而不是 {@code TAIL}：{@code renderMountHealth} 内部有提前返回，
 * {@code TAIL} 对应方法末尾的 return，提前返回时会被跳过。
 *
 * <p>{@code require = 0}：将来签名再变也只是面板不显示，不会让游戏启动崩掉。
 *
 * <p>整段包了 try/catch：画面板出任何问题都不能影响原版 HUD。
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "renderMountHealth", at = @At("HEAD"), require = 0)
    private void ylcTierTaggerHud(DrawContext context, CallbackInfo ci) {
        try {
            TierHudPanel.render(context, MinecraftClient.getInstance());
        } catch (Throwable ignored) {
            // 静默忽略：HUD 装饰不该成为崩溃源
        }
    }
}
