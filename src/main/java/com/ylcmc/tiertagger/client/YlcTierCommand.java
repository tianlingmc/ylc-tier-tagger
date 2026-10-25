package com.ylcmc.tiertagger.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.ylcmc.tiertagger.core.PlayerEntry;
import com.ylcmc.tiertagger.core.TierConfig;
import com.ylcmc.tiertagger.core.TierEntry;
import com.ylcmc.tiertagger.core.TierRegistry;
import com.ylcmc.tiertagger.gui.ConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * 客户端命令 /ylctier。
 *
 * <p>子命令：
 * <ul>
 *   <li>{@code /ylctier config} —— 打开设置界面</li>
 *   <li>{@code /ylctier retrieve} —— 重新拉取榜单数据</li>
 *   <li>{@code /ylctier reload} —— 重载模组配置</li>
 *   <li>{@code /ylctier <玩家名>} —— 查看某人在各模式的段位</li>
 * </ul>
 *
 * <p>这里刻意用原始类型（不带泛型参数）：客户端命令的 source 类型在
 * 1.20.x 与 1.21.9+ 之间换过类（CommandSource / ClientCommandSource），
 * 泛型擦除后就不受影响了。
 *
 * <p>注意：{@link #build()} 注册到客户端 dispatcher 只是为了补全提示，
 * 真正执行走 {@link #execute(String)} —— 由 ClientPlayNetworkHandlerMixin
 * 在 sendChatCommand 处拦截，因为原版只会把命令发给服务端。
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class YlcTierCommand {

    /** 命令名。注册处与拦截处都用它判断。 */
    public static final String NAME = "ylctier";

    /**
     * 尝试把一条玩家输入的命令当作本模组的命令执行。
     *
     * @param raw 玩家输入的内容，可能带前导斜杠
     * @return 命中并已处理返回 true（调用方据此取消发包），不是本模组的命令返回 false
     */
    public static boolean execute(String raw) {
        if (raw == null) {
            return false;
        }
        String cmd = raw.trim();
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }
        if (cmd.isEmpty()) {
            return false;
        }

        String[] parts = cmd.split("\\s+", 2);
        if (!NAME.equalsIgnoreCase(parts[0])) {
            return false;
        }
        String arg = parts.length > 1 ? parts[1].trim() : "";
        return run(arg);
    }

    /** 执行具体的子命令。 */
    private static boolean run(String arg) {
        if (arg.isEmpty()) {
            reply(buildHelp());
            return true;
        }

        String[] sub = arg.split("\\s+", 2);
        String head = sub[0];

        if ("config".equalsIgnoreCase(head)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.setScreen(new ConfigScreen(Text.literal("YLCTierTagger 配置"), client.currentScreen));
            }
            return true;
        }

        if ("retrieve".equalsIgnoreCase(head)) {
            boolean started = YlcTierTaggerClient.SERVICE.refreshAsync();
            reply(Text.literal(started ? "正在重新拉取榜单…" : "正在拉取中，请稍候")
                    .copy().styled(s -> s.withColor(started ? Formatting.YELLOW : Formatting.GRAY)));
            return true;
        }

        if ("reload".equalsIgnoreCase(head)) {
            TierConfig.load();
            reply(Text.literal("已重新载入模组配置")
                    .copy().styled(s -> s.withColor(Formatting.GREEN)));
            return true;
        }

        PlayerEntry entry = YlcTierTaggerClient.SERVICE.find(head);
        if (entry == null) {
            reply(Text.literal("排行榜里没有这个玩家").copy().styled(s -> s.withColor(Formatting.RED)));
            return true;
        }
        reply(buildPlayerCard(entry));
        return true;
    }

    /** 注册到客户端 dispatcher，只为了补全提示。 */
    public static LiteralArgumentBuilder build() {
        LiteralArgumentBuilder root = LiteralArgumentBuilder.literal(NAME);

        root.executes(context -> 1);

        root.then(LiteralArgumentBuilder.literal("config").executes(context -> 1));
        root.then(LiteralArgumentBuilder.literal("retrieve").executes(context -> 1));
        root.then(LiteralArgumentBuilder.literal("reload").executes(context -> 1));

        root.then(RequiredArgumentBuilder.argument("player", StringArgumentType.string())
                .executes(context -> 1));

        return root;
    }

    /** 不带参数时的用法说明。 */
    private static MutableText buildHelp() {
        MutableText text = Text.literal("- - - - - [ YLCTierTagger Help ] - - - - -\n").copy()
                .styled(s -> s.withColor(Formatting.GOLD));
        appendLine(text, "/ylctier config", "打开配置界面");
        appendLine(text, "/ylctier retrieve", "重新拉取榜单数据");
        appendLine(text, "/ylctier reload", "重载模组配置");
        appendLine(text, "/ylctier <玩家名>", "查看该玩家的段位");
        return text;
    }

    private static void appendLine(MutableText target, String cmd, String desc) {
        target.append(Text.literal(cmd).copy().styled(s -> s.withColor(Formatting.YELLOW)));
        target.append(Text.literal(" - " + desc + "\n").copy().styled(s -> s.withColor(Formatting.GRAY)));
    }

    /** 列出某人在各模式的段位。 */
    private static MutableText buildPlayerCard(PlayerEntry entry) {
        MutableText result = Text.literal("======== " + entry.name + " ========\n")
                .copy().styled(s -> s.withColor(Formatting.GOLD));

        if (entry.tiers != null) {
            for (TierEntry t : entry.tiers) {
                if (t == null || !TierRegistry.isValidTier(t.tier)) {
                    continue;
                }
                TierRegistry.Mode mode = TierRegistry.get(t.mode);
                String icon = mode == null ? "" : String.valueOf(mode.icon);
                String label = mode == null ? t.mode : mode.label;

                result.append(Text.literal(icon + " " + label + "：")
                        .copy().styled(s -> s.withColor(Formatting.GRAY)));
                result.append(Text.literal(t.tier)
                        .copy().styled(s -> s.withColor(0xFF000000 | TierConfig.get().colorOf(t.tier))));
                result.append(Text.literal("\n").copy().styled(s -> s.withColor(Formatting.GRAY)));
            }
        }

        result.append(Text.literal("地区：" + (entry.region == null ? "-" : entry.region)
                        + "　积分：" + entry.points + "　名次：" + entry.rank)
                .copy().styled(s -> s.withColor(Formatting.DARK_GRAY)));
        return result;
    }

    /** 往聊天栏发一条消息。 */
    private static void reply(Text text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(text, false);
        }
    }
}
