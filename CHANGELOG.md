# 更新日志

本项目的版本号采用 `大版本.小版本.修订号`。

## 1.0.9

- 修复聊天混用导致的启动崩溃：上一版 `ChatHudMixin` 用 `@ModifyArg` 配 `@At("HEAD")` 注入 `addMessage`，但 `@ModifyArg` 必须指向真正消费参数的指令，单凭方法头无法确定注入点，会在 Fabric 启动期（混用 apply 阶段）抛 `InvalidInjectionException`，造成游戏「初始化游戏」阶段直接崩溃、根本进不去。改为 `@ModifyVariable`（`argsOnly = true`，在方法入口替换消息参数）

- Fixed a startup crash caused by the chat mixin: the previous `ChatHudMixin` used `@ModifyArg` with `@At("HEAD")` on `addMessage`, but `@ModifyArg` requires a real argument-consuming instruction, so it threw `InvalidInjectionException` during Fabric's mixin apply at startup and the game crashed before reaching the title screen. Replaced with `@ModifyVariable` (`argsOnly = true`, which substitutes the message argument at method entry)

## 1.0.8

- 修复聊天栏在 1.21.11 因修改不可变的 `siblings` 列表抛出 `UnsupportedOperationException`，导致聊天包处理失败、客户端被「协议错误」踢出的问题（改为重建整条消息树，不再改动已有列表）
- ESC 暂停菜单（游戏内「模组」按钮）下方新增「PVPTIER 设置」按钮，点击打开设置界面
- 重做设置界面显示：改为自绘不透明面板，修正严重显示错误（此前为空文字按钮 + 半透明浅字叠加）；文案「回复默认」修正为「恢复默认」；列表式布局参考 AppleSkin 模组

- Fixed a crash where, in 1.21.11, decorating chat messages modified the immutable `siblings` list and threw `UnsupportedOperationException`, causing packet handling to fail and the client to be disconnected with a "protocol error" (now the whole message tree is rebuilt instead of mutating existing lists)
- Added a "PVPTIER 设置" button below the in-game "Mods" button in the ESC pause menu, opening the settings screen
- Reworked the settings screen rendering: a self-drawn opaque panel that fixes a severe display bug (previously an empty-text button with a semi-transparent overlay); the label "回复默认" is corrected to "恢复默认"; the list layout follows the AppleSkin mod

## 1.0.7

- 重做设置界面：改为编辑内存草稿，「保存」才写回配置；新增「取消」「回复默认」
- 设置界面新增顶部标题导航栏与底部确认脚行
- 设置界面「优先模式」项与模式选择界面均显示各模式的图标
- 设置界面内容区可滚动（右侧 ↑/↓ 按钮），适配窗口偏小的屏幕
- 修复聊天栏在开了聊天插件的服务器上不显示段位的问题（改为按玩家名组件定位，不再依赖固定的消息结构）

- Reworked the settings screen: it now edits an in-memory draft and only writes to disk on "Save"; added "Cancel" and "Reset to defaults"
- The settings screen now has a top title bar and a bottom confirmation row
- The preferred-mode row and the mode picker now show each mode's icon
- The settings content scrolls (↑/↓ buttons on the right) to fit small windows
- Fixed tiers not showing in chat on servers running chat plugins (locate the player-name component instead of relying on a fixed message layout)

## 1.0.6

- 修复游戏初始化时选项界面按钮注入导致的崩溃
- 明确榜单数据来自 pvptier.ylcmc.top，与 pvptier.com 无关

- Fixed a crash on startup caused by the options-screen button injection
- Clarified that the tier data comes from pvptier.ylcmc.top, unrelated to pvptier.com

## 1.0.0

首个正式版本。

### 显示

- 头顶名牌、Tab 玩家列表、聊天栏三处显示段位
- 聊天栏里的段位可选择放在名字前或名字后
- 八个模式各有对应图标，段位分 `HT1` ~ `HT5` 与 `LT1` ~ `LT5`
- 各段位的颜色可以调整

### 设置

- 游戏内输入 `/ylctier` 打开设置界面
- 选项界面左下角有「PVPTIER 榜单设置」按钮
- 装有 ModMenu 时，模组列表里有「配置」按钮
- 装有 Cloth Config 时可用 `/config` 命令
- 可切换优先模式、显示规则与显示位置

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

## 1.0.0 之前

测试阶段（0.1.x）。主要变化：

- 首个可运行的版本
- 取消前置模组依赖，改为零前置
- 增加游戏选项界面的设置入口
- 模组图标改用原始图片，完整显示背景与文字
- 聊天栏玩家名也显示段位
- 同时提供 1.20.1 ~ 1.20.4 与 1.20.5 ~ 1.21.11 两个版本
- 版本号与安装包命名规范化
- 补齐 Modrinth 所需的元数据与自动发版流程
