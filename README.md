# ylc-tier-tagger

1.21.11 高版本 服务器 YueLunarCourt 的 专属 PVP 榜单 Tagger 模组 | The exclusive PVP leaderboard Tagger mod for the YueLunarCourt server on Minecraft 1.21.11 (a newer version).

## 功能

- 头顶名牌与 Tab 玩家列表，在玩家名旁显示「模式图标 + 段位」
- 数据取自 PVPTIER 榜单接口，每 5 分钟刷新一次，落本地缓存
- `/ylctier <玩家名>` 查看某人在各模式的段位
- `/config` 打开配置界面（cloth-config）

## 模式

八个模式与榜单一致：斧 axe、锤 mace、合金 nethop、药水 pot、SMP smp、剑 sword、UHC uhc、水晶 vanilla。

段位为 `HT1`~`HT5` 与 `LT1`~`LT5`，强度序 `HT1 > LT1 > HT2 > LT2 > ... > HT5 > LT5`（值小者强）。

## 配置项

| 项 | 说明 |
|---|---|
| `enabled` | 总开关 |
| `showOnTab` | 是否在 Tab 玩家列表显示 |
| `renderLocation` | 段位显示在名字左侧或右侧 |
| `selectedMode` | 优先使用的模式 |
| `displayRule` | 只用选中的模式 / 只显示最高的档位 / 优先选中的模式 |
| `HT1`~`HT5`、`LT1`~`LT5` | 各段位颜色，可取色 |

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
| Fabric API | 0.141.6+1.21.11 |
| cloth-config | 21.11.160 |
| Loom | 1.14.10 |
| Gradle | 9.2.1 |

插件 id 必须写 `net.fabricmc.fabric-loom-remap`：`net.fabricmc.fabric-loom` 是「不 remap」标记插件，会强制关闭混淆，导致 Yarn 映射配不进去。

Loom 1.14 起不再默认启用 mixin 注解处理器，混流目标名由 Loom 在 remap 阶段直接改写为 intermediary 名，因此不需要 refmap 文件。

## 目录

```
src/main/java/com/ylcmc/tiertagger/
  YlcTierTagger.java               模组入口
  client/YlcTierTaggerClient.java  初始化：配置、数据拉取、定时刷新
  client/YlcTierCommand.java       /ylctier 命令
  core/TierService.java            接口拉取与本地缓存
  core/TierRegistry.java           模式注册表与段位强度序
  core/TierLabel.java              段位选取与名字拼接
  core/TierConfig.java             配置项
  mixin/PlayerEntityMixin.java     头顶名牌
  mixin/PlayerListHudMixin.java    Tab 玩家列表
tools/make-icon.py                 由 modicon.jpg 生成 mod 图标
```
