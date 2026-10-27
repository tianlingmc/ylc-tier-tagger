package com.ylcmc.tiertagger.core;

import com.ylcmc.tiertagger.client.YlcTierTaggerClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * 给聊天栏里的「玩家名字段」加段位标签。
 *
 * <p>注入点是原版的 {@code MessageType.Parameters.name()}——聊天装饰（{@code Decoration}）
 * 拼整行时就是调这个访问器取发送者名字。我们只装饰名字本身，不碰消息本体，
 * 因此原版的颜色、字体、点击与悬浮事件、以及装饰模板里的其它参数全都原样保留。
 *
 * <p>之前那种「把整条消息树拆开重建」的做法会丢样式，已经不用了。
 */
public final class ChatTagger {

    private ChatTagger() {
    }

    /**
     * 给一个名字组件加上段位标签。
     *
     * @param name 原版的发送者名字（可能带服务器前缀或颜色）
     * @return 装饰后的名字；没有对应段位时原样返回
     */
    public static Text decorateName(Text name) {
        if (name == null) {
            return null;
        }

        TierConfig cfg = TierConfig.get();
        if (!cfg.enabled || !cfg.showInChat) {
            return name;
        }

        PlayerEntry entry = findEntry(name.getString());
        if (entry == null) {
            return name;
        }

        TierLabel.Selected selected = TierLabel.pick(entry, cfg);
        if (selected == null) {
            return name;
        }

        MutableText badge = TierLabel.badge(selected, cfg);
        if (badge == null) {
            return name;
        }

        return TierLabel.combine(name, badge, cfg);
    }

    /** 精确优先，再退到「字面量包含某玩家名」（名字可能带服务器前缀/颜色）。 */
    private static PlayerEntry findEntry(String s) {
        if (s == null) {
            return null;
        }
        String text = s.trim();
        if (text.isEmpty()) {
            return null;
        }

        PlayerEntry exact = YlcTierTaggerClient.SERVICE.find(text);
        if (exact != null) {
            return exact;
        }

        for (PlayerEntry e : YlcTierTaggerClient.SERVICE.entries()) {
            if (e.name != null && e.name.length() >= 2 && text.contains(e.name)) {
                return e;
            }
        }
        return null;
    }
}
