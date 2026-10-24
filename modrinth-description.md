# YLCTierTagger

> 1.21.11 高版本 服务器 YueLunarCourt 的 专属 PVP 榜单 Tagger 模组
>
> The exclusive PVP leaderboard Tagger mod for the YueLunarCourt server on Minecraft 1.21.11 (a newer version).

在 YueLunarCourt 的 PVP 榜单上，每位玩家都有一份属于自己的段位。
这个模组让客户端把段位直接显示出来：玩家的名字旁边会出现对应的模式图标与段位。

On the YueLunarCourt PVP leaderboard, every player has a tier of their own.
This mod displays it on the client: the mode icon and tier of the player appear next to their name.

## 显示位置 / Where it appears

| 位置 / Location | 说明 / Description |
|---|---|
| 头顶名牌 / Nametag | 站在玩家附近时，名字旁显示段位 / Shown next to the name when you are close to the player |
| Tab 玩家列表 / Tab list | 按 Tab 打开的玩家列表中显示段位 / Shown in the player list opened with Tab |
| 聊天栏 / Chat | 聊天消息提到某个玩家时，名字后面跟着段位 / Shown after the name when a chat message mentions that player |

聊天栏里的段位放在名字前还是名字后可以设置，Tab 列表与头顶名牌的显示位置同样可调。

The position of the tier in chat, before or after the name, can be configured.
The position in the tab list and on the nametag can be adjusted the same way.

## 模式与段位 / Modes and tiers

| 模式 / Mode | 榜单标识 / Leaderboard key |
|---|---|
| 斧 / Axe | `axe` |
| 锤 / Mace | `mace` |
| 合金 / Netherite | `nethop` |
| 药水 / Pot | `pot` |
| SMP / SMP | `smp` |
| 剑 / Sword | `sword` |
| UHC / UHC | `uhc` |
| 水晶 / Crystal | `vanilla` |

段位分高段与低段两档，共 `HT1` ~ `HT5` 与 `LT1` ~ `LT5`。
同一模式内 `HT1` 最高、`LT5` 最低。各段位的颜色可以在设置里调整。

Tiers come in two groups, `HT1` to `HT5` and `LT1` to `LT5`.
Within a mode, `HT1` is the highest and `LT5` the lowest.
The colour of each tier can be adjusted in the settings.

## 游戏内文本 / In-game text

游戏内的界面与提示使用中文，下面同时给出英文说明。

The in-game interface and messages are in Chinese. English explanations are given below.

| 位置 / Location | 游戏内文本 / In-game text | 说明 / Description |
|---|---|---|
| 命令 / Command | `/ylctier` | 打开设置界面 / Opens the settings screen |
| 命令 / Command | `/ylctier refresh` | 手动重新同步榜单 / Syncs the leaderboard again |
| 命令 / Command | `/ylctier <玩家名>` | 查看某人的段位 / Shows the tiers of a player |
| 选项界面 / Options screen | PVPTIER 榜单设置 | 设置入口 / Entry to the settings screen |
| 设置项 / Setting | 总开关 | 模组总开关 / Master switch |
| 设置项 / Setting | Tab 列表显示 | 是否在 Tab 列表显示 / Whether to show in the tab list |
| 设置项 / Setting | 聊天栏显示 | 是否在聊天栏显示 / Whether to show in chat |
| 设置项 / Setting | 显示位置 | 段位在名字前或后 / Whether the tier goes before or after the name |
| 设置项 / Setting | 优先模式 | 优先显示的模式 / The mode that is preferred |
| 设置项 / Setting | 显示规则 | 单一模式或最高档位 / Single mode or the highest tier |
| 设置项 / Setting | 各段位颜色 | 每个段位的颜色 / The colour of each tier |
| 同步成功 / On success | 已同步 N 名玩家的段位 | N 名玩家的数据已同步 / The tiers of N players have been synced |
| 同步失败 / On failure | `[PVPTIER] 排行榜同步失败：<原因>` | 同步失败并说明原因 / The sync failed and the reason is given |
| 手动重试提示 / Retry hint | 可输入 /ylctier refresh 手动重试。 | 提示可手动重试 / Tells you that a manual retry is possible |

## 设置 / Settings

有三种方式打开设置界面：

- 游戏内输入 `/ylctier`
- 选项界面左下角的「PVPTIER 榜单设置」
- 安装了 ModMenu 的话，模组列表里的「配置」按钮

安装了 Cloth Config 时，还可以使用标准的 `/config` 命令。

There are three ways to open the settings screen:

- Type `/ylctier` in game
- The "PVPTIER 榜单设置" button in the bottom left of the options screen
- The "Config" button in the mod list, if ModMenu is installed

If Cloth Config is installed, the standard `/config` command is available as well.

## 依赖 / Dependencies

模组本身不需要任何前置模组，只依赖 Fabric Loader。
ModMenu 与 Cloth Config 属于可选依赖，装上之后会多出对应的入口，不装也能正常使用。

The mod itself does not require any other mods and only needs Fabric Loader.
ModMenu and Cloth Config are optional. With them installed a few more entry points become
available, but everything works without them.

## 支持的客户端版本 / Supported client versions

| 客户端 / Client | 需要的 Java / Java | 下载哪个文件 / File to download |
|---|---|---|
| 1.20.5 ~ 1.21.11 | Java 21 | `YLCTierTagger-<版本>-fabric-mc1.21.11.jar` |
| 1.20.1 ~ 1.20.4 | Java 17 | `YLCTierTagger-<版本>-fabric-mc1.20.4.jar` |

1.20.5 之前的 Minecraft 运行在 Java 17 上，文件格式与新版不通用，因此单独提供一份。

Minecraft versions before 1.20.5 run on Java 17 and use a different file format, so they are
provided as a separate file.

## 数据 / Data

段位数据取自 PVPTIER 榜单接口。

- 每次启动游戏会自动同步一次
- 同步结果缓存在游戏的 config 目录中，站点临时不可用时会继续沿用上一次的数据
- 同步失败时会在游戏内说明原因，并提示可以用 `/ylctier refresh` 手动重试
- 同步在后台进行，不会影响游戏启动

The tier data is taken from the PVPTIER leaderboard API.

- It is synced once automatically whenever the game starts
- The result is cached in the config folder, and the last known data keeps being used
  while the site is unavailable
- If the sync fails, the reason is shown in game together with a hint to retry with
  `/ylctier refresh`
- The sync runs in the background and does not affect starting the game
