# v1.2.0 (2026/10/31)

## Added

- Tier HUD panel: an always-on panel for the nearest player within 30 blocks, with a "current distance: N m" line when there is one, falling back to your own stats otherwise. It shows the name and overall rank, region, rank badge with points (and title), an OVERALL line, then every mode tier - one per line, with the mode icon, sorted by tier strength
- Seven rank badges (Combat Grandmaster / Master / Ace / Specialist / Cadet / Novice / Rookie) picked by the point thresholds from the site's `rank_titles` (400 / 250 / 100 / 50 / 20 / 10 / 0). The icons are taken straight from the leaderboard site (`card-icons/badge-*.png`, plus `card-icons/points.png` for the points line) and registered as bitmap font glyphs E709 ~ E710
- The HUD position is draggable: "Configure HUD position" in the settings opens a drag screen - hold the left button on the panel to move it (press on empty space to snap it there), the percentage coordinates are shown live, and it takes effect after Save. The position is stored as a fraction of the screen size, so it survives resolution and GUI scale changes. A "Tier HUD" switch turns the whole thing off

## Fixed

- Removed the tier from chat. Decorating a chat line requires rebuilding the whole message tree, which easily breaks vanilla colours and click/hover events, and the decoration template is decided by the server (this server sends an already composed message and never references the `sender` parameter), so it cannot work reliably. The tab list and the nametag are unaffected, and the "chat" toggle has been dropped from the settings screen
- The options-screen button could be blown up into one huge panel covering every vanilla button: `ClickableWidget.setDimensionsAndPosition` takes `(width, height, x, y)` - dimensions first - so passing `(x, y, w, h)` set the width and height to the coordinate values. It now calls `setDimensions(w, h)`, `setX` and `setY` separately
- The options-screen button now follows the width of the vanilla buttons (vanilla builds them via `ButtonWidget.Builder.build()` with no explicit size) and no longer has a hard minimum, so it cannot grow wider than its cell; layout containers such as `ThreePartsLayoutWidget` / `GridWidget` are filtered out by size; the injection is wrapped in try/catch
- Startup crash: the tier HUD was hooked at the wrong point. Mixin requires the handler to declare all of the target method's parameters before the `CallbackInfo`, and the second parameter of `InGameHud.render` is `float` on 1.20.4 but `RenderTickCounter` on 1.21.11, so taking only the first one throws `InvalidInjectionException` during apply. It is now hooked on `renderStatusBars(DrawContext)`, whose signature is identical in both versions, and the injection uses `require = 0` so a future signature change only hides the panel instead of crashing
- The HUD position screen could not be dragged and its preview did not match where the HUD actually appears. It read `client.mouse.getX()`, which is in window pixels rather than GUI-scaled coordinates, so the hit test never matched and the panel snapped to a corner. It now uses the `mouseX/mouseY` passed into `render()`, and the panel size and positioning were moved into `TierHudPanel.boxWidth / boxHeight / position` so the HUD and the drag screen share one calculation

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
