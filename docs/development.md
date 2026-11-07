# Development

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

The ordinary plugin artifact is `build/libs/cooldown-overlays-<version>.jar`. For a bundled development-client JAR, use `./gradle-local shadowJar`. A standalone plugin JAR is not a drop-in installer for the normal client: local testing uses the development launcher, and distribution through the Plugin Hub requires a separate submission and review. See RuneLite's [Plugin Hub development guide](https://github.com/runelite/plugin-hub#creating-new-plugins).

## Manual verification

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
