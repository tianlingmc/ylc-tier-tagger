package com.ylcmc.tiertagger.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.ylcmc.tiertagger.core.ChatTagger;
import net.minecraft.network.message.MessageType;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 聊天栏的段位标签，从「消息」改到「玩家名字段」上注入。
 *
 * <p>原版拼聊天行时，{@code Decoration.apply(content, parameters)} 会通过
 * {@code Decoration$Parameter$Selector} 调 {@code parameters.name()} 取发送者名字，
 * 再把它填进 {@code chat.type.text} 这类模板。这里只改这个访问器的返回值，
 * 消息本体一个字节都不动——原版的颜色、字体、点击/悬浮事件、模板里的其它参数全部保真。
 *
 * <p>{@code MessageType$Parameters.name()} 在 1.20.4 与 1.21.11 里同名同签名
 * （无参、返回 {@link Text}），所以这一处可以跨版本通用。
 *
 * <p>原先挂在 {@code ChatHud.addMessage} 上替换整条消息的做法已删除。
 */
@Mixin(MessageType.Parameters.class)
public class MessageTypeParametersMixin {

    @ModifyReturnValue(method = "name", at = @At("RETURN"))
    private Text ylcTierTaggerDecorateChatName(Text original) {
        return ChatTagger.decorateName(original);
    }
}
