# CnTierTagger 上游代码分析

分析对象：https://github.com/tmdakm/CnTierTagger
本地位置：`_ref/CnTierTagger`（上游只读参考，HEAD = `000e9c8` / ver 1.7.3）
本目录 `ylc-tier-tagger/` 预留给自研版本

---

## 一、项目定位

Fabric **客户端**模组。定时拉取「中国 1.9+ PVP TierList」的排名数据，把玩家的段位渲染到名字旁边（头顶名牌 + Tab 列表）。

- 作者 Huangyu（tmdakm），Apache-2.0
- 最新版 1.7.3，支持 Minecraft 26.1 ~ 26.2
- 分发：Modrinth `mod/cntiertagger`，随 GitHub Release 自动构建发布
- 交互：`/cntier <玩家名>` 查某人全部模式档位；ModMenu 内 Cloth-Config 配置界面

---

## 二、技术栈

| 项 | 值 |
|---|---|
| Java | 25（gradle toolchain） |
| 构建 | Fabric Loom 1.17-SNAPSHOT |
| Minecraft | 26.2 |
| Loader | 0.19.3 |
| 依赖 | fabric-api 0.157.0+26.2 / cloth-config 26.2.155 / modmenu 20.0.1 |
| 网络 | JDK 自带 `java.net.http.HttpClient`（无第三方 HTTP 库） |
| JSON | Gson（MC 自带） |
| 打包 | GitHub Actions，release 触发 → 上传 Modrinth + Release 附件 |

---

## 三、源码结构（Java 共 523 行）

```
win/huangyu/cntiertagger/
  Cntiertagger.java                  ModInitializer，空实现占位
  client/CntiertaggerClient.java     ClientModInitializer：注册配置/命令 + 每 tick 检查缓存
  TierManager.java                   核心：拉取、缓存、档位选择、渲染（228 行）
  ConfigManager.java                 AutoConfig 包装 + DisplayRule/Mode 枚举
  ModConfig.java                     配置项 + 档位颜色表
  PlayerData.java                    反序列化 DTO
  TierMode.java                      模式 -> (emoji, color)
  PlayerNameHelper.java              取 GameProfile 原名
  command/CntierCommand.java         /cntier <name>
  mixin/PlayerMixin.java             注入 Player#getDisplayName（头顶名牌）
  mixin/PlayerTabOverlayMixin.java   注入 PlayerTabOverlay#getNameForDisplay（Tab 列表）
  modmenu/CntierTaggerMenuIntegration.java
```

资源：`assets/cntiertagger/textures/fonts/*.png`（8 张 32x32 RGBA 图标）、`assets/minecraft/font/default.json`（位图字体注册）、`assets/cntiertagger/lang/{zh_cn,en_us}.json`。

---

## 四、数据流

1. **启动**：`CntiertaggerClient` 静态字段 `new TierManager()` → 构造函数先 `refreshCache()`（异步）再 `loadCache()`（读本地缓存）。
2. **缓存**：文件 `./config/tier_cache.json`（相对游戏工作目录），有效期 **4 分钟**。
3. **刷新**：每客户端 tick 调 `checkCacheExpiration()`，过期则 `refreshCache()`。失败后 1 分钟内不重试。
4. **拉取**：`GET https://cntier.win/api/ranking/overall` → Gson 反序列化为 `PlayerData[]` → 整体覆盖 `playerMap`（ConcurrentHashMap）。
5. **渲染**：Mixin 拦截名字 Component → `getPlayerTier(name)` → 拼成 `图标 段位 | 名字`（LEFT）或 `名字 | 段位 图标`（RIGHT）。

### 期望的数据结构

```json
{
  "uuid": "...",
  "name": "...",
  "region": "...",
  "modeTiers": {
    "Axe": "HT1",
    "Sword": "LT2",
    "Mace": "Retired HT3",
    "SMP": "HT2 | HT1"
  }
}
```

`modeTiers` 的三种取值语义：

- 纯 `HT1` ~ `LT5`：当前段位
- `Retired XXX`：退役，渲染成 `R` + XXX（R 单独着色）
- `A | B`：峰值 | 当前，取 B

段位强度序（值小者强）：`HT1 > LT1 > HT2 > LT2 > HT3 > LT3 > HT4 > LT4 > HT5 > LT5`

---

## 五、显示规则（DisplayRule）

| 规则 | 行为 |
|---|---|
| `SELECTED_ONLY` | 只用选定模式；该模式无档位则完全不显示 |
| `HIGHEST_ONLY` | 显示该玩家所有模式里段位最高的那个（此时"选定模式"失效） |
| `MIXED` | 优先选定模式，没有则回落到最高档位模式 |

配置项：`enableMod`、`showOnPlayerList`、`selectedMode`、`displayRule`、`renderLocation(LEFT/RIGHT)`、10 个档位颜色 + `Retired` 颜色（全部可在 ModMenu 里调色）。

---

## 六、图标字体机制

8 个模式图标用私用区码位 `U+E701` ~ `U+E708`，通过**覆盖** `assets/minecraft/font/default.json` 注册 bitmap provider（每个 png 一个 provider，`ascent: 8`、`height: 8`）。

**风险点**：资源包/mod 的同路径文件是整文件替换而非合并。理论上覆盖后原版 ASCII provider 会丢失，导致文字变 notdef 方框。但该模组实际使用中没有相关 issue（仓库仅 1 个 issue，是帧率问题），说明实践中未暴露。自行开发时建议保留原版 providers，或改用 `reference` provider。

---

## 七、代码问题清单（静态审查 + 历史 PR 比对）

