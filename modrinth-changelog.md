# 1.0.0

首个正式版本。

The first full release.

## 显示 / Display

- 头顶名牌、Tab 玩家列表、聊天栏三处显示段位
- 聊天栏里的段位可选择放在名字前或名字后
- 八个模式各有对应图标，段位分 `HT1` ~ `HT5` 与 `LT1` ~ `LT5`
- 各段位的颜色可以调整

- The tier is shown on nametags, in the tab list and in chat
- In chat the tier can be placed before or after the name
- Every mode has its own icon, tiers range from `HT1` to `HT5` and `LT1` to `LT5`
- The colour of every tier can be adjusted

## 设置 / Settings

- 游戏内输入 `/ylctier` 打开设置界面
- 选项界面左下角有「PVPTIER 榜单设置」按钮
- 装有 ModMenu 时，模组列表里有「配置」按钮
- 装有 Cloth Config 时可用 `/config` 命令
- 可切换优先模式、显示规则与显示位置

- The settings screen opens with `/ylctier`
- There is a "PVPTIER 榜单设置" button in the bottom left of the options screen
- With ModMenu installed there is a "Config" button in the mod list
- With Cloth Config installed the standard `/config` command is available
- The preferred mode, the display rule and the position can be changed

## 数据 / Data

- 每次启动游戏自动同步一次榜单
- 同步失败时说明失败原因、当前是否沿用本地数据，并给出手动重试的入口
- 同步耗时较长时同样会给出提示
- 同步在后台进行，不影响游戏启动
- 本地缓存位于游戏的 config 目录

- The leaderboard is synced once whenever the game starts
- If the sync fails, the reason is given along with whether the last known data is used
  and how to retry manually
- A hint is also shown when the sync takes a while
- The sync runs in the background and does not affect starting the game
- The local cache lives in the config folder of the game

## 兼容 / Compatibility

- 1.20.5 ~ 1.21.11，需要 Java 21
- 1.20.1 ~ 1.20.4，需要 Java 17
- 不需要任何前置模组
- ModMenu 与 Cloth Config 为可选依赖

- 1.20.5 to 1.21.11, requires Java 21
- 1.20.1 to 1.20.4, requires Java 17
- No other mods are required
- ModMenu and Cloth Config are optional
