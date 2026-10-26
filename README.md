# Cooldown Overlays

A RuneLite plugin that draws World of Warcraft-style cooldown sweeps and countdowns directly on inventory items.

- **Imbued heart** and **saturated heart** in your inventory.
- **Surge potions**, with the shared cooldown on all dose variants.
- **Stamina**, **antifire**, **super antifire**, **antipoison**, **anti-venom**, and **divine potions**, with the remaining effect duration on each inventory icon. Extended versions, antidotes, Sanfew serum, and mixes have their own settings where applicable. Scarred scraps share the stamina effect and have a separate display setting.
- **Goading**, **Menaphite remedy**, **moonlight**, and **prayer regeneration potions**.
- **Ordinary combat boost potions**, with an estimated countdown until a combat stat falls below your chosen level.
- **Chambers of Xeric overloads**, **prayer enhances**, and **antipoisons**, including weak, regular, and strong versions, plus **Nightmare Zone** and **blighted overloads**.
- **Tombs of Amascut smelling salts**, **liquid adrenaline**, **silk dressing**, and **blessed crystal scarabs**, including both remaining doses or uses.
- **Timer**, **circular sweep**, or **item fill**, with an optional countdown over either progress style. The circle clears clockwise; the item fill reveals the original icon colours from bottom to top.
- An independent checkbox for every supported item family, plus progress opacity and timer color settings.
- One optional **Show ready indicator** setting, with seven visual styles and a live super combat potion preview.
- Optional protection against accidental re-dosing, with a configurable grace window and deliberate overrides.

Search for **Cooldown Overlays** in RuneLite's plugin settings. All supported item families are enabled by default. Timers are tracked even while their individual checkboxes are off, so switching a checkbox back on preserves the timer.

Set **Display mode** to **Item fill** or **Timer and item fill** for the rising reveal. Only the unrevealed part of the item's silhouette is dimmed: its original colours and texture show below the moving boundary, while transparent gaps and the inventory background stay untouched. **Progress opacity** controls the dimming for both fill and circular sweep styles. An unknown duration shows a static dim silhouette and **?** until a valid timer is available. If the sprite has not loaded, the fill waits for it while any countdown remains visible.

**Potion cooldowns** contains surge potions. **Potion effects** contains a separate checkbox for each of the 29 supported effect-item families. All remaining doses of a family share the same setting, so drinking a dose or rearranging your inventory keeps the overlay on the correct icon. Notes, placeholders, empty vials, and unrelated items do not receive overlays. Shared protection applies to all matching potion icons: for example, drinking extended antifire also shows the current antifire protection on ordinary antifire potions you carry.

**Raid potions** contains 13 settings: three strengths each of Chambers overloads, prayer enhances, and antipoisons, plus smelling salts, liquid adrenaline, silk dressing, and blessed crystal scarabs. Strength variants of the same Chambers effect share its timer, while each variant retains its own display checkbox. Ambrosia restores stats immediately and has no sustained effect timer to display.

**Boost potions** contains separate checkboxes for ordinary attack, strength, defence, their super versions, combat, super combat, ranging, magic, bastion, and battlemage potions. Dose and mix variants share the matching checkbox. Set independent **Attack below**, **Strength below**, **Defence below**, **Ranged below**, and **Magic below** levels. These are actual stat levels: **Strength below 110** becomes ready at **109**, and stays inactive at 110. Each target defaults to **0**, which disables that skill until you choose a level.

The **Super combat** checkbox also covers all four Last Man Standing super combat doses. These use their own Attack-and-Strength timer because they do not boost Defence. LMS ranging doses share the ordinary Ranging setting.

The timer counts down until a relevant stat falls strictly below its configured level. Combined potions use the first enabled, eligible stat to reach its own target. A dose must improve that stat before its icon can show a ready indicator, including when a target is higher than the potion's full boost. Enable **Show ready indicator** to show the selected signal, and carry a matching enabled potion in your inventory: without a potion, there is no icon or visual reminder. Older relative-boost thresholds are not converted into actual stat targets; set the new levels explicitly.

