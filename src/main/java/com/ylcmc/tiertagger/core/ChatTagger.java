package com.ylcmc.tiertagger.core;

import com.ylcmc.tiertagger.client.YlcTierTaggerClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;

/**
 * 给聊天消息里的玩家名加上段位标签。
 *
 * <p>服务端发过来的玩家消息结构是三个 sibling：<code>[名字, "&gt; ", 内容]</code>
 * （1.19+ 的格式），另有一种是名字自带尖括号 <code>&lt;名字&gt;</code>。
 * 这里只在名字后追加一小段标签，不动消息的其他部分与样式。
 */
public final class ChatTagger {

    private ChatTagger() {
    }

    /** 尝试给这条消息的玩家名加上段位标签。认不出来就什么都不做。 */
    public static void decorate(Text text) {        if (!(text instanceof MutableText mutable)) {
            return;
        }

        TierConfig cfg = TierConfig.get();
        if (!cfg.enabled || !cfg.showInChat) {
            return;
        }

        List<Text> siblings = mutable.getSiblings();
        if (siblings.size() < 2) {
            return;
        }

        Text namePart = siblings.get(0);
        if (!(namePart instanceof MutableText nameText)) {
            return;
        }

        String raw = nameText.getString();
        String name = null;

        if (raw.length() > 2 && raw.startsWith("<") && raw.endsWith(">")) {
            // <名字> 形式
            name = raw.substring(1, raw.length() - 1);
        } else if (siblings.get(1).getString().startsWith(">")) {
            // 名字 与 "> " 分开的常规形式
            name = raw;
        }

        if (name == null || name.isEmpty()) {
            return;
        }

        PlayerEntry entry = YlcTierTaggerClient.SERVICE.find(name);
        if (entry == null) {
            return;
        }

        TierLabel.Selected selected = TierLabel.pick(entry, cfg);
        if (selected == null) {
            return;
        }

        MutableText badge = TierLabel.badge(selected, cfg);
        if (badge == null) {
            return;
        }

        if (cfg.renderLocation == TierConfig.RenderLocation.RIGHT) {
            // 标签放在名字后面
            nameText.append(Text.literal(" ").copy());
            nameText.append(badge);
        } else {
            // 标签放在名字前面：整段名字包一层，名字本身的样式不受影响
            MutableText wrapper = Text.literal("").copy();
            wrapper.append(badge);
            wrapper.append(Text.literal(" ").copy());
            wrapper.append(nameText);
            siblings.set(0, wrapper);
        }
    }
}
