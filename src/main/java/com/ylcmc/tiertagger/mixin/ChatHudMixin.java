package com.ylcmc.tiertagger.mixin;

import com.ylcmc.tiertagger.core.ChatTagger;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在聊天消息进列表之前，给玩家名补上段位标签。
 *
 * <p>只注入带签名和指示器的那一个重载——另一个 {@code addMessage(Text)}
 * 内部就是调用它，注入两个会加两遍。
 */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"))
    private void ylcTierTaggerDecorateName(Text text, MessageSignatureData signature,
                                           MessageIndicator indicator, CallbackInfo ci) {
        ChatTagger.decorate(text);
    }
}
