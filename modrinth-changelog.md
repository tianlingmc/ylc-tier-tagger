# 1.0.8

修复聊天栏在 1.21.11 因修改不可变的 siblings 列表抛出 UnsupportedOperationException，导致聊天包处理失败、客户端被「协议错误」踢出的严重问题——改为重建整条消息树，不再改动已有列表。ESC 暂停菜单（游戏内「模组」按钮）下方新增「PVPTIER 设置」按钮，点击打开设置界面。重做设置界面显示：改为自绘不透明面板，修正严重显示错误（此前为空文字按钮 + 半透明浅字叠加）；文案「回复默认」修正为「恢复默认」；列表式布局参考 AppleSkin 模组。

Fixed a crash where, in 1.21.11, decorating chat messages modified the immutable siblings list and threw UnsupportedOperationException, causing packet handling to fail and the client to be disconnected with a "protocol error" — now the whole message tree is rebuilt instead of mutating existing lists. Added a "PVPTIER 设置" button below the in-game "Mods" button in the ESC pause menu, opening the settings screen. Reworked the settings screen rendering: a self-drawn opaque panel that fixes a severe display bug (previously an empty-text button with a semi-transparent overlay); the label "回复默认" is corrected to "恢复默认"; the list layout follows the AppleSkin mod.

# 1.0.7

重做设置界面：编辑内存草稿、「保存」才写回配置，新增「取消」「回复默认」；顶部标题导航栏与底部确认脚行；优先模式项与模式选择界面显示各模式图标；内容区可滚动（右侧 ↑/↓）适配小屏。修复聊天栏在开了聊天插件的服务器上不显示段位的问题——改为按玩家名组件定位，不再依赖固定的消息结构。

Reworked the settings screen (in-memory draft, save-only-on-confirm, cancel/reset, title bar, confirmation row, mode icons, scrollable for small windows). Fixed tiers not showing in chat on servers with chat plugins by locating the player-name component instead of relying on a fixed message layout.

# 1.0.6

修复游戏初始化时，选项界面左下角「PVPTIER 榜单设置」按钮的注入方式导致的崩溃。
数据来源说明：榜单数据来自我们自己的站点 pvptier.ylcmc.top（YueLunarCourt 服务器），与第三方站点 pvptier.com 无关。

Fixed a crash on game startup caused by how the "PVPTIER 榜单设置" button was injected into the options screen.
Data source note: the tier data comes from our own site pvptier.ylcmc.top (the YueLunarCourt server) and is not related to the third-party site pvptier.com.

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
