package com.ylcmc.tiertagger.mixin;

import com.ylcmc.tiertagger.core.ChatTagger;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 在聊天消息进列表之前，给玩家名补上段位标签。
 *
 * <p>只注入带签名和指示器的那一个重载——另一个 {@code addMessage(Text)}
 * 内部就是调用它，注入两个会加两遍。
 *
 * <p>用 {@link ModifyVariable} 把第一个入参（消息 {@link Text}）替换成装饰后的新树。
 * 注意：不能用 {@code ModifyArg} 配 {@code @At("HEAD")}——{@code ModifyArg}
 * 必须指向真正消费该参数的指令，单凭方法头无法确定注入点，会在 Fabric
 * 启动期（混用 apply 阶段）抛 {@code InvalidInjectionException}，导致游戏初始化即崩溃。
 *
 * <p>decorate 内部会重建整条消息树，因此不会去改动任何不可变的 siblings 列表
 * （旧写法直接 add 不可变列表，会在 1.21.11 抛 UnsupportedOperationException，
 * 导致聊天包处理失败、客户端被「协议错误」踢出）。
 */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true,
            index = 0)
    private Text ylcTierTaggerDecorateName(Text text) {
        return ChatTagger.decorate(text);
    }
}
