package com.ylcmc.tiertagger.modmenu;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.loader.api.FabricLoader;

/**
 * cloth-config 的可选集成。
 *
 * <p>装了 cloth-config 时注册一份配置，玩家就能用标准的 /config 命令打开配置界面。
 * 没装的话这个类里的 cloth-config 依赖永远不会被解析，因此零前置也能正常跑。
 */
public final class ClothConfigBootstrap {

    private static final String CLOTH_CONFIG = "cloth-config";

    private ClothConfigBootstrap() {
    }

    /** 装了 cloth-config 才注册。 */
    public static void register() {
        if (!hasClothConfig()) {
            return;
        }
        try {
            AutoConfig.register(ClothConfigAdapter.class, GsonConfigSerializer::new);
        } catch (Exception e) {
            System.err.println("[ylc-tier-tagger] 注册 cloth-config 失败：" + e);
        }
    }

    /** 让 cloth-config 那边重新读一遍配置。 */
    public static void reload() {
        if (!hasClothConfig()) {
            return;
        }
        try {
            AutoConfig.getConfigHolder(ClothConfigAdapter.class).load();
        } catch (Exception e) {
            System.err.println("[ylc-tier-tagger] 同步 cloth-config 失败：" + e);
        }
    }

    private static boolean hasClothConfig() {
        return FabricLoader.getInstance().getModContainer(CLOTH_CONFIG).isPresent();
    }
}
