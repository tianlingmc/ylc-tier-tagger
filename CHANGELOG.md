# 更新日志

本项目的版本号采用 `大版本.小版本.修订号`。

## 1.0.0

首个公开版本。

### 显示

- 头顶名牌、Tab 玩家列表、聊天栏三处显示段位
- 聊天栏里的段位可选择放在名字前或名字后
- 八个模式各有对应图标，段位分 `HT1` ~ `HT5` 与 `LT1` ~ `LT5`
- 各段位的颜色可以调整

### 设置

- 游戏内输入 `/ylctier` 打开设置界面
- ESC 暂停菜单（游戏内「模组」按钮）下方有「PVPTIER 设置」按钮
- 装有 ModMenu 时，模组列表里有「配置」按钮
- 装有 Cloth Config 时可用 `/config` 命令
- 设置页编辑的是内存草稿，只有点「保存」才写回配置，另有「取消」「恢复默认」
- 设置页内容区用原生风格的滚动条，支持滚轮

### 数据

- 每次启动游戏自动同步一次榜单
- 同步失败时说明失败原因、当前是否沿用本地数据，并给出手动重试的入口
- 同步耗时较长时同样会给出提示
- 同步在后台进行，不影响游戏启动
- 本地缓存位于游戏的 config 目录

### 兼容

- 1.20.5 ~ 1.21.11，需要 Java 21
- 1.20.1 ~ 1.20.4，需要 Java 17
- 不需要任何前置模组
- ModMenu 与 Cloth Config 为可选依赖

### 稳定性

- 聊天栏对两类消息都能正确加标签：服务端拼好的广播与系统提示，以及客户端本地用
  `Text.translatable("chat.type.text", 名字, 内容)` 拼出来的普通发言（后者的名字存在翻译
  组件的参数里、不在兄弟节点里，两种结构都已覆盖）
- 消息树是重建而不是就地修改，不会触碰 1.21.x 里不可变的 `siblings` 列表，也就不会因
  协议错误被踢出
- 两个版本范围的构建都能正常启动（mixin 的 `compatibilityLevel` 与各自运行的 Java 匹配）
- 榜单接口偶发返回非 JSON（瞬断或网关）时会自动重试 3 次并回退到本地缓存，不会崩溃或卡启动

The first public release.

### Display

- The tier is shown on nametags, in the tab list and in chat
- In chat the tier can be placed before or after the name
- Every mode has its own icon, tiers range from `HT1` to `HT5` and `LT1` to `LT5`
- The colour of every tier can be adjusted

### Settings

- Open the settings screen with `/ylctier` in game
- A "PVPTIER 设置" button sits below the in-game "Mods" button in the ESC pause menu
- With ModMenu installed there is a "Config" button in the mod list
- With Cloth Config installed the standard `/config` command is available
- The settings screen edits an in-memory draft and only writes to disk on "Save"; "Cancel" and "Reset to defaults" are available too
- The settings content uses a vanilla-style scrollbar and supports the mouse wheel

### Data

- The leaderboard is synced once whenever the game starts
- If the sync fails, the reason is given along with whether the last known data is used and how to retry manually
- A hint is also shown when the sync takes a while
- The sync runs in the background and does not affect starting the game
- The local cache lives in the config folder of the game

### Compatibility

- 1.20.5 to 1.21.11, requires Java 21
- 1.20.1 to 1.20.4, requires Java 17
- No other mods are required
- ModMenu and Cloth Config are optional

### Stability

- Chat tagging covers both message shapes: broadcasts and system hints composed by the server, and regular chat that the client composes locally with `Text.translatable("chat.type.text", name, content)` (there the name lives in the translatable arguments rather than in the siblings)
- The message tree is rebuilt instead of being mutated in place, so the immutable `siblings` list on 1.21.x is never touched and the client is not disconnected with a protocol error
- Both version ranges start normally (the mixin `compatibilityLevel` matches the Java each of them runs on)
- When the leaderboard API occasionally returns non-JSON (brief outage or gateway), the mod retries 3 times and falls back to the local cache instead of crashing or hanging
