package com.ylcmc.tiertagger.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.ylcmc.tiertagger.core.PlayerEntry;
import com.ylcmc.tiertagger.core.TierConfig;
import com.ylcmc.tiertagger.core.TierEntry;
import com.ylcmc.tiertagger.core.TierRegistry;
import com.ylcmc.tiertagger.gui.ConfigScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * /ylctier —— 打开设置界面
 * /ylctier refresh —— 手动重新拉取榜单
 * /ylctier &lt;玩家名&gt; —— 查看某人在各模式的段位
 *
 * <p>命令直接注册到游戏自带的客户端 dispatcher 上，不依赖 fabric-api。
 */
public class YlcTierCommand {

    public static LiteralArgumentBuilder<ClientCommandSource> build() {
        LiteralArgumentBuilder<ClientCommandSource> root = LiteralArgumentBuilder.literal("ylctier");

        root.executes(context -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                client.setScreen(new ConfigScreen(Text.literal("PVPTIER 榜单设置")));
            }
            return 1;
        });

        root.then(LiteralArgumentBuilder.<ClientCommandSource>literal("refresh")
                .executes(context -> {
                    boolean started = YlcTierTaggerClient.SERVICE.refreshAsync();
                    reply(context, Text.literal(started ? "正在重新拉取榜单…" : "正在拉取中，请稍候"));
                    return 1;
                }));

        root.then(RequiredArgumentBuilder.<ClientCommandSource, String>argument("player", StringArgumentType.string())
                .executes(context -> {
                    String name = context.getArgument("player", String.class);
                    PlayerEntry entry = YlcTierTaggerClient.SERVICE.find(name);
                    if (entry == null) {
                        reply(context, Text.literal("排行榜里没有这个玩家").copy().styled(s -> s.withColor(Formatting.RED)));
                        return 0;
                    }
                    reply(context, buildPlayerCard(entry));
                    return 1;
                }));

        return root;
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
                        .copy().styled(s -> s.withColor(TierConfig.get().colorOf(t.tier))));
                result.append(Text.literal("\n").copy().styled(s -> s.withColor(Formatting.GRAY)));
            }
        }

        result.append(Text.literal("地区：" + (entry.region == null ? "-" : entry.region)
                        + "　积分：" + entry.points + "　名次：" + entry.rank)
                .copy().styled(s -> s.withColor(Formatting.DARK_GRAY)));
        return result;
    }

    /** 往聊天栏发一条消息。 */
    private static void reply(CommandContext<ClientCommandSource> context, Text text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            client.player.sendMessage(text, false);
        }
    }
}
