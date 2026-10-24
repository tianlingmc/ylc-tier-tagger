package com.ylcmc.tiertagger.core;

import java.util.List;

/**
 * PVPTIER 排行榜接口返回体（对应 pvptierweb 的 GET /api/list）。
 *
 * <pre>
 * {
 *   "ok": true,
 *   "endpoint": "/api/list",
 *   "meta": { "mode": "overall", "total": 123, "count": 123, "limit": 200, "offset": 0, ... },
 *   "data": [ { "name": "...", "region": "...", "points": 123, "rank": 1, "tiers": [ ... ] } ]
 * }
 * </pre>
 */
public class ApiResponse {

    public boolean ok;
    public Meta meta;
    public List<PlayerEntry> data;

    public static class Meta {
        public String mode;
        public String mode_label;
        public String query;
        public int total;
        public int count;
        public int limit;
        public int offset;
        public String generated_at;
    }
}
