package com.ylcmc.tiertagger.modmenu;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.ylcmc.tiertagger.gui.ConfigScreen;
import net.minecraft.text.Text;

/**
 * ModMenu 集成：让模组在 ModMenu 列表里带一个「配置」按钮。
 *
 * <p>这是可选集成——ModMenu 没装时这个类根本不会被加载（Fabric 的 entrypoint
 * 是懒加载的，没人请求 modmenu 类型就不会去解析它），所以不影响零前置使用。
 */
public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> new ConfigScreen(Text.literal("PVPTIER 榜单设置"));
    }
}
