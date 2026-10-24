package com.ylcmc.tiertagger.core;

import com.ylcmc.tiertagger.client.YlcTierTaggerClient;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
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
 *
 * <p>注意：新版 MC 里 {@link Text#getSiblings()} 返回的是<b>不可变</b>列表，
 * 直接在上面 {@code add} 会抛 {@link UnsupportedOperationException}，进而让聊天包
 * 处理失败、客户端被以「协议错误」踢出。所以这里采用「重建整条消息树」的方式：
 * 递归复制每个组件（保留各自的文字、样式、子节点），只在命中的名字处插入徽章，
 * 最后由调用方用新树替换原 {@code Text}。全程不修改任何已有列表。
 */
public final class ChatTagger {

    private ChatTagger() {
    }

    /**
     * 尝试给这条消息的玩家名加上段位标签。
     *
     * @return 装饰后的消息（无改动时原样返回，绝不返回 null）
     */
    public static Text decorate(Text text) {
        if (!(text instanceof MutableText root)) {
            return text;
        }

        TierConfig cfg = TierConfig.get();
        if (!cfg.enabled || !cfg.showInChat) {
            return text;
        }

        NameHit hit = findName(root, null, -1);
        if (hit == null || hit.parent == null) {
            return text;
        }

        TierLabel.Selected selected = TierLabel.pick(hit.entry, cfg);
        if (selected == null) {
            return text;
        }

        MutableText badge = TierLabel.badge(selected, cfg);
        if (badge == null) {
            return text;
        }

        // 重建整条消息树，在名字组件处插入「徽章 + 空格 + 名字」（或反序）。
        return rebuild(text, hit, badge, cfg.renderLocation);
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

    /**
     * 递归重建消息树。遇到命中的名字节点时，把它包成「徽章 + 空格 + 名字」的新组件返回；
     * 其它节点只做浅拷贝（保留自身文字、样式）并把子节点递归重建后挂上。
     */
    private static Text rebuild(Text node, NameHit hit, MutableText badge, TierConfig.RenderLocation loc) {
        MutableText copy = node.copyContentOnly();

        if (node == hit.node) {
            // 名字节点：先完整保留它自己（含自身子节点），再连同徽章一起包起来。
            for (Text s : node.getSiblings()) {
                copy.append(rebuild(s, hit, badge, loc));
            }
            MutableText wrapper = Text.literal("").setStyle(Style.EMPTY);
            MutableText space = Text.literal(" ");
            if (loc == TierConfig.RenderLocation.RIGHT) {
                wrapper.append(copy);
                wrapper.append(space);
                wrapper.append(badge);
            } else {
                wrapper.append(badge);
                wrapper.append(space);
                wrapper.append(copy);
            }
            return wrapper;
        }

        for (Text s : node.getSiblings()) {
            copy.append(rebuild(s, hit, badge, loc));
        }
        return copy;
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