Under **Ready indicator**, enable **Show ready indicator**, then choose **Ready style**:

- **Animated border** retains the moving, pulsing border inspired by TellMeWhen's [activation border](https://github.com/ascott18/TellMeWhen/blob/master/Components/IconModules/IconModule_IconContainer/IconContainer.lua). This remains the default style.
- **Steady outline** draws a fixed coloured contour.
- **Soft glow** adds a stationary, faint halo.
- **Corner marker** places a small coloured dot in the bottom-right corner.
- **Breathing glow** gently fades in and out without moving around the icon.
- **Single flash** flashes once for 0.8 seconds, then disappears.
- **Flash, then steady** flashes once, then settles into a fixed outline.

**Indicator length** offers **While ready** or **For a duration**. Duration mode shows the signal for the selected number of seconds, fading out at the end; the default is three seconds. **Single flash** always plays once, including in **While ready** mode. Changing the ready style restarts the signal on currently ready items.

**Border style** defaults to **Item outline**, which follows the visible sprite. Choose **Inventory slot** for a border around the rectangular tile. **Border width** controls the main stroke from **1–6 pixels**, with a default of **1 pixel**. Border and glow styles use these settings; the corner marker has its own fixed position. An unavailable item silhouette leaves a border hidden until the sprite is available.

**Outline opacity** controls the ready outline, glow, or corner marker from **0–100%**, defaulting to **100%**. Lower it for a more transparent signal; **0%** hides the ready signal. This is separate from **Progress opacity**, which controls the circular sweep and item dimming during a cooldown. The selected animation and timed fade still apply.

Ready indicators are off by default. They appear only when a cooldown is ready, a fixed potion effect has expired, or an ordinary combat stat falls below its configured level and a dose would improve it. The colour, duration, style, and width remain configurable. Signals respect interface clipping and do not alter click targets. Already-ready items can show a signal after the first valid server sample. Hidden inventory tabs do not pause or replay an old duration. Existing enablement, colour, duration, and border preferences are preserved.

### Re-dose protection

Under **Re-dose protection**, enable **Prevent accidental re-dosing** to replace an inventory item's left-click use with an inert **Wait** entry while its known timer is active. This is off by default and respects each item family's checkbox. **Allow re-dose within** permits normal use during the final **0–60 seconds**, defaulting to **15 seconds**. Right-click retains the original use option, and holding **Shift** bypasses protection while preserving your normal Shift-click action.

Ordinary combat potions reuse the configured stat targets: **Strength below 110** protects against re-dosing at 110 or above and allows a dose at 109 if it would improve Strength. Protection leaves clicks available when a timer is unknown, stale, or unavailable. It does not block food, Sanfew serum, moonlight potions, silk dressing, blessed crystal scarabs, Scarred scraps, or barbarian mixes. Stamina and extended stamina remain usable below 100% run energy, and divine potions remain usable when a relevant stat has drained below their full boost.

Protection follows confirmed game state; it does not debounce rapid clicks before the server confirms a dose. Server timers remain authoritative, and this feature does not automate drinking.

### Settings preview

Enable **Show preview** near the top of the plugin settings to open a compact preview window alongside them. It shows the actual **Super combat potion(4)** icon at native size and enlarged, using the same renderer and current display settings as inventory items. Its independent countdown reaches ready after **10 seconds**. **Restart preview** starts the demonstration again.

Keep the preview open while changing display mode, opacity, ready style, colour, width, or duration. The preview respects **Show ready indicator**; turn that on to see ready signals. Changing ready-indicator settings after the countdown finishes replays the ready phase without another ten-second wait. Closing the preview or stopping the plugin stops its animation and turns **Show preview** off. The demonstration does not change game timers or require a potion in your inventory.

## Local setup

This workspace includes the official RuneLite source under `.upstream/runelite`, the official plugin template under `.upstream/example-plugin`, a workspace-local Temurin 17 JDK under `.tools/jdk`, and a local Gradle cache. These downloaded files are ignored by Git. System Java and your installed RuneLite application are unchanged.

