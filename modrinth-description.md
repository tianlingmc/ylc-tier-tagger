# YLCTierTagger

> The exclusive PVP leaderboard Tagger mod for the YueLunarCourt server.

Every player on the YueLunarCourt PVP leaderboard has a tier. This mod shows it on the client:
the matching mode icon and tier appear next to the player's name.

## Where it appears

| Location | Description |
|---|---|
| Nametag | Shown next to the name when you are close to the player |
| Tab list | Shown in the player list opened with Tab |
| HUD panel | An always-on panel showing the nearest player within 30 blocks (with the distance), or your own stats when nobody is that close: name and overall rank, region, rank badge with points, and every mode tier on its own line with its icon |

The tier can be placed before or after the name in both the nametag and the tab list.

## Modes and tiers

| Mode | Leaderboard key |
|---|---|
| Axe | `axe` |
| Mace | `mace` |
| Netherite | `nethop` |
| Pot | `pot` |
| SMP | `smp` |
| Sword | `sword` |
| UHC | `uhc` |
| Crystal | `vanilla` |

Tiers come in two groups, `HT1` to `HT5` and `LT1` to `LT5`.
Within a mode, `HT1` is the highest and `LT5` the lowest.
The colour of each tier can be adjusted in the settings.

## Commands

| Command | Description |
|---|---|
| `/ylctier` | Shows the usage list |
| `/ylctier config` | Opens the settings screen |
| `/ylctier retrieve` | Fetches the leaderboard again |
| `/ylctier reload` | Reloads the mod config from disk |
| `/ylctier <player>` | Shows the tiers of that player |

## Settings

| Option | Description |
|---|---|
| Master switch | Turns every tier display on or off |
| Tab list | Whether the tier is shown in the tab list |
| Tier HUD | Whether the HUD panel is shown |
| HUD position | Drag the HUD into place on its own screen |
| Position | Whether the tier goes before or after the name |
| Preferred mode | The mode that is preferred |
| Display rule | Single mode, highest tier only, or prefer the chosen mode |
| Tier colours | The colour of each tier |

Changes are kept in memory and only written to disk when you press Save; Cancel and
Reset to defaults are available too.

## Opening the settings

- Type `/ylctier config` in game
- Click the YLCTierTagger config button on the options screen (it sits to the right of
  "Credits & Attribution" and below "Accessibility Settings")
- Click the config button in the mod list, if ModMenu is installed
- Use the standard `/config` command, if Cloth Config is installed

## Dependencies

The mod needs nothing but Fabric Loader.
ModMenu and Cloth Config are optional: with them installed a few more entry points appear,
but everything works without them.

## Supported client versions

| Client | Java | File to download |
|---|---|---|
| 1.20.5 ~ 1.21.11 | Java 21 | `YLCTierTagger-<version>-fabric-mc1.21.11.jar` |
| 1.20.1 ~ 1.20.4 | Java 17 | `YLCTierTagger-<version>-fabric-mc1.20.4.jar` |

Minecraft versions before 1.20.5 run on Java 17 and use a different file format,
so they are shipped as a separate file.

## Data

Tier data comes from the PVPTIER leaderboard API (pvptier.ylcmc.top).

- Synced once automatically whenever the game starts
- The result is cached in the config folder, and the last known data keeps being used while
  the site is unavailable
- If the sync fails, the reason is shown in game and you can retry with `/ylctier retrieve`
- The sync runs in the background and does not affect starting the game
