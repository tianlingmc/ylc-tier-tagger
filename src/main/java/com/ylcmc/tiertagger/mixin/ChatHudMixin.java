package com.ylcmc.tiertagger.mixin;

import com.ylcmc.tiertagger.core.ChatTagger;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 在聊天消息进列表之前，给玩家名补上段位标签。
 *
 * <p>只注入带签名和指示器的那一个重载——另一个 {@code addMessage(Text)}
 * 内部就是调用它，注入两个会加两遍。
 *
 * <p>用 {@link ModifyArg} 把第一个入参（消息 {@link Text}）替换成装饰后的新树。
 * decorate 内部会重建整条消息树，因此不会去改动任何不可变的 siblings 列表
 * （旧写法直接 add 不可变列表，会在 1.21.11 抛 UnsupportedOperationException，
 * 导致聊天包处理失败、客户端被「协议错误」踢出）。
 */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyArg(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            index = 0)
    private Text ylcTierTaggerDecorateName(Text text) {
        return ChatTagger.decorate(text);
    }
}