The plugin follows the [official plugin template](https://github.com/runelite/example-plugin) and compiles to **Java 11** bytecode. Developing this standalone plugin does not require building the complete RuneLite source tree.

Build and run the finite, headless checks:

```sh
./gradle-local build
```

Start the development client yourself when ready:

```sh
./gradle-local run
```

The launcher loads this plugin into RuneLite in developer mode. For Jagex accounts, follow RuneLite's [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts) guide to log in to the development client. Keep launcher credentials out of this repository.

To open the project in an IDE, import `build.gradle` and select `.tools/jdk/Contents/Home` as the Gradle JDK. On another machine, install JDK 17 and use the standard Gradle wrapper (`./gradlew build` or `./gradlew run`); `gradle-local` also honors an explicitly set `JAVA_HOME` and `GRADLE_USER_HOME`.

On macOS, the `run` task supplies the Java module exports required by RuneLite's native fullscreen adapter. If you run `CooldownOverlaysLauncher` directly from an IDE instead of using the Gradle task, add these VM options:

```text
--add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED
--add-exports=java.desktop/com.apple.eawt.event=ALL-UNNAMED
```

The bundled development-client JAR carries the equivalent `Add-Exports` manifest attribute for `java -jar` launches. Missing these exports on newer JDKs causes an `IllegalAccessError` involving `OSXFullScreenAdapter` during startup.

The ordinary plugin artifact is `build/libs/cooldown-overlays-1.0.6.jar`. For a bundled development-client JAR, use `./gradle-local shadowJar`. A standalone plugin JAR is not a drop-in installer for the normal client: local testing uses the development launcher, and distribution through the Plugin Hub requires a separate submission and review. See RuneLite's [Plugin Hub development guide](https://github.com/runelite/plugin-hub#creating-new-plugins).

## Timing behavior

Tracking samples the game's cooldown varbits after each server update, using the same game values as RuneLite's [Timers and Buffs plugin](https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/timersandbuffs). Clicking an item does not itself start a timer.

- Hearts share `VarbitID.IMBUED_HEART_TIMER`, which exposes remaining time in ten-tick buckets. Observed uses select the seven-minute imbued-heart or five-minute saturated-heart sweep duration. Enabling the plugin or logging in mid-cooldown restores the remaining timer, with up to six seconds of bucket uncertainty; the sweep assumes the seven-minute duration until the next observed use because the varbit does not identify which heart started it.
- Surge uses `SURGE_POTION_TIMER` in ten-tick buckets. Stamina uses `STAMINA_DURATION` in ten-tick buckets, gated by `STAMINA_ACTIVE`. Both ordinary and extended stamina are supported.
- Antifire and super antifire use their respective 30-tick and 20-tick server counters. Refreshing a dose preserves the current bucket phase and accommodates extended or accumulated durations.
- Antipoison, super antipoison, antidote, Sanfew serum, and Chambers antipoison icons show **poison immunity**, including protection that remains after venom immunity expires. Anti-venom icons continue to show **venom immunity**, so their ready border can prompt another dose as soon as venom protection ends.
- All eight divine potion families use their separate, tick-resolution server counters. Combined divine effects can also appear on the corresponding individual-stat potion icons.
- Chambers overloads use `RAIDS_OVERLOAD_TIMER` in 25-tick buckets, with a standard five-minute duration. Refreshes preserve the current bucket phase. Prayer enhance uses `RAIDS_PRAYERENHANCE_TIMER` multiplied by the server's `RAIDS_PRAYERENHANCE_RATE`, as the game's [buff bar scripts](https://github.com/runelite/cs2-scripts/blob/master/scripts/%5Bproc,buff_bar_get_value%5D.cs2) do. These timers clear outside Chambers even if an old counter lingers.
- Nightmare Zone and blighted overloads use their own independent 25-tick counters. Menaphite remedy uses 25-tick buckets, goading uses six-tick buckets, and prayer regeneration uses 12-tick buckets. Moonlight uses its tick-resolution counter, including RuneLite's one-tick synchronization adjustment for the associated divine Defence timer.
- Smelling salts use `TOA_MIDRAIDLOOT_STATS_TIMER` in 25-tick buckets, with a standard eight-minute duration.
- Liquid adrenaline uses the `TOA_MIDRAIDLOOT_ENERGY_ACTIVE` flag and a 150-second duration for an observed activation. A successful drink message refreshes the timer even when the flag stays active. Because the flag provides no remaining duration, logging in or enabling the plugin midway through an active dose shows a static dim progress shape and **?** until it ends or a new dose is confirmed. If the predicted duration elapses while the server flag remains active, the timer returns to **?** rather than implying the effect has ended. The server clearing the flag always ends the timer.
- Silk dressing and blessed crystal scarab timers start only after their confirmed game messages, lasting 100 and 40 game ticks respectively. Re-use refreshes the timer, and death clears it. These effects do not expose a remaining-time counter, so enabling the plugin, reconnecting, or hopping worlds midway through one leaves its overlay hidden until another confirmed use.
- Ordinary boosts use current/base combat levels and cadence-confirmed one-level decreases to learn the shared decay phase, following RuneLite's [Boosts Information timing](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/boosts/BoostsPlugin.java). A level normally decays every 100 game ticks, extended to up to 150 ticks with Preserve. Drinking does not restart this already-running cycle, so the first decrease can happen shortly after a dose. Re-drinking retains the shared phase. Preserve changes, stat drains, base-level changes, and re-doses recalculate the estimate. The actual stat falling strictly below its configured level controls readiness; a prediction alone never starts the border.
- The first eligible decrease starts a tentative phase; another decrease on its expected Preserve-aware deadline confirms it. Multiple skills falling in the same game tick count as one observation. Until confirmation, or after a missed expected decay, ordinary boosts display **?** rather than inventing a countdown. An off-cadence decrease replaces a tentative phase but cannot move a confirmed one. Readiness still updates immediately from actual stat levels, and re-dose protection leaves uncertain timers available. Predictions assume Preserve stays in its current state for future cycles. External drains recurring exactly on the same cadence remain indistinguishable from natural decay without a server timing signal. Hitpoints and Rapid Heal are not used to learn this combat-stat cycle.
- Relevant divine, overload, and smelling-salts effects suppress ordinary potion prompts while they maintain the same stats. Expiry behavior differs between these effects, so the plugin does not append a guessed natural-decay duration to a maintained boost.
- Rendering smooths between ticks for up to one tick. An unchanged counter can represent a paused cooldown, so the estimate never counts beyond the current server bucket. During a stalled connection, it waits for server confirmation instead of suggesting that a cooldown is ready. Overlays disappear while logged out or while their widgets are hidden.

Bucket-based effect timers have limited precision immediately after login or enabling the plugin: up to six seconds for surge/stamina, 18 seconds for antifire and poison/venom immunity, 12 seconds for super antifire, 15 seconds for overloads, Menaphite remedy, and smelling salts, 3.6 seconds for goading, or 7.2 seconds for prayer regeneration. Prayer enhance precision follows its current server rate. The next server changes refine the estimate. For extended, stacked, or already-running effects, the sweep scales to the reported duration or the standard duration, whichever is greater; its denominator can restart on plugin reload. Divine and moonlight timers are reported in individual game ticks.

## In-game verification

Automated tests exercise the timer state and renderer without launching RuneLite. Actual game behavior still requires manual verification:

1. Enable **Cooldown Overlays**, use each heart, and verify its inventory overlay clears when the game permits another use. Move the heart between slots while it cools down.
2. Try all five display modes, change opacity and text color, and disable individual item families during a timer. Re-enabling one should reveal the current remaining time. In item-fill modes, check the rising full-colour reveal on different item shapes, including while dragging, and confirm transparent gaps and the inventory background stay clear. The numeric countdown should appear only in the combined mode, or as **?** for an unknown duration.
3. Switch inventory tabs, move items, open a menu over an icon, and try fixed, resizable, and stretched layouts. Overlays should follow their icons and stay beneath menus.
4. Disable and re-enable the plugin, log out and back in, and hop worlds during cooldowns. Hearts and potion counters should restore their remaining estimate; an already-active liquid adrenaline effect should display **?** until it ends or a new dose is confirmed.
5. In Chambers, drink each overload and prayer enhance strength. Verify the timers follow the remaining doses, refresh correctly, and disappear on raid exit.
6. Drink a surge dose, verify the timer follows the remaining doses, and check its pause/resume behavior in content that pauses the cooldown. A reset at a pool should clear it.
7. Drink stamina, antifire, super antifire, anti-venom, and divine potions. Verify their effect timers appear on the appropriate remaining doses. Refresh antifire midway through a bucket, try an extended variant, and check that venom protection clears before the longer poison-immunity tail.
8. Under **Ready indicator**, test all seven styles with **While ready** and **For a duration**. Check that steady outline, soft glow, and corner marker stay still, single flash stops after its first flash, and flash-then-steady settles into an outline. Compare **Item outline** on hearts and potion bottles against **Inventory slot**, and check border widths from 1 to 6 pixels. Vary **Outline opacity** from 0 to 100%, and confirm it affects only the ready signal. The item outline should follow the visible sprite rather than its rectangular tile. A cooling-down item must not animate. Check initially ready items, change the color, opacity, and duration, use the item, switch tabs, and log out to verify cancellation and clipping.
9. In ToA, use smelling salts and liquid adrenaline. Verify their timers follow the remaining uses. Drink a second adrenaline dose while the first remains active and confirm the timer refreshes. An unsuccessful use must not refresh it.
10. Carry a strength or super strength potion, enable **Show ready indicator**, and set **Strength below** to **110**. Verify the border stays off at 110 and appears at 109 when a dose would improve Strength. Drink one, wait for a natural decrease, and verify the timer predicts the drop below 110. Set separate targets for other skills, and set one to 0 to disable it. Re-drink midway through the decay cycle, toggle Preserve, and test a combined potion whose skills reach their targets at different times. Remove the matching potions from your inventory and verify no icon or reminder remains.
11. Drink each new effect potion and antipoison family, and verify the overlay follows its remaining doses. After anti-venom wears off, compare its ready border with the longer poison timer on antipoison items. Check Nightmare Zone and blighted overloads independently of Chambers, and check Scarred scraps while stamina is active.
12. In ToA, apply silk dressing and crack a blessed crystal scarab. Confirm 60-second and 24-second timers, refresh each effect, and check death, logout, and world-hop cancellation. Failed uses and player chat must not start a timer.

13. Open **Show preview**, confirm the super combat potion reaches ready after ten seconds, and replay it with **Restart preview**. Change visual settings during the countdown and after it finishes, then close the window and disable the plugin. Reopen it to verify a fresh countdown and no leftover animation.
14. Enable **Prevent accidental re-dosing**, use a supported potion, and verify its left-click becomes **Wait** outside the grace window. Confirm right-click use and your normal Shift-click action still work, then check expiry, the grace window, disabled item families, and unknown timers. Check an ordinary potion at and below its stat target, stamina below 100% run energy, a divine potion after a stat drain, and the excluded healing or restoration items. Disable protection and verify normal left-click use returns.
15. In LMS, check all four super combat doses with Attack and Strength targets, then with only a Defence target. Defence alone must not create an LMS super combat overlay or block its use. Confirm an initial **?**, wait for enough decay observations to establish the countdown, and re-dose midway through a cycle: the next natural decrease should stay on that cycle. Check that actual threshold crossings still produce readiness while timing is unknown, and compare with Preserve enabled and disabled outside LMS.

No game input or item use is automated by this plugin.

## License

[BSD 2-Clause](LICENSE).
