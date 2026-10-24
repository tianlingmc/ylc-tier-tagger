# 1.1.1

修复普通聊天消息（玩家自己打的字）拿不到段位标签：客户端是用 `Text.translatable("chat.type.text", 名字, 内容)` 拼这类消息的，名字存在 `TranslatableTextContent` 的 args 里、不在 siblings 里，原先只遍历 siblings 的查找方式必然漏掉；已新增对翻译组件参数的替换处理。表现为进服广播、系统提示能正常加标签，而 `玩家名: 内容` 形式的发言完全没标签。参数匹配做了保守处理：优先「arg 文本正好等于某个玩家名」，其次取最短的「包含玩家名」项，避免把正文里的名字误判成说话人。

Fixed missing tier tags on regular chat messages (text typed by players): the client composes them with `Text.translatable("chat.type.text", name, content)`, so the name lives in the `TranslatableTextContent` args rather than in siblings, and the old siblings-only walk always missed it; added replacement support for translatable arguments. Symptom: join broadcasts and system messages were tagged while `name: message` chat lines got nothing. Argument matching is conservative: prefers an arg whose text exactly equals a player name, otherwise the shortest arg containing one, so bodies are less likely to be mistaken for the sender.

修复设置页所有文字不可见：MC 的颜色是 ARGB，源码里普遍写成 `0xRRGGBB`（alpha 为 0）等于全透明，导致文字、分割线、段位色块一个都画不出来，而带 `0xff` 的行底色照常显示——这正是截图里「有色块、没文字」的原因；已统一补齐 alpha。同时修复聊天栏与玩家列表里的段位文字可能全透明（同一个 alpha 问题）。设置页滚动从「上下箭头按钮」改为原生风格的滚动条（轨道 + 滑块）并支持滚轮，方向与原版 `ScrollableWidget` 一致。拖拽滚动暂不支持：`mouseDragged` / `mouseClicked` 的签名在 1.20.x（`double,double,int`）与 1.21.11（`Click`）之间不兼容，单份源码无法同时覆盖。

Fixed the config screen showing no text at all: MC colours are ARGB and the code mostly wrote `0xRRGGBB` (alpha 0, fully transparent), so text, separators and tier swatches were never drawn while the row background (which had `0xff`) showed normally - exactly the "bars without text" seen in screenshots; alpha is now set everywhere. Also fixed tier text possibly rendering fully transparent in chat and the player list (same alpha issue). Config scrolling changed from up/down arrow buttons to a vanilla-style scrollbar (track + thumb) with wheel support, matching vanilla `ScrollableWidget` direction. Drag-to-scroll is not supported: `mouseDragged` / `mouseClicked` signatures differ between 1.20.x (`double,double,int`) and 1.21.11 (`Click`).

# 1.1.0

修复 1.20.1–1.20.4（Java 17）构建启动即崩：mixin 配置 compatibilityLevel 误写 JAVA_21，Java 17 运行时拒绝应用、所有混用失效；改为 JAVA_17，两个版本范围统一可跑。修复聊天栏段位装饰引发的两次启动崩溃（@ModifyArg@HEAD 不是合法注入点；@ModifyVariable 误用 argsOnly+index=0 把 this 槽位当参数）；最终用 @ModifyVariable 正确槽位（非静态方法第一个参数在槽位 1）替换消息参数。修正 LT5 与 LT4 默认颜色重复；修正 fabric.mod.json 描述转义错误并更正支持版本范围。数据接口偶发返回非 JSON（瞬断/网关）已做 3 次重试与本地缓存兜底，不会崩或卡启动。

Fixed a startup crash on the 1.20.1–1.20.4 (Java 17) build (mixin compatibilityLevel was wrongly JAVA_21 and the Java 17 runtime refused it, breaking every mixin); changed to JAVA_17 so both version ranges run. Fixed two startup crashes from chat tag decoration (@ModifyArg@HEAD is not a valid injection point; @ModifyVariable misused argsOnly+index=0 treating the this slot as a parameter); now @ModifyVariable with the correct slot (first parameter of a non-static method is at slot 1) substitutes the message argument. Fixed LT5 sharing LT4's default colour; fixed a bad escape in fabric.mod.json description and corrected the supported-version note. The data API occasionally returns non-JSON (outage/gateway) and is already retried 3× with a local-cache fallback, so it never crashes or hangs.

# 1.0.10

修复 1.0.9 引入的混用崩溃：ChatHudMixin 的 @ModifyVariable 误用 argsOnly=true, index=0——非静态方法的第一个参数在局部变量槽位 1（this 在 0 且不可改、argsOnly 也会把 0 判非法），导致启动期又崩。改回 index=1，在方法入口替换消息参数。

Fixed the mixin crash introduced in 1.0.9: ChatHudMixin's @ModifyVariable wrongly used argsOnly=true, index=0 (in a non-static method the first parameter is at local slot 1, this is slot 0 and cannot be modified, and argsOnly also rejects 0), which crashed at startup again. Changed back to index=1 to substitute the message argument at method entry.

# 1.0.9

修复聊天混用导致的启动崩溃：上一版 ChatHudMixin 用 @ModifyArg 配 @At("HEAD") 注入 addMessage，但 @ModifyArg 必须指向真正消费参数的指令，单凭方法头无法确定注入点，会在 Fabric 启动期（混用 apply 阶段）抛 InvalidInjectionException，造成游戏「初始化游戏」阶段直接崩溃、根本进不去。改为 @ModifyVariable（argsOnly = true，在方法入口替换消息参数）。

Fixed a startup crash caused by the chat mixin: the previous ChatHudMixin used @ModifyArg with @At("HEAD") on addMessage, but @ModifyArg requires a real argument-consuming instruction, so it threw InvalidInjectionException during Fabric's mixin apply at startup and the game crashed before reaching the title screen. Replaced with @ModifyVariable (argsOnly = true, which substitutes the message argument at method entry).

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
