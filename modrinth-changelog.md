# v1.2.0 (2026/10/27)

## Added

- No new features; this release only fixes what was left over from 1.1.0

## Fixed

- Removed the tier from chat. Decorating a chat line requires rebuilding the whole message tree, which easily breaks vanilla colours and click/hover events, and the decoration template is decided by the server (this server sends an already composed message and never references the `sender` parameter), so it cannot work reliably. The tab list and the nametag are unaffected, and the "chat" toggle has been dropped from the settings screen
- The options-screen button could be blown up into one huge panel covering every vanilla button: `ClickableWidget.setDimensionsAndPosition` takes `(width, height, x, y)` - dimensions first - so passing `(x, y, w, h)` set the width and height to the coordinate values. It now calls `setDimensions(w, h)`, `setX` and `setY` separately
- The options-screen button now follows the width of the vanilla buttons (vanilla builds them via `ButtonWidget.Builder.build()` with no explicit size, so they use the default width and height) and no longer has a hard minimum, so it cannot grow wider than its cell; layout containers such as `ThreePartsLayoutWidget` / `GridWidget` are filtered out by size so they are never mistaken for a reference button; the injection is wrapped in try/catch so a failure cannot break the vanilla screen

# v1.1.0 (2026/10/25)

## Added

- `/ylctier config` opens the settings screen, `/ylctier retrieve` fetches the leaderboard again, `/ylctier reload` reloads the mod config, and `/ylctier` without arguments lists every subcommand
- Tier colours are picked on a dedicated colour page: the palette is laid out as a grid, each cell shows the swatch plus its hex value, the current value is outlined, and one click selects it and returns (replacing the old click-to-cycle behaviour)
- A single `" | "` separator between the tier and the player name, with the icon always in front of the tier: `[icon] tier | name` on the left, `name | [icon] tier` on the right
- The settings entry is now called "YLCTierTagger 配置" everywhere (options screen, ESC pause menu, ModMenu), and the options-screen button sits to the right of "Credits & Attribution" and below "Accessibility Settings"
- ESC on the settings screen, the colour page and the mode page returns to the previous screen instead of closing the screen entirely
- `/ylctier retrieve` posts its result to chat (success or failure) instead of relying only on a toast

## Fixed

- `/ylctier` reported "unknown or incomplete command": the client dispatcher is replaced every time the server sends a command tree, and the command was only registered when the network handler changed, so it was wiped right after; it is now re-registered whenever the root node is missing
- When the tier was shown on the right, the icon ended up behind the tier in the tab list and on the nametag; the icon is now always in front
- The options-screen button was misplaced and fell back to the bottom-left corner (the key for "Credits & Attribution" is `options.credits_and_attribution`, not `options.credits`); it is now positioned one row below "Accessibility Settings", with a text-based fallback
- Every `/ylctier` subcommand returned "unknown or incomplete command": vanilla `sendChatCommand` only sends the command to the server and never executes client commands, and with no dependencies there is no Fabric API client-command hook; `ClientPlayNetworkHandlerMixin` now intercepts `sendChatCommand`, runs matching commands locally and cancels the packet

# v1.0.0 (2026/10/25)

## Added

- Tier shown on the nametag, in the tab list and in chat, with the chat position configurable
- Every mode has its own icon; tiers are HT1 ~ HT5 and LT1 ~ LT5, and the colour of each tier can be adjusted
- Settings screen reachable from `/ylctier`, the ESC pause menu and ModMenu; it edits an in-memory draft and only writes on "Save", with "Cancel" and "Reset to defaults" available
- Preferred mode, display rule and display position can be switched
- The leaderboard syncs once in the background at every start; on failure the reason, whether cached data is used and a manual retry hint are shown
- Two builds: 1.20.5 ~ 1.21.11 (Java 21) and 1.20.1 ~ 1.20.4 (Java 17); no dependencies, ModMenu and Cloth Config are optional

## Fixed

- Chat messages typed by players got no tier (the name lives in the translatable arguments, not in the siblings)
- A startup crash caused by decorating chat, and a "protocol error" kick on 1.21.x after mutating the immutable `siblings` list
- The 1.20.1 ~ 1.20.4 (Java 17) build crashed at startup because of a wrong mixin `compatibilityLevel`
- No text at all in the settings screen, plus fully transparent tier text and swatches (colours were written without an alpha channel)
- Scrolling in the settings screen now uses a native-style scrollbar with mouse-wheel support
- LT5 and LT4 shared the same default colour
- A bad escape in the `fabric.mod.json` description and a wrong supported-version note
- Parsing failures when the leaderboard API returns non-JSON (three retries plus a local-cache fallback)
