# ylc-tier-tagger

1.21.11 高版本 服务器 YueLunarCourt 的 专属 PVP 榜单 Tagger 模组 | The exclusive PVP leaderboard Tagger mod for the YueLunarCourt server on Minecraft 1.21.11 (a newer version).

## 功能

- 头顶名牌与 Tab 玩家列表，在玩家名旁显示「模式图标 + 段位」
- **进程启动时拉一次最新榜单**，进游戏后弹窗告知数据状态（成功显示人数，失败显示原因）
- 数据取自 PVPTIER 榜单接口，另有本地缓存兜底，缓存写在 `config/ylc-tier-tagger/tier_cache.json`
- **零前置**：只依赖 Fabric Loader，不需要 fabric-api、cloth-config 或任何其他模组
- `/ylctier` 打开设置界面（自绘），`/ylctier refresh` 手动重拉，`/ylctier <玩家名>` 查看某人的段位

## 模式

八个模式与榜单一致：斧 axe、锤 mace、合金 nethop、药水 pot、SMP smp、剑 sword、UHC uhc、水晶 vanilla。

段位为 `HT1`~`HT5` 与 `LT1`~`LT5`，强度序 `HT1 > LT1 > HT2 > LT2 > ... > HT5 > LT5`（值小者强）。

## 配置项

设置在游戏内 `/ylctier` 界面里改，写入 `config/ylc-tier-tagger/settings.json`。

| 项 | 说明 |
|---|---|
| `enabled` | 总开关 |
| `showOnTab` | 是否在 Tab 玩家列表显示 |
| `renderLocation` | 段位显示在名字左侧或右侧 |
| `selectedMode` | 优先使用的模式 |
| `displayRule` | 只用选中的模式 / 只显示最高的档位 / 优先选中的模式 |
| `HT1`~`HT5`、`LT1`~`LT5` | 各段位颜色，界面里点按钮循环切换预设色 |

## 构建

```bash
./gradlew build
```

产物在 `build/libs/ylc-tier-tagger-<版本>.jar`，直接放进客户端 `mods` 目录。

环境要求：JDK 21（Minecraft 1.21.11 的运行时要求）。

## 构建环境

| 项 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| 映射 | Yarn 1.21.11+build.6 |
| Fabric Loader | 0.19.3 |
| Loom | 1.14.10 |
| Gradle | 9.2.1 |

运行时只依赖 Fabric Loader，编译期另外用到 MC 自带的 Brigadier 与 Gson。

插件 id 必须写 `net.fabricmc.fabric-loom-remap`：`net.fabricmc.fabric-loom` 是「不 remap」标记插件，会强制关闭混淆，导致 Yarn 映射配不进去。

Loom 1.14 起不再默认启用 mixin 注解处理器，混流目标名由 Loom 在 remap 阶段直接改写为 intermediary 名，因此不需要 refmap 文件。

## 零前置是怎么做到的

| 原本依赖 | 换成 |
|---|---|
| fabric-api 的 `ClientTickEvents` | `MinecraftClientMixin` 注入 `MinecraftClient.tick` |
| fabric-api 的 `ClientCommandRegistrationCallback` | 注入拿到 `ClientPlayNetworkHandler`，把命令注册到游戏自带的 `CommandDispatcher<ClientCommandSource>` |
| cloth-config 的配置界面 | 自绘 `ConfigScreen`，配置用 Gson 存 JSON |
| cloth-config 的 `/config` 命令 | 自己的 `/ylctier` 命令 |

## 目录

```
src/main/java/com/ylcmc/tiertagger/
  YlcTierTagger.java               模组入口
  client/YlcTierTaggerClient.java  初始化：配置、启动拉取、tick 处理
  client/YlcTierCommand.java       /ylctier 命令
  core/TierService.java            接口拉取与本地缓存
  core/TierRegistry.java           模式注册表与段位强度序
  core/TierLabel.java              段位选取与名字拼接
  core/TierConfig.java             配置项与读写
  gui/ConfigScreen.java            自绘设置界面
  mixin/MinecraftClientMixin.java  tick 钩子：命令注册、刷新、提示
  mixin/PlayerEntityMixin.java     头顶名牌
  mixin/PlayerListHudMixin.java    Tab 玩家列表
tools/make-icon.py                 由 modicon.jpg 生成 mod 图标
```
