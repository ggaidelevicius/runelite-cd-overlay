# Settings reference

Detailed behaviour of every setting. For a quick overview, see the [README](../README.md).

## Item families

Search for **Cooldown Overlays** in RuneLite's plugin settings. All supported item families are enabled by default. Timers are tracked even while their individual checkboxes are off, so switching a checkbox back on preserves the timer.

Set **Display mode** to **Item fill** or **Timer and item fill** for the rising reveal. Only the unrevealed part of the item's silhouette is dimmed: its original colours and texture show below the moving boundary, while transparent gaps and the inventory background stay untouched. **Progress opacity** controls the dimming for both fill and circular sweep styles. An unknown duration shows a static dim silhouette and **?** until a valid timer is available. If the sprite has not loaded, the fill waits for it while any countdown remains visible.

**Potion cooldowns** contains surge potions. **Potion effects** contains a separate checkbox for each of the 29 supported effect-item families. All remaining doses of a family share the same setting, so drinking a dose or rearranging your inventory keeps the overlay on the correct icon. Notes, placeholders, empty vials, and unrelated items do not receive overlays. Shared protection applies to all matching potion icons: for example, drinking extended antifire also shows the current antifire protection on ordinary antifire potions you carry.

**Raid potions** contains 13 settings: three strengths each of Chambers overloads, prayer enhances, and antipoisons, plus smelling salts, liquid adrenaline, silk dressing, and blessed crystal scarabs. Strength variants of the same Chambers effect share its timer, while each variant retains its own display checkbox. Ambrosia restores stats immediately and has no sustained effect timer to display.

**Boost potions** contains separate checkboxes for ordinary attack, strength, defence, their super versions, combat, super combat, ranging, magic, bastion, and battlemage potions. Dose and mix variants share the matching checkbox. Set independent **Attack below**, **Strength below**, **Defence below**, **Ranged below**, and **Magic below** levels. These are actual stat levels: **Strength below 110** becomes ready at **109**, and stays inactive at 110. Each target defaults to **0**, which disables that skill until you choose a level.

The **Super combat** checkbox also covers all four Last Man Standing super combat doses. These use their own Attack-and-Strength timer because they do not boost Defence. LMS ranging doses share the ordinary Ranging setting.

The timer counts down until a relevant stat falls strictly below its configured level. Combined potions use the first enabled, eligible stat to reach its own target. A dose must improve that stat before its icon can show a ready indicator, including when a target is higher than the potion's full boost. Enable **Show ready indicator** to show the selected signal, and carry a matching enabled potion in your inventory: without a potion, there is no icon or visual reminder. Older relative-boost thresholds are not converted into actual stat targets; set the new levels explicitly.

## Ready indicator

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

## Re-dose protection

Under **Re-dose protection**, enable **Prevent accidental re-dosing** to replace an inventory item's left-click use with an inert **Wait** entry while its known timer is active. This is off by default and respects each item family's checkbox. **Allow re-dose within** permits normal use during the final **0–60 seconds**, defaulting to **15 seconds**. Right-click retains the original use option, and holding **Shift** bypasses protection while preserving your normal Shift-click action.

Ordinary combat potions reuse the configured stat targets: **Strength below 110** protects against re-dosing at 110 or above and allows a dose at 109 if it would improve Strength. Protection leaves clicks available when a timer is unknown, stale, or unavailable. It does not block food, Sanfew serum, moonlight potions, silk dressing, blessed crystal scarabs, Scarred scraps, or barbarian mixes. Stamina and extended stamina remain usable below 100% run energy, and divine potions remain usable when a relevant stat has drained below their full boost.

Protection follows confirmed game state; it does not debounce rapid clicks before the server confirms a dose. Server timers remain authoritative, and this feature does not automate drinking.

## Settings preview

Enable **Show preview** near the top of the plugin settings to open a compact preview window alongside them. It shows the actual **Super combat potion(4)** icon at native size and enlarged, using the same renderer and current display settings as inventory items. Its independent countdown reaches ready after **10 seconds**. **Restart preview** starts the demonstration again.

Keep the preview open while changing display mode, opacity, ready style, colour, width, or duration. The preview respects **Show ready indicator**; turn that on to see ready signals. Changing ready-indicator settings after the countdown finishes replays the ready phase without another ten-second wait. Closing the preview or stopping the plugin stops its animation and turns **Show preview** off. The demonstration does not change game timers or require a potion in your inventory.
