# YLCTierTagger

> 1.21.11 高版本 服务器 YueLunarCourt 的 专属 PVP 榜单 Tagger 模组
>
> The exclusive PVP leaderboard Tagger mod for the YueLunarCourt server on Minecraft 1.21.11 (a newer version).

在 YueLunarCourt 的 PVP 榜单上，每位玩家都有一份属于自己的段位。
这个模组让客户端把段位直接显示出来：玩家的名字旁边会出现对应的模式图标与段位。

## 显示位置

| 位置 | 说明 |
|---|---|
| 头顶名牌 | 站在玩家附近时，名字旁显示段位 |
| Tab 玩家列表 | 按 Tab 打开的玩家列表中显示段位 |
| 准星目标 | 准星指向某个玩家时，准星旁显示对方的名字、名次、积分、各模式段位与称号 |

段位放在名字前还是名字后可以设置，名牌与 Tab 列表两处都适用。

## 模式与段位

| 模式 | 榜单标识 |
|---|---|
| 斧 | `axe` |
| 锤 | `mace` |
| 合金 | `nethop` |
| 药水 | `pot` |
| SMP | `smp` |
| 剑 | `sword` |
| UHC | `uhc` |
| 水晶 | `vanilla` |

段位分高段与低段两档，共 `HT1` ~ `HT5` 与 `LT1` ~ `LT5`。
同一模式内 `HT1` 最高、`LT5` 最低。各段位的颜色可以在设置里调整。

## 游戏内文本

| 位置 | 文本 |
|---|---|
| 命令 | `/ylctier` |
| 命令 | `/ylctier config` |
| 命令 | `/ylctier retrieve` |
| 命令 | `/ylctier reload` |
| 命令 | `/ylctier <玩家名>` |
| 选项界面 | YLCTierTagger 配置 |
| 设置项 | 总开关 |
| 设置项 | Tab 列表显示 |
| 设置项 | 准星目标段位 |
| 设置项 | 显示位置 |
| 设置项 | 优先模式 |
| 设置项 | 显示规则 |
| 设置项 | 各段位颜色 |
| 同步成功 | 已同步 N 名玩家的段位 |
| 同步失败 | `[PVPTIER] 排行榜同步失败：<原因>` |
| 手动重试提示 | 可输入 /ylctier refresh 手动重试。 |

## 设置

有三种方式打开设置界面：

- 游戏内输入 `/ylctier`
- 选项界面左下角的「PVPTIER 榜单设置」
- 安装了 [ModMenu](https://modrinth.com/mod/modmenu) 的话，模组列表里的「配置」按钮

安装了 [Cloth Config](https://modrinth.com/mod/cloth-config) 时，还可以使用标准的 `/config` 命令。

## 依赖

模组本身不需要任何前置模组，只依赖 Fabric Loader。
ModMenu 与 Cloth Config 属于可选依赖，装上之后会多出对应的入口，不装也能正常使用。

## 支持的客户端版本

| 客户端 | 需要的 Java | 下载哪个文件 |
|---|---|---|
| 1.20.5 ~ 1.21.11 | Java 21 | `YLCTierTagger-<版本>-fabric-mc1.21.11.jar` |
| 1.20.1 ~ 1.20.4 | Java 17 | `YLCTierTagger-<版本>-fabric-mc1.20.4.jar` |

1.20.5 之前的 Minecraft 运行在 Java 17 上，文件格式与新版不通用，因此单独提供一份。

## 安装

把对应版本的 `.jar` 放进客户端的 `mods` 目录即可。

## 数据

段位数据取自 PVPTIER 榜单接口，即我们自己的站点 pvptier.ylcmc.top（YueLunarCourt 服务器），
与第三方站点 pvptier.com 没有任何关系，请勿混淆。

- 每次启动游戏会自动同步一次
- 同步结果缓存在游戏的 config 目录中，站点临时不可用时会继续沿用上一次的数据
- 同步失败时会在游戏内说明原因，并提示可以用 `/ylctier refresh` 手动重试
- 同步在后台进行，不会影响游戏启动

## 构建

需要 JDK 21。

```bash
./gradlew build
```

产物在 `build/libs/`，命名为 `YLCTierTagger-<版本号>-fabric-mc<目标版本>.jar`，
文件名里的 MC 版本由构建时自动填上。
版本号采用 `大版本.小版本.修订号`，第三位会在每次构建时自动加一。

需要同时产出 1.20.1 ~ 1.20.4 的包时：

```bash
bash tools/build-all.sh
```

两个版本的包会分别放在 `dist/` 下的两个目录里，版本号一致。

## 相关链接

- Modrinth：https://modrinth.com/mod/ylc-tier-tagger
- 源码：https://github.com/tianlingmc/ylc-tier-tagger
- 问题反馈：https://github.com/tianlingmc/ylc-tier-tagger/issues

## 发布

- 项目描述的正文：`modrinth-description.md`（中英双语），复制其内容到 Modrinth 的描述框即可。
  `modrinth.json` 里的是同一份文本，交给 `tools/sync-description.py` 同步。
- 版本更新日志：`modrinth-changelog.md`，复制其内容到 Modrinth 该版本的更新日志框。

## 许可

MIT。模式图标来自 [CnTierTagger](https://github.com/tmdakm/CnTierTagger)（Apache-2.0），详见 `NOTICE`。
