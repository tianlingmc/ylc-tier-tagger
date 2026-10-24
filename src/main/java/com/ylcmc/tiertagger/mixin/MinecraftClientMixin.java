package com.ylcmc.tiertagger.mixin;

import com.ylcmc.tiertagger.client.YlcTierTaggerClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端每 tick 的钩子：注册命令、检查缓存刷新、进服后弹提示。
 *
 * <p>用这个代替 fabric-api 的 ClientTickEvents，从而不依赖任何前置。
 */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void ylcTierTaggerTick(CallbackInfo ci) {
        YlcTierTaggerClient.onTick((MinecraftClient) (Object) this);
    }
}
