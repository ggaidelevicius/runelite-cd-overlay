# Cooldown Overlays

A RuneLite plugin that draws World of Warcraft-style cooldown sweeps and countdowns directly on inventory items.

- **Imbued heart** and **saturated heart** in your inventory.
- **Surge potions**, with the shared cooldown on all dose variants.
- **Stamina**, **antifire**, **super antifire**, **anti-venom**, and **divine potions**, with the remaining effect duration on each inventory icon. Extended versions and mixes have their own settings where applicable.
- **Ordinary combat boost potions**, with an estimated countdown until a combat stat falls below your chosen level.
- **Chambers of Xeric overloads** and **prayer enhances**, including weak, regular, and strong versions.
- **Tombs of Amascut smelling salts** and **liquid adrenaline**, including both remaining doses or uses.
- **Timer**, **circular sweep**, or **both**. The shaded circle clears clockwise as the cooldown expires.
- An independent checkbox for every supported item family, plus circle opacity and timer color settings.
- One optional **Animate when ready** setting, inspired by TellMeWhen's [activation border](https://github.com/ascott18/TellMeWhen/blob/master/Components/IconModules/IconModule_IconContainer/IconContainer.lua).

Search for **Cooldown Overlays** in RuneLite's plugin settings. All supported item families are enabled by default. Timers are tracked even while their individual checkboxes are off, so switching a checkbox back on preserves the timer.

**Potion cooldowns** contains surge potions. **Potion effects** contains a separate checkbox for each of the 17 supported effect-potion families. All remaining doses of a family share the same setting, so drinking a dose or rearranging your inventory keeps the overlay on the correct icon. Notes, placeholders, empty vials, and unrelated items do not receive overlays. Shared protection applies to all matching potion icons: for example, drinking extended antifire also shows the current antifire protection on ordinary antifire potions you carry.

**Raid potions** contains eight more settings: **Overload (-)**, **Overload**, **Overload (+)**, **Prayer enhance (-)**, **Prayer enhance**, **Prayer enhance (+)**, **Smelling salts**, and **Liquid adrenaline**. Strength variants of the same Chambers effect share its timer, while each variant retains its own display checkbox. Ambrosia restores stats immediately and has no sustained effect timer to display.

**Boost potions** contains separate checkboxes for ordinary attack, strength, defence, their super versions, combat, super combat, ranging, magic, bastion, and battlemage potions. Dose and mix variants share the matching checkbox. Set independent **Attack below**, **Strength below**, **Defence below**, **Ranged below**, and **Magic below** levels. These are actual stat levels: **Strength below 110** becomes ready at **109**, and stays inactive at 110. Each target defaults to **0**, which disables that skill until you choose a level.

The timer counts down until a relevant stat falls strictly below its configured level. Combined potions use the first enabled, eligible stat to reach its own target. A dose must improve that stat before its icon can animate, including when a target is higher than the potion's full boost. Enable **Animate when ready** to show the border, and carry a matching enabled potion in your inventory: without a potion, there is no icon or visual reminder. Older relative-boost thresholds are not converted into actual stat targets; set the new levels explicitly.

Under **Animations**, enable **Animate when ready**, then choose **Animation length**:

- **While ready** keeps the moving activation border visible until the item is used or stops being ready.
- **For a duration** plays it for the selected number of seconds after the item becomes ready. The default is three seconds.

**Border style** defaults to **Item outline**, which traces the item's visible silhouette using the sprite's alpha channel. Choose **Inventory slot** for a border around the rectangular tile. **Border width** controls the main visible stroke from **1–6 pixels**, with a default of **1 pixel**. In item-outline mode, an unavailable sprite leaves the border hidden until the sprite is available; it does not substitute a rectangular border.

Animations are off by default. They appear only when a cooldown is ready, a fixed potion effect has expired, or an ordinary combat stat falls below its configured level and a dose would improve it. Running timers never animate. The color, duration, style, and width remain configurable. The outline and its soft glow extend around the item, respect interface clipping, and do not alter click targets. Already-ready items can animate after the first valid server sample, and enabling animations or changing their length, style, or width restarts the border on currently ready items. Hidden tabs do not pause or replay an old duration. Existing animation enablement, color, and duration preferences are carried over from earlier settings.

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

The ordinary plugin artifact is `build/libs/cooldown-overlays-1.0.1.jar`. For a bundled development-client JAR, use `./gradle-local shadowJar`. A standalone plugin JAR is not a drop-in installer for the normal client: local testing uses the development launcher, and distribution through the Plugin Hub requires a separate submission and review. See RuneLite's [Plugin Hub development guide](https://github.com/runelite/plugin-hub#creating-new-plugins).

## Timing behavior