1. **API 地址硬编码**：`TierManager.java:17` 写死 `https://cntier.win/api/ranking/overall`，无配置项。换数据源必须改代码重新编译。
2. **HTTP 无超时**：`HttpRequest.newBuilder()` 未设 `timeout()`。服务器挂起时 `isRefreshing` 永久为 true，之后**永不刷新**。
3. **Executor 泄漏**：`refreshCache()` 每次 `Executors.newSingleThreadExecutor()` 后从不 `shutdown()`。PR #2 曾用 try-with-resources 修过，后续重构把修复丢了。
4. **异常抛在池线程**：`catch` 里 `throw new RuntimeException("Failed to refresh cache", e)`，PR #2 原本改成 `printStackTrace()`。
5. **构造函数顺序反了**：先 `refreshCache()` 再 `loadCache()`；而 `loadCache()` 发现缓存过期时再调 `refreshCache()` 会被 1 分钟重试间隔挡掉 → 启动瞬间可能无数据可用。
6. **缓存路径不稳**：`./config/tier_cache.json` 相对游戏工作目录，多实例或服务端环境会互相覆盖。
7. **§ 颜色码失效**：`CntierCommand` 里用 `"§e"` / `"§7"`，现代 MC 的 Component 不解析 § → 颜色不生效。`TierMode.color` 字段（`§8`/`§b`/`§9`…）是**死代码**，全项目无引用。
8. **每帧分配**：`appendTier()` 每次调用新建多个 Component 并 `copy()`；`Player#getDisplayName` 每帧对每个可见玩家调用一次 → GC 压力，这是仓库 issue #1「部分环境显著掉帧」的根因方向。
9. **全量拉取**：每 4 分钟全量下载整个榜单，无 ETag / 增量。
10. **`uuid` 字段无用**：DTO 里反序列化了但没人读。

### 历史 PR #2（已合并，后被重构覆盖）

- 缓存时长 `4 小时` → `4 分钟`（排行榜变得快）
- `refreshCache` 改为 try-with-resources 管理 executor
- `onInitializeClient` 里的 `tierManager.loadCache()` 调用被移除
- `checkCacheExpiration` 加上过期判断

---

## 八、与自研 pvptierweb 对接的差距

上游期望 vs `pvptierweb` 实际输出：

| 维度 | CnTierTagger 期望 | pvptierweb 实际 |
|---|---|---|
| 端点 | `https://cntier.win/api/ranking/overall` | `GET /api/list?mode=overall&limit=&offset=` |
| 顶层结构 | 裸数组 `[...]` | `{ok, endpoint, meta, data:[...]}` |
| 单条结构 | `{uuid, name, region, modeTiers:{}}` | `{name, region, points, rank, title, tiers:[{mode, mode_label, tier, high, level, strength}]}` |
| 模式键 | `Axe/Sword/Mace/BUHC/NPOT/Potion/Vanilla/SMP` | `axe/sword/mace/uhc/pot/nethop/vanilla/smp` |
| 档位值 | `HT1`/`LT2`，另支持 `Retired X`、`峰值 \| 当前` | 纯 `HT1`..`LT5`，无退役与峰值概念 |
| 分页 | 无，一次全量 | `limit` 上限 200，需翻页或另开端点 |
| 退役态 | 支持 | 不支持 |

**注意**：只改 `API_URL` 会直接炸 —— Gson 把 `{ok,data}` 当 `PlayerData[]` 解析会抛异常；即使侥幸解析出对象，`data.modeTiers` 为 null，`getPlayerTier` 里 `data.modeTiers.getOrDefault(...)` 会 NPE。

---

## 九、自研改造清单

### 服务端（pvptierweb）

1. 加一个模组专用端点（如 `GET /api/tier-tag`），一次返回全量、扁平化字段，避开 200 条分页限制。
2. 字段对齐模组 DTO：`name`、`region`、`modeTiers: { 模式键: 段位 }`。
3. 模式键统一（建议服务端输出与 mod 内部一致，或在 mod 里做一张映射表）。
4. 若要支持"退役 / 峰值"，需要在 `players` 表加状态位——当前表结构只有 `tier_0`..`tier_7` 纯段位列。
5. 图标：站内是 SVG（Axe.svg 等 8 张），mod 的位图字体需要 PNG，需另转一套 32x32 RGBA。

### 客户端（ylc-tier-tagger）

1. API 地址改成配置项（`ModConfig` 加 `apiBaseUrl`），默认可指向自己的接口。
2. 新写 DTO 适配 `pvptierweb` 的 `{ok, data:[{name, region, tiers:[{mode, tier}]}]}`，`tiers` 数组转 Map。
3. 模式键映射：`uhc→BUHC`、`nethop→NPOT`、`pot→Potion`、`vanilla→Vanilla`，其余同名。
4. HTTP 加 `timeout(Duration.ofSeconds(10))`；executor 改单例常驻或 try-with-resources。
5. 缓存路径改到 `FabricLoader.getInstance().getConfigDir()`。
6. 修 `§` 颜色码：改用 `Style.withColor(TextColor)`；`TierMode.color` 要么删掉要么接上。
7. 降每帧分配：`appendTier` 结果按玩家名做缓存，避免每帧重建 Component。
8. `assets/minecraft/font/default.json` 保留原版 providers 或用 `reference` 兜底。
9. 构造函数改为先 `loadCache()` 再 `refreshCache()`。

---

## 十、其他事实

- 上游仓库只有一个 issue（#1 帧率），已被 PR #2 关闭；无字体/文字相关反馈。
- `cntier.win` 当前返回 Cloudflare 521（源站不可达），无法直接抓取真实响应样本，接口结构以代码反推为准。
- Modrinth 页面显示「Published last year / Updated 2 months ago」。
