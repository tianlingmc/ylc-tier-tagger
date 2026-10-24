# ylc-tier-tagger

1.21.11 高版本 服务器 YueLunarCourt 的 专属 PVP 榜单 Tagger 模组 | The exclusive PVP leaderboard Tagger mod for the YueLunarCourt server on Minecraft 1.21.11 (a newer version).

## 功能

- 头顶名牌、Tab 玩家列表、**聊天栏里的玩家名**，都带上「模式图标 + 段位」
- 聊天栏里的标签放在名字前面还是后面，跟「显示位置」设置联动
- **进程启动时拉一次最新榜单**，进游戏后弹窗告知数据状态（成功显示人数，失败显示原因）
- 数据取自 PVPTIER 榜单接口，另有本地缓存兜底，缓存写在 `config/ylc-tier-tagger/tier_cache.json`
- **零前置**：只依赖 Fabric Loader，不需要 fabric-api、cloth-config 或任何其他模组
- **ModMenu / cloth-config 是可选的**：装了就有模组列表里的「配置」按钮和标准的 `/config` 命令，没装也能正常用
- 设置界面有四个入口：游戏内 `/ylctier`、游戏「选项」界面左下角的「PVPTIER 榜单设置」按钮、ModMenu 里的「配置」按钮、装了 cloth-config 时的 `/config`
- `/ylctier refresh` 手动重拉，`/ylctier <玩家名>` 查看某人的段位

## 模式

八个模式与榜单一致：斧 axe、锤 mace、合金 nethop、药水 pot、SMP smp、剑 sword、UHC uhc、水晶 vanilla。

段位为 `HT1`~`HT5` 与 `LT1`~`LT5`，强度序 `HT1 > LT1 > HT2 > LT2 > ... > HT5 > LT5`（值小者强）。

## 配置项

设置在游戏内 `/ylctier` 界面里改，写入 `config/ylc-tier-tagger.json`。装了 cloth-config 的话两边共用这一个文件。

| 项 | 说明 |
|---|---|
| `enabled` | 总开关 |
| `showOnTab` | 是否在 Tab 玩家列表显示 |
| `showInChat` | 是否在聊天消息的玩家名后显示 |
| `renderLocation` | 段位显示在名字左侧或右侧（头顶名牌、Tab、聊天栏统一） |
| `selectedMode` | 优先使用的模式 |
| `displayRule` | 只用选中的模式 / 只显示最高的档位 / 优先选中的模式 |
| `HT1`~`HT5`、`LT1`~`LT5` | 各段位颜色，界面里点按钮循环切换预设色 |

## 构建

```bash
./gradlew build
```

产物在 `build/libs/TLCTierTagger-<版本号>-fabric.jar`，直接放进客户端 `mods` 目录。

环境要求：JDK 21（Minecraft 1.20.5 及以后的运行时要求）。

### 版本号

版本号格式 `大版本.小版本.修订号`，目前是 beta 阶段，保持在 `0.x.y` 不进 1.0.0。
**每次构建第三位自动 +1**（0.1.0 → 0.1.1 → 0.1.2 …），写回 `gradle.properties` 的 `mod_version`。
只想重新打一次包、不想动版本号时加 `-PnoVersionBump`。

### 支持的客户端版本

| 客户端 | 产物 | 说明 |
|---|---|---|
| 1.20.5 ~ 1.21.11 | `TLCTierTagger-<版本>-fabric.jar` | 同一个包，Java 21 |
| 1.20.1 ~ 1.20.4 | 同名，需要单独构建 | 这些版本跑在 Java 17 上，字节码版本不同 |

跨版本靠的是 intermediary 映射：Yarn 的类名与方法名在版本间会改，但 intermediary 名基本稳定，
所以同一份字节码能同时跑在多个版本上。为此做了两处适配：

- 文本构造用 `Text.literal`（intermediary `method_43470`，1.20.1 起一直没变），
  而不是 `Text.of`（1.20/1.20.6/1.21.4+ 各是不同 intermediary 名）
- 玩家名用反射取（`GameProfile` 在 1.21.9 改成了 record，`name()` 与 `getName()` 两边都兼容）