Tracking samples the game's cooldown varbits after each server update, using the same game values as RuneLite's [Timers and Buffs plugin](https://github.com/runelite/runelite/tree/master/runelite-client/src/main/java/net/runelite/client/plugins/timersandbuffs). Clicking an item does not itself start a timer.

- Hearts share `VarbitID.IMBUED_HEART_TIMER`, which exposes remaining time in ten-tick buckets. Observed uses select the seven-minute imbued-heart or five-minute saturated-heart sweep duration. Enabling the plugin or logging in mid-cooldown restores the remaining timer, with up to six seconds of bucket uncertainty; the sweep assumes the seven-minute duration until the next observed use because the varbit does not identify which heart started it.
- Surge uses `SURGE_POTION_TIMER` in ten-tick buckets. Stamina uses `STAMINA_DURATION` in ten-tick buckets, gated by `STAMINA_ACTIVE`. Both ordinary and extended stamina are supported.
- Antifire and super antifire use their respective 30-tick and 20-tick server counters. Refreshing a dose preserves the current bucket phase and accommodates extended or accumulated durations.
- Anti-venom timers show **venom immunity**, including the short protection from ordinary anti-venom. The remaining poison immunity after venom protection ends is not shown as venom protection.
- All eight divine potion families use their separate, tick-resolution server counters. Combined divine effects can also appear on the corresponding individual-stat potion icons.
- Chambers overloads use `RAIDS_OVERLOAD_TIMER` in 25-tick buckets, with a standard five-minute duration. Refreshes preserve the current bucket phase. Prayer enhance uses `RAIDS_PRAYERENHANCE_TIMER` multiplied by the server's `RAIDS_PRAYERENHANCE_RATE`, as the game's [buff bar scripts](https://github.com/runelite/cs2-scripts/blob/master/scripts/%5Bproc,buff_bar_get_value%5D.cs2) do. These timers clear outside Chambers even if an old counter lingers.
- Smelling salts use `TOA_MIDRAIDLOOT_STATS_TIMER` in 25-tick buckets, with a standard eight-minute duration.
- Liquid adrenaline uses the `TOA_MIDRAIDLOOT_ENERGY_ACTIVE` flag and a 150-second duration for an observed activation. A successful drink message refreshes the timer even when the flag stays active. Because the flag provides no remaining duration, logging in or enabling the plugin midway through an active dose shows a dim circle and **?** until it ends or a new dose is confirmed. If the predicted duration elapses while the server flag remains active, the timer returns to **?** rather than implying the effect has ended. The server clearing the flag always ends the timer.
- Ordinary boosts use current/base combat levels and an observed natural one-level decay to learn the shared decay phase, following RuneLite's [Boosts Information timing](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/boosts/BoostsPlugin.java). A level normally decays every 100 game ticks, extended to up to 150 ticks with Preserve. Re-drinking retains the shared phase. Preserve changes, stat drains, base-level changes, and re-doses recalculate the estimate. The actual stat falling strictly below its configured level controls readiness; a prediction alone never starts the border.
- Until the first usable decay observation, or after a missed expected decay, ordinary boosts display **?** rather than inventing the remaining phase. Predictions assume Preserve stays in its current state for future cycles. A one-level external stat drain can resemble natural decay; observations away from an already-known decay window are ignored, but the initial estimate cannot always distinguish them.
- Relevant divine, overload, and smelling-salts effects suppress ordinary potion prompts while they maintain the same stats. Expiry behavior differs between these effects, so the plugin does not append a guessed natural-decay duration to a maintained boost.
- Rendering smooths between ticks for up to one tick. An unchanged counter can represent a paused cooldown, so the estimate never counts beyond the current server bucket. During a stalled connection, it waits for server confirmation instead of suggesting that a cooldown is ready. Overlays disappear while logged out or while their widgets are hidden.

Bucket-based effect timers have limited precision immediately after login or enabling the plugin: up to six seconds for surge/stamina, 18 seconds for antifire/anti-venom, 12 seconds for super antifire, or 15 seconds for Chambers overloads and smelling salts. Prayer enhance precision follows its current server rate. The next server changes refine the estimate. For extended, stacked, or already-running effects, the sweep scales to the reported duration or the standard duration, whichever is greater; its denominator can restart on plugin reload. Divine timers are reported in individual game ticks.

## In-game verification

Automated tests exercise the timer state and renderer without launching RuneLite. Actual game behavior still requires manual verification:

1. Enable **Cooldown Overlays**, use each heart, and verify its inventory overlay clears when the game permits another use. Move the heart between slots while it cools down.
2. Try all three display modes, change opacity and text color, and disable individual item families during a timer. Re-enabling one should reveal the current remaining time.
3. Switch inventory tabs, move items, open a menu over an icon, and try fixed, resizable, and stretched layouts. Overlays should follow their icons and stay beneath menus.
4. Disable and re-enable the plugin, log out and back in, and hop worlds during cooldowns. Hearts and potion counters should restore their remaining estimate; an already-active liquid adrenaline effect should display **?** until it ends or a new dose is confirmed.
5. In Chambers, drink each overload and prayer enhance strength. Verify the timers follow the remaining doses, refresh correctly, and disappear on raid exit.
6. Drink a surge dose, verify the timer follows the remaining doses, and check its pause/resume behavior in content that pauses the cooldown. A reset at a pool should clear it.
7. Drink stamina, antifire, super antifire, anti-venom, and divine potions. Verify their effect timers appear on the appropriate remaining doses. Refresh antifire midway through a bucket, try an extended variant, and check that venom protection clears before the longer poison-immunity tail.
8. Under **Animations**, test **Animate when ready** with **While ready** and **For a duration**. Compare **Item outline** on hearts and potion bottles against **Inventory slot**, and check border widths from 1 to 6 pixels. The item outline should follow the visible sprite rather than its rectangular tile. A cooling-down item must not animate. Check initially ready items, change the color and duration, use the item, switch tabs, and log out to verify cancellation and clipping.
9. In ToA, use smelling salts and liquid adrenaline. Verify their timers follow the remaining uses. Drink a second adrenaline dose while the first remains active and confirm the timer refreshes. An unsuccessful use must not refresh it.
10. Carry a strength or super strength potion, enable **Animate when ready**, and set **Strength below** to **110**. Verify the border stays off at 110 and appears at 109 when a dose would improve Strength. Drink one, wait for a natural decrease, and verify the timer predicts the drop below 110. Set separate targets for other skills, and set one to 0 to disable it. Re-drink midway through the decay cycle, toggle Preserve, and test a combined potion whose skills reach their targets at different times. Remove the matching potions from your inventory and verify no icon or reminder remains.

No game input or item use is automated by this plugin.

## License

[BSD 2-Clause](LICENSE).
