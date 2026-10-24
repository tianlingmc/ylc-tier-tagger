package com.ylcmc.tiertagger.client;

import com.ylcmc.tiertagger.core.TierConfig;
import com.ylcmc.tiertagger.core.TierService;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * 客户端入口：注册配置、启动数据拉取、定时刷新。
 */
public class YlcTierTaggerClient implements ClientModInitializer {

    /** 全局唯一的排行榜数据服务。 */
    public static final TierService SERVICE = new TierService();

    /** 每 6 秒检查一次缓存是否过期。 */
    private static final int CHECK_INTERVAL_TICKS = 120;

    private int tickCounter = 0;

    /** 当前配置。AutoConfig 内部有缓存，这里只是取一下引用。 */
    public static TierConfig config() {
        return AutoConfig.getConfigHolder(TierConfig.class).getConfig();
    }

    @Override
    public void onInitializeClient() {
        AutoConfig.register(TierConfig.class, GsonConfigSerializer::new);

        YlcTierCommand.register();
        SERVICE.bootstrap();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            tickCounter++;
            if (tickCounter < CHECK_INTERVAL_TICKS) {
                return;
            }
            tickCounter = 0;
            if (SERVICE.isExpired()) {
                SERVICE.refreshAsync();
            }
        });
    }
}
