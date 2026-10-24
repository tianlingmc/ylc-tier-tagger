package com.ylcmc.tiertagger.client;

import com.ylcmc.tiertagger.core.TierConfig;
import com.ylcmc.tiertagger.core.TierService;
import com.ylcmc.tiertagger.modmenu.ClothConfigBootstrap;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

/**
 * 客户端入口：读配置、启动时拉一次排行榜。
 *
 * <p>每 tick 的处理放在 {@link #onTick(MinecraftClient)}，由 MinecraftClientMixin 调用，
 * 这样不需要依赖 fabric-api 的事件总线。
 */
public class YlcTierTaggerClient implements ClientModInitializer {

    /** 全局唯一的排行榜数据服务。 */
    public static final TierService SERVICE = new TierService();

    /** 每 60 秒检查一次缓存是否过期。 */
    private static final int REFRESH_CHECK_TICKS = 1200;

    /** 进服后最多等这么多 tick 再提示，兜底防止拉取一直挂着不给提示。 */
    private static final int NOTIFY_WAIT_TICKS = 60;

    private static int tickCounter = 0;
    private static int ticksSinceJoin = 0;
    private static boolean notified = false;

    /** 上一次注册命令时用的网络处理器，换服后需要重新注册。 */
    private static ClientPlayNetworkHandler registeredHandler = null;

    @Override
    public void onInitializeClient() {
        // 可选集成：装了 cloth-config 就注册一份，共用同一个配置文件
        ClothConfigBootstrap.register();
        TierConfig.load();
        // 进程启动即拉一次最新数据，本地缓存只作兜底
        SERVICE.bootstrap();
    }

    /** 每客户端 tick 调用一次。 */
    public static void onTick(MinecraftClient client) {
        registerCommand(client);

        if (client.player == null) {
            ticksSinceJoin = 0;
            notified = false;
            return;
        }

        ticksSinceJoin++;

        // 玩家可能在 cloth-config 的界面上改了设置，跟着同步一次
        if (ticksSinceJoin % 40 == 0) {
            TierConfig.reloadIfChanged();
        }

        // 进游戏后弹一次提示，说明数据状态
        if (!notified && (SERVICE.status() != TierService.Status.LOADING || ticksSinceJoin > NOTIFY_WAIT_TICKS)) {
            notified = true;
            notifyStatus(client);
        }

        tickCounter++;
        if (tickCounter < REFRESH_CHECK_TICKS) {
            return;
        }
        tickCounter = 0;
        if (SERVICE.isExpired()) {
            SERVICE.refreshAsync();
        }
    }

    /** 客户端命令注册到游戏自带的 dispatcher 上。 */
    private static void registerCommand(MinecraftClient client) {
        ClientPlayNetworkHandler handler = client.getNetworkHandler();
        if (handler == null || handler == registeredHandler) {
            return;
        }
        try {
            handler.getCommandDispatcher().register(YlcTierCommand.build());
            registeredHandler = handler;
        } catch (Exception e) {
            System.err.println("[ylc-tier-tagger] 注册客户端命令失败：" + e);
        }
    }

    /** 把当前数据状态弹给玩家看。 */
    private static void notifyStatus(MinecraftClient client) {
        String title = "PVPTIER 榜单";
        String body;

        switch (SERVICE.status()) {
            case OK:
                body = "已加载 " + SERVICE.size() + " 名玩家的段位";
                break;
            case FAILED:
                body = "拉取失败：" + (SERVICE.lastError() == null ? "未知原因" : SERVICE.lastError())
                        + (SERVICE.hasData() ? "（正在使用本地缓存）" : "");
                break;
            case LOADING:
                body = "正在拉取…";
                break;
            default:
                body = "尚未拉取";
                break;
        }

        client.getToastManager().add(new SystemToast(
                SystemToast.Type.PERIODIC_NOTIFICATION,
                Text.of(title),
                Text.of(body)));
    }
}
