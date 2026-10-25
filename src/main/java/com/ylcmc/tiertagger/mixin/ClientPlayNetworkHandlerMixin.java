package com.ylcmc.tiertagger.mixin;

import com.ylcmc.tiertagger.client.YlcTierCommand;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 让 /ylctier 真正能在客户端本地执行。
 *
 * <p>原版的 {@code sendChatCommand} 只做一件事：把命令封成 {@code CommandExecutionC2SPacket}
 * 发给服务端（它开头那次 parse 只是为了给参数签名），压根不会执行客户端命令。
 * 零前置又没有 fabric-api 的客户端命令钩子，所以之前输入任何 /ylctier 子命令
 * 都会被原样发到服务端，服务端不认识就回「未知或不完整的命令」。
 *
 * <p>{@code sendChatCommand(String)} 在 1.20.4 与 1.21.11 签名一致，可以安全拦截。
 * 命中我们自己的命令就本地处理并取消发包，其它命令原样放行。
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true)
    private void ylcTierTaggerRunClientCommand(String command, CallbackInfo ci) {
        if (YlcTierCommand.execute(command)) {
            ci.cancel();
        }
    }
}
