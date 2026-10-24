package com.ylcmc.tiertagger.modmenu;

import com.ylcmc.tiertagger.core.TierConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

/**
 * cloth-config 用的配置类。
 *
 * <p>直接继承 {@link TierConfig}，这样配置项只有一份。字段名与自研配置一致，
 * 两边读写同一个文件 {@code config/ylc-tier-tagger.json}，装了 cloth-config
 * 也能接着用。
 *
 * <p>这个类只有在检测到 cloth-config 存在时才会被加载，因此零前置环境下
 * 不会因为缺少 ConfigData 接口而出问题。
 */
@Config(name = "ylc-tier-tagger")
public class ClothConfigAdapter extends TierConfig implements ConfigData {
}
