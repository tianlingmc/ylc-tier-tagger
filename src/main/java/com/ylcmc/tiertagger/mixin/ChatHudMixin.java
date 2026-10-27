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
 * <p>用 {@link ModifyVariable} 在方法入口把第一个入参（消息 {@link Text}）替换成
 * {@link ChatTagger#decorate} 的结果。注意 {@code index} 是非静态方法的局部变量槽位：
 * {@code this}=0、第一个参数（Text）=1，所以这里必须写 1，而不是 0（0 是 {@code this}，
 * 既不允许改、{@code argsOnly} 也会把 0 视为非法）。
 *
 * <p>不用 {@code ModifyArg}：它必须指向真正消费参数的指令，单凭 {@code @At("HEAD")}
 * 无法确定注入点，会在 Fabric 启动期 apply 阶段抛 {@code InvalidInjectionException}。
 * 也不用 {@code @Inject} 取消后重调：那样会二次进入本混用、反复装饰同一段消息。
 *
 * <p>decorate 内部会重建整条消息树，因此不会改动任何不可变的 siblings 列表
 * （旧写法直接 add 不可变列表，会在 1.21.11 抛 UnsupportedOperationException，
 * 导致聊天包处理失败、客户端被「协议错误」踢出）。
 */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            index = 1)
    private Text ylcTierTaggerDecorateName(Text text) {
        return ChatTagger.decorate(text);
    }
}
