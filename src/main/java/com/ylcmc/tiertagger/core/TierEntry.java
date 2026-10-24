package com.ylcmc.tiertagger.core;

/**
 * 单个模式下的段位记录（对应 pvptierweb formatPlayer 里的 tiers 数组元素）。
 *
 * <pre>
 * { "mode": "vanilla", "mode_label": "水晶", "tier": "HT1", "high": true, "level": 1, "strength": 0 }
 * </pre>
 *
 * tier 为 null 表示该模式未定级。
 */
public class TierEntry {

    public String mode;
    public String mode_label;
    public String tier;
    public Boolean high;
    public Integer level;
    public Integer strength;
}