给 1.20.4 单独出包：

```bash
./gradlew build -PnoVersionBump -PmcVersion=1.20.4 -PyarnVersion=1.20.4+build.3 -PjavaRelease=17
```

## 构建环境

| 项 | 版本 |
|---|---|
| Minecraft | 1.21.11 |
| 映射 | Yarn 1.21.11+build.6 |
| Fabric Loader | 0.19.3 |
| Loom | 1.14.10 |
| Gradle | 9.2.1 |
| ModMenu（可选） | 17.0.1 |
| cloth-config（可选） | 21.11.160 |

运行时只依赖 Fabric Loader。ModMenu 与 cloth-config 只在编译期与 remap 时需要，运行时玩家装了才有对应入口，没装不受影响。ModMenu 从 Modrinth 的 Maven 代理拉（`https://api.modrinth.com/maven`），避开官方 maven 仓库在本机连不上的问题。

插件 id 必须写 `net.fabricmc.fabric-loom-remap`：`net.fabricmc.fabric-loom` 是「不 remap」标记插件，会强制关闭混淆，导致 Yarn 映射配不进去。

Loom 1.14 起不再默认启用 mixin 注解处理器，混流目标名由 Loom 在 remap 阶段直接改写为 intermediary 名，因此不需要 refmap 文件。

## 零前置是怎么做到的

| 原本依赖 | 换成 |
|---|---|
| fabric-api 的 `ClientTickEvents` | `MinecraftClientMixin` 注入 `MinecraftClient.tick` |
| fabric-api 的 `ClientCommandRegistrationCallback` | 注入拿到 `ClientPlayNetworkHandler`，把命令注册到游戏自带的 `CommandDispatcher<ClientCommandSource>` |
| cloth-config 的配置界面 | 自绘 `ConfigScreen`，配置用 Gson 存 JSON |
| cloth-config 的 `/config` 命令 | 自己的 `/ylctier` 命令 |

ModMenu 与 cloth-config 改成**可选集成**（`suggests` 而非 `depends`）：

- ModMenu 集成类实现 `ModMenuApi`，通过 fabric.mod.json 的 `modmenu` entrypoint 注册。Fabric 的 entrypoint 是懒加载的，ModMenu 没装就没人请求这个类型，这个类根本不会被加载，所以零前置环境不受影响。
- cloth-config 用 `ClothConfigBootstrap` 包一层：先检测 `FabricLoader.getModContainer("cloth-config")`，装了才反射式调用 `AutoConfig.register`。配置类 `ClothConfigAdapter` 直接继承自研的 `TierConfig`，两边读写同一个 `config/ylc-tier-tagger.json`，不会各存一份。
- 玩家若在 cloth-config 界面改了设置，客户端每 2 秒比对一次配置文件时间戳并重读，两边保持同步。

## 目录

```
src/main/java/com/ylcmc/tiertagger/
  YlcTierTagger.java               模组入口
  client/YlcTierTaggerClient.java  初始化：配置、启动拉取、tick 处理
  client/YlcTierCommand.java       /ylctier 命令
  core/TierService.java            接口拉取与本地缓存
  core/TierRegistry.java           模式注册表与段位强度序
  core/TierLabel.java              段位选取与名字拼接
  core/ChatTagger.java             聊天消息玩家名加标签
  core/PlayerNames.java            反射取玩家名（跨 GameProfile 改动）
  core/TierConfig.java             配置项与读写
  gui/ConfigScreen.java            自绘设置界面
  gui/ModeSelectScreen.java        tier 模式选择界面
  modmenu/ModMenuIntegration.java  ModMenu 可选集成
  modmenu/ClothConfigAdapter.java  cloth-config 可选集成（继承 TierConfig）
  modmenu/ClothConfigBootstrap.java  cloth-config 探测与注册
  mixin/MinecraftClientMixin.java  tick 钩子：命令注册、刷新、提示
  mixin/PlayerEntityMixin.java     头顶名牌
  mixin/ChatHudMixin.java          聊天栏玩家名
  mixin/PlayerListHudMixin.java    Tab 玩家列表
tools/make-icon.py                 由 modicon.jpg 生成 mod 图标
```
