package com.ylcmc.tiertagger.core;

import com.ylcmc.tiertagger.client.YlcTierTaggerClient;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;

/**
 * 给聊天消息里的玩家名加上段位标签。
 *
 * <p>关键改动：不再依赖「消息必须是 &lt;名字&gt; + 内容」这种固定 sibling 结构——
 * 开了聊天插件的服（EssentialsChat 之类）会把整行重排，原假设直接 return。
 * 这里改成在整条消息里递归找「玩家名组件」：玩家名组件一定带
 * {@code SHOW_ENTITY} 的悬浮事件（原生与主流聊天插件都会给名字加这个），
 * 找到后把徽章作为它的兄弟节点插到旁边，左侧/右侧由配置决定。
 */
public final class ChatTagger {

    private ChatTagger() {
    }

    /** 尝试给这条消息的玩家名加上段位标签。认不出来就什么都不做。 */
    public static void decorate(Text text) {
        if (!(text instanceof MutableText root)) {
            return;
        }

        TierConfig cfg = TierConfig.get();
        if (!cfg.enabled || !cfg.showInChat) {
            return;
        }

        NameHit hit = findName(root, null, -1);
        if (hit == null || hit.parent == null) {
            return;
        }

        TierLabel.Selected selected = TierLabel.pick(hit.entry, cfg);
        if (selected == null) {
            return;
        }

        MutableText badge = TierLabel.badge(selected, cfg);
        if (badge == null) {
            return;
        }

        // 在名字组件的兄弟列表里，按配置插到它左边或右边（含一个空格）。
        List<Text> sibs = hit.parent.getSiblings();
        MutableText space = Text.literal(" ");
        if (cfg.renderLocation == TierConfig.RenderLocation.RIGHT) {
            sibs.add(hit.index + 1, space);
            sibs.add(hit.index + 2, badge);
        } else {
            sibs.add(hit.index, badge);
            sibs.add(hit.index + 1, space);
        }
    }

    /** 命中结果：名字组件、它的父组件、在父里的下标、对应的玩家记录。 */
    private static final class NameHit {
        final MutableText node;
        final MutableText parent;
        final int index;
        final PlayerEntry entry;
        final int score;

        NameHit(MutableText node, MutableText parent, int index, PlayerEntry entry, int score) {
            this.node = node;
            this.parent = parent;
            this.index = index;
            this.entry = entry;
            this.score = score;
        }
    }

    /** 递归查找最像「玩家名」的组件。 */
    private static NameHit findName(MutableText node, MutableText parent, int index) {
        NameHit self = scoreNode(node, parent, index, parent == null);
        NameHit best = self;
        List<Text> sibs = node.getSiblings();
        for (int i = 0; i < sibs.size(); i++) {
            if (sibs.get(i) instanceof MutableText child) {
                NameHit h = findName(child, node, i);
                if (h != null && (best == null || h.score > best.score)) {
                    best = h;
                }
            }
        }
        return best;
    }

    /** 给一个组件打分：带 SHOW_ENTITY 悬浮的名字组件权重最高。 */
    private static NameHit scoreNode(MutableText node, MutableText parent, int index, boolean isRoot) {
        HoverEvent he = node.getStyle().getHoverEvent();
        if (he != null && he.getAction() == HoverEvent.Action.SHOW_ENTITY) {
            PlayerEntry e = findEntryInSubtree(node);
            if (e != null) {
                return new NameHit(node, parent, index, e, 100);
            }
        }
        // 退路：用字面量反查玩家（显示名可能带前缀/颜色）。根节点和过长的整行不参加，避免误匹配消息正文。
        if (!isRoot) {
            String s = node.getString();
            if (s != null && s.length() <= 64) {
                PlayerEntry e = findEntry(s);
                if (e != null) {
                    return new NameHit(node, parent, index, e, 50);
                }
            }
        }
        return null;
    }

    /** 精确优先，再退到「字面量包含某玩家名」。 */
    private static PlayerEntry findEntry(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        PlayerEntry exact = YlcTierTaggerClient.SERVICE.find(s);
        if (exact != null) {
            return exact;
        }
        for (PlayerEntry e : YlcTierTaggerClient.SERVICE.entries()) {
            if (e.name != null && e.name.length() >= 2 && s.contains(e.name)) {
                return e;
            }
        }
        return null;
    }

    /** 在组件自身及其子孙的字面量里找玩家。 */
    private static PlayerEntry findEntryInSubtree(MutableText node) {
        PlayerEntry e = findEntry(node.getString());
        if (e != null) {
            return e;
        }
        for (Text child : node.getSiblings()) {
            if (child instanceof MutableText m) {
                PlayerEntry r = findEntryInSubtree(m);
                if (r != null) {
                    return r;
                }
            }
        }
        return null;
    }
}
