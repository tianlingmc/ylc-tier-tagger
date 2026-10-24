package com.ylcmc.tiertagger.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.ylcmc.tiertagger.core.PlayerEntry;
import com.ylcmc.tiertagger.core.TierEntry;
import com.ylcmc.tiertagger.core.TierRegistry;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * /ylctier <玩家名> —— 查看某人在各模式的段位。
 */
public class YlcTierCommand {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, ra) -> {
            dispatcher.register(
                    literal("ylctier")
                            .then(argument("player", StringArgumentType.string())
                                    .executes(context -> {
                                        String name = context.getArgument("player", String.class);
                                        PlayerEntry entry = YlcTierTaggerClient.SERVICE.find(name);
                                        if (entry == null) {
                                            context.getSource().sendError(Text.of("排行榜里没有这个玩家"));
                                            return 1;
                                        }

                                        MutableText result = Text.of("======== " + entry.name + " ========\n")
                                                .copy().styled(s -> s.withColor(Formatting.GOLD));

                                        if (entry.tiers != null) {
                                            for (TierEntry t : entry.tiers) {
                                                if (t == null || !TierRegistry.isValidTier(t.tier)) {
                                                    continue;
                                                }
                                                TierRegistry.Mode mode = TierRegistry.get(t.mode);
                                                String icon = mode == null ? "" : String.valueOf(mode.icon);
                                                String label = mode == null ? t.mode : mode.label;

                                                result.append(Text.of(icon + " " + label + "：")
                                                        .copy().styled(s -> s.withColor(Formatting.GRAY)));
                                                result.append(Text.of(t.tier)
                                                        .copy().styled(s -> s.withColor(YlcTierTaggerClient.config().colorOf(t.tier))));
                                                result.append(Text.of("\n")
                                                        .copy().styled(s -> s.withColor(Formatting.GRAY)));
                                            }
                                        }

                                        result.append(Text.of("地区：" + (entry.region == null ? "-" : entry.region)
                                                        + "　积分：" + entry.points + "　名次：" + entry.rank)
                                                .copy().styled(s -> s.withColor(Formatting.DARK_GRAY)));

                                        context.getSource().sendFeedback(result);
                                        return 1;
                                    }))
            );
        });
    }
}
