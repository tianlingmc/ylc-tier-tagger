package com.ylcmc.tiertagger.core;

import java.util.List;

/** 排行榜里的一条玩家记录。 */
public class PlayerEntry {

    public String name;
    public String region;
    public int points;
    public int rank;
    public String title;
    public List<TierEntry> tiers;

    /** 取该玩家在指定模式下的档位，未定级返回 null。 */
    public String tierOf(String modeKey) {
        if (tiers == null) return null;
        for (TierEntry t : tiers) {
            if (t != null && t.mode != null && t.mode.equals(modeKey)) {
                return t.tier;
            }
        }
        return null;
    }
}
