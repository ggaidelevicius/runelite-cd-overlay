package com.cooldownoverlays;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(CooldownOverlaysConfig.GROUP)
public interface CooldownOverlaysConfig extends Config
{
	String GROUP = "cooldown-overlays";

	@ConfigSection(
		name = "Hearts",
		description = "Choose which heart items display their shared cooldown",
		position = 10
	)
	String heartsSection = "hearts";

	@ConfigSection(
		name = "Potion cooldowns",
		description = "Choose which potions show their reuse cooldown",
		position = 11
	)
	String potionCooldownsSection = "potionCooldowns";

	@ConfigSection(
		name = "Potion effects",
		description = "Show the remaining effect duration on potion items in your inventory",
		position = 12
	)
	String potionEffectsSection = "potionEffects";

	@ConfigSection(
		name = "Boost potions",
		description = "Choose actual skill levels to re-dose below; 0 disables a skill",
		position = 13
	)
	String boostPotionsSection = "boostPotions";

	@ConfigSection(
		name = "Animations",
		description = "Animate inventory icons when their items are ready to use",
		position = 5
	)
	String animationsSection = "animations";

	@ConfigSection(
		name = "Raid potions",
		description = "Choose which raid supplies show their remaining effect duration",
		position = 20
	)
	String raidPotionsSection = "raidPotions";

	@ConfigItem(
		keyName = "displayMode",
		name = "Display mode",
		description = "Show a timer, a circular sweep, or an item fill that reveals the original icon from bottom to top",
		position = 0
	)
	default DisplayMode displayMode()
	{
		return DisplayMode.BOTH;
	}

	@Range(min = 0, max = 100)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "circleOpacity",
		name = "Progress opacity",
		description = "How much to darken the remaining circular sweep or unfilled part of the item",
		position = 1
	)
	default int circleOpacity()
	{
		return 60;
	}

	@ConfigItem(
		keyName = "timerColor",
		name = "Timer color",
		description = "Color of the countdown text",
		position = 2
	)
	default Color timerColor()
	{
		return Color.WHITE;
	}

	@ConfigItem(
		keyName = "animateActivation",
		name = "Animate when ready",
		description = "Animate an item when its cooldown ends, effect expires, or a relevant stat falls below its selected level",
		position = 0,
		section = animationsSection
	)
	default boolean animateWhenReady()
	{
		return false;
	}

	@ConfigItem(
		keyName = "animationMode",
		name = "Animation length",
		description = "Keep the border animated while ready, or stop after the selected duration",
		position = 1,
		section = animationsSection
	)
	default AnimationMode animationMode()
	{
		return AnimationMode.TIMED;
	}

	@Range(min = 1, max = 10)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "animationDuration",
		name = "Animation duration",
		description = "How long the ready border remains visible in duration mode",
		position = 2,
		section = animationsSection
	)
	default int animationDuration()
	{
		return 3;
	}

	@ConfigItem(
		keyName = "animationColor",
		name = "Animation color",
		description = "Color of the animated ready border",
		position = 3,
		section = animationsSection
	)
	default Color animationColor()
	{
		return new Color(255, 205, 64);
	}

	@ConfigItem(
		keyName = "animationBorderStyle",
		name = "Border style",
		description = "Trace the item's visible outline, or draw a border around its inventory slot",
		position = 4,
		section = animationsSection
	)
	default BorderStyle animationBorderStyle()
	{
		return BorderStyle.ITEM_OUTLINE;
	}

	@Range(min = 1, max = 6)
	@Units(Units.PIXELS)
	@ConfigItem(
		keyName = "animationBorderWidth",
		name = "Border width",
		description = "Width of the activation border's main visible stroke in pixels",
		position = 5,
		section = animationsSection
	)
	default int animationBorderWidth()
	{
		return 1;
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "attackThreshold", name = "Attack below",
		description = "Animate carried Attack-boosting potions below this actual Attack level; 0 disables this target, and a dose must improve Attack",
		position = 0, section = boostPotionsSection
	)
	default int attackThreshold()
	{
		return 0;
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "strengthThreshold", name = "Strength below",
		description = "Animate carried Strength-boosting potions below this actual Strength level; 110 activates at 109, 0 disables, and a dose must improve Strength",
		position = 1, section = boostPotionsSection
	)
	default int strengthThreshold()
	{
		return 0;
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "defenceThreshold", name = "Defence below",
		description = "Animate carried Defence-boosting potions below this actual Defence level; 0 disables this target, and a dose must improve Defence",
		position = 2, section = boostPotionsSection
	)
	default int defenceThreshold()
	{
		return 0;
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "rangedThreshold", name = "Ranged below",
		description = "Animate carried Ranged-boosting potions below this actual Ranged level; 0 disables this target, and a dose must improve Ranged",
		position = 3, section = boostPotionsSection
	)
	default int rangedThreshold()
	{
		return 0;
	}

	@Range(min = 0, max = 255)
	@ConfigItem(
		keyName = "magicThreshold", name = "Magic below",
		description = "Animate carried Magic-boosting potions below this actual Magic level; 0 disables this target, and a dose must improve Magic",
		position = 4, section = boostPotionsSection
	)
	default int magicThreshold()
	{
		return 0;
	}

	@ConfigItem(keyName = "attackPotion", name = "Attack potion",
		description = "Show time until Attack falls below its selected level",
		position = 5, section = boostPotionsSection)
	default boolean attackPotion()
	{
		return true;
	}

	@ConfigItem(keyName = "strengthPotion", name = "Strength potion",
		description = "Show time until Strength falls below its selected level",
		position = 6, section = boostPotionsSection)
	default boolean strengthPotion()
	{
		return true;
	}

	@ConfigItem(keyName = "defencePotion", name = "Defence potion",
		description = "Show time until Defence falls below its selected level",
		position = 7, section = boostPotionsSection)
	default boolean defencePotion()
	{
		return true;
	}

	@ConfigItem(keyName = "superAttackPotion", name = "Super attack",
		description = "Show time until Attack falls below its selected level",
		position = 8, section = boostPotionsSection)
	default boolean superAttackPotion()
	{
		return true;
	}

	@ConfigItem(keyName = "superStrengthPotion", name = "Super strength",
		description = "Show time until Strength falls below its selected level",
		position = 9, section = boostPotionsSection)
	default boolean superStrengthPotion()
	{
		return true;
	}

	@ConfigItem(keyName = "superDefencePotion", name = "Super defence",
		description = "Show time until Defence falls below its selected level",
		position = 10, section = boostPotionsSection)
	default boolean superDefencePotion()
	{
		return true;
	}

	@ConfigItem(keyName = "combatPotion", name = "Combat potion",
		description = "Show time until Attack or Strength falls below its selected level",
		position = 11, section = boostPotionsSection)
	default boolean combatPotion()
	{
		return true;
	}

	@ConfigItem(keyName = "superCombatPotion", name = "Super combat",
		description = "Show time until Attack, Strength, or Defence falls below its selected level",
		position = 12, section = boostPotionsSection)
	default boolean superCombatPotion()
	{
		return true;
	}

	@ConfigItem(keyName = "rangingPotion", name = "Ranging potion",
		description = "Show time until Ranged falls below its selected level",
		position = 13, section = boostPotionsSection)
	default boolean rangingPotion()
	{
		return true;
	}

	@ConfigItem(keyName = "magicPotion", name = "Magic potion",
		description = "Show time until Magic falls below its selected level",
		position = 14, section = boostPotionsSection)
	default boolean magicPotion()
	{
		return true;
	}

	@ConfigItem(keyName = "bastionPotion", name = "Bastion potion",
		description = "Show time until Ranged or Defence falls below its selected level",
		position = 15, section = boostPotionsSection)
	default boolean bastionPotion()
	{
		return true;
	}

	@ConfigItem(keyName = "battlemagePotion", name = "Battlemage potion",
		description = "Show time until Magic or Defence falls below its selected level",
		position = 16, section = boostPotionsSection)
	default boolean battlemagePotion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "imbuedHeart",
		name = "Imbued heart",
		description = "Display the heart cooldown on imbued hearts in your inventory",
		position = 0,
		section = heartsSection
	)
	default boolean imbuedHeart()
	{
		return true;
	}

	@ConfigItem(
		keyName = "saturatedHeart",
		name = "Saturated heart",
		description = "Display the heart cooldown on saturated hearts in your inventory",
		position = 1,
		section = heartsSection
	)
	default boolean saturatedHeart()
	{
		return true;
	}

	@ConfigItem(
		keyName = "surgePotion", name = "Surge potion",
		description = "Show the shared reuse cooldown on every surge potion dose",
		position = 0, section = potionCooldownsSection
	)
	default boolean surgePotion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "staminaPotion", name = "Stamina potion",
		description = "Show stamina effect time on stamina potions and stamina mixes",
		position = 0, section = potionEffectsSection
	)
	default boolean staminaPotion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "extendedStaminaPotion", name = "Extended stamina potion",
		description = "Show stamina effect time on extended stamina potions",
		position = 1, section = potionEffectsSection
	)
	default boolean extendedStaminaPotion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "antifirePotion", name = "Antifire potion",
		description = "Show antifire protection time on antifire potions and mixes",
		position = 2, section = potionEffectsSection
	)
	default boolean antifirePotion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "extendedAntifirePotion", name = "Extended antifire potion",
		description = "Show antifire protection time on extended antifire potions and mixes",
		position = 3, section = potionEffectsSection
	)
	default boolean extendedAntifirePotion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "superAntifirePotion", name = "Super antifire potion",
		description = "Show super antifire protection time on super antifire potions and mixes",
		position = 4, section = potionEffectsSection
	)
	default boolean superAntifirePotion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "extendedSuperAntifirePotion", name = "Extended super antifire",
		description = "Show super antifire protection time on extended super antifire potions and mixes",
		position = 5, section = potionEffectsSection
	)
	default boolean extendedSuperAntifirePotion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "antiVenom", name = "Anti-venom",
		description = "Show remaining venom immunity on anti-venom potions",
		position = 6, section = potionEffectsSection
	)
	default boolean antiVenom()
	{
		return true;
	}

	@ConfigItem(
		keyName = "antiVenomPlus", name = "Anti-venom+",
		description = "Show remaining venom immunity on anti-venom+ potions",
		position = 7, section = potionEffectsSection
	)
	default boolean antiVenomPlus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "extendedAntiVenomPlus", name = "Extended anti-venom+",
		description = "Show remaining venom immunity on extended anti-venom+ potions",
		position = 8, section = potionEffectsSection
	)
	default boolean extendedAntiVenomPlus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "divineSuperAttack", name = "Divine super attack",
		description = "Show divine Attack boost time on divine super attack potions",
		position = 9, section = potionEffectsSection
	)
	default boolean divineSuperAttack()
	{
		return true;
	}

	@ConfigItem(
		keyName = "divineSuperStrength", name = "Divine super strength",
		description = "Show divine Strength boost time on divine super strength potions",
		position = 10, section = potionEffectsSection
	)
	default boolean divineSuperStrength()
	{
		return true;
	}

	@ConfigItem(
		keyName = "divineSuperDefence", name = "Divine super defence",
		description = "Show divine Defence boost time on divine super defence potions",
		position = 11, section = potionEffectsSection
	)
	default boolean divineSuperDefence()
	{
		return true;
	}

	@ConfigItem(
		keyName = "divineRanging", name = "Divine ranging",
		description = "Show divine Ranged boost time on divine ranging potions",
		position = 12, section = potionEffectsSection
	)
	default boolean divineRanging()
	{
		return true;
	}

	@ConfigItem(
		keyName = "divineMagic", name = "Divine magic",
		description = "Show divine Magic boost time on divine magic potions",
		position = 13, section = potionEffectsSection
	)
	default boolean divineMagic()
	{
		return true;
	}

	@ConfigItem(
		keyName = "divineSuperCombat", name = "Divine super combat",
		description = "Show the combined divine boost time on divine super combat potions",
		position = 14, section = potionEffectsSection
	)
	default boolean divineSuperCombat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "divineBastion", name = "Divine bastion",
		description = "Show the combined divine boost time on divine bastion potions",
		position = 15, section = potionEffectsSection
	)
	default boolean divineBastion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "divineBattlemage", name = "Divine battlemage",
		description = "Show the combined divine boost time on divine battlemage potions",
		position = 16, section = potionEffectsSection
	)
	default boolean divineBattlemage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "antipoison", name = "Antipoison",
		description = "Show poison immunity on antipoison doses and mixes",
		position = 17, section = potionEffectsSection
	)
	default boolean antipoison()
	{
		return true;
	}

	@ConfigItem(
		keyName = "superAntipoison", name = "Superantipoison",
		description = "Show poison immunity on superantipoison doses and mixes",
		position = 18, section = potionEffectsSection
	)
	default boolean superAntipoison()
	{
		return true;
	}

	@ConfigItem(
		keyName = "antidotePlus", name = "Antidote+",
		description = "Show poison immunity on antidote+ doses and mixes",
		position = 19, section = potionEffectsSection
	)
	default boolean antidotePlus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "antidotePlusPlus", name = "Antidote++",
		description = "Show poison immunity on antidote++ doses",
		position = 20, section = potionEffectsSection
	)
	default boolean antidotePlusPlus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sanfewSerum", name = "Sanfew serum",
		description = "Show poison immunity on Sanfew serum doses",
		position = 21, section = potionEffectsSection
	)
	default boolean sanfewSerum()
	{
		return true;
	}

	@ConfigItem(
		keyName = "goadingPotion", name = "Goading potion",
		description = "Show the remaining goading effect on every dose",
		position = 22, section = potionEffectsSection
	)
	default boolean goadingPotion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "menaphiteRemedy", name = "Menaphite remedy",
		description = "Show the remaining periodic stat restoration effect on every dose",
		position = 23, section = potionEffectsSection
	)
	default boolean menaphiteRemedy()
	{
		return true;
	}

	@ConfigItem(
		keyName = "moonlightPotion", name = "Moonlight potion",
		description = "Show the remaining Moonlight potion effect on every dose",
		position = 24, section = potionEffectsSection
	)
	default boolean moonlightPotion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayerRegenerationPotion", name = "Prayer regeneration",
		description = "Show the remaining prayer regeneration effect on every dose",
		position = 25, section = potionEffectsSection
	)
	default boolean prayerRegenerationPotion()
	{
		return true;
	}

	@ConfigItem(
		keyName = "nmzOverload", name = "Nightmare Zone overload",
		description = "Show the Nightmare Zone overload effect on every dose",
		position = 26, section = potionEffectsSection
	)
	default boolean nmzOverload()
	{
		return true;
	}

	@ConfigItem(
		keyName = "blightedOverload", name = "Blighted overload",
		description = "Show the blighted overload effect on every dose",
		position = 27, section = potionEffectsSection
	)
	default boolean blightedOverload()
	{
		return true;
	}

	@ConfigItem(
		keyName = "scarredScraps", name = "Scarred scraps",
		description = "Show the stamina effect on Scarred scraps",
		position = 28, section = potionEffectsSection
	)
	default boolean scarredScraps()
	{
		return true;
	}

	@ConfigItem(
		keyName = "weakOverload", name = "Overload (-)",
		description = "Show the Chambers overload effect time on weak overload doses",
		position = 0, section = raidPotionsSection
	)
	default boolean weakOverload()
	{
		return true;
	}

	@ConfigItem(
		keyName = "overload", name = "Overload",
		description = "Show the Chambers overload effect time on regular overload doses",
		position = 1, section = raidPotionsSection
	)
	default boolean overload()
	{
		return true;
	}

	@ConfigItem(
		keyName = "strongOverload", name = "Overload (+)",
		description = "Show the Chambers overload effect time on strong overload doses",
		position = 2, section = raidPotionsSection
	)
	default boolean strongOverload()
	{
		return true;
	}

	@ConfigItem(
		keyName = "weakPrayerEnhance", name = "Prayer enhance (-)",
		description = "Show Chambers prayer enhancement time on weak doses",
		position = 3, section = raidPotionsSection
	)
	default boolean weakPrayerEnhance()
	{
		return true;
	}

	@ConfigItem(
		keyName = "prayerEnhance", name = "Prayer enhance",
		description = "Show Chambers prayer enhancement time on regular doses",
		position = 4, section = raidPotionsSection
	)
	default boolean prayerEnhance()
	{
		return true;
	}

	@ConfigItem(
		keyName = "strongPrayerEnhance", name = "Prayer enhance (+)",
		description = "Show Chambers prayer enhancement time on strong doses",
		position = 5, section = raidPotionsSection
	)
	default boolean strongPrayerEnhance()
	{
		return true;
	}

	@ConfigItem(
		keyName = "smellingSalts", name = "Smelling salts",
		description = "Show the Tombs of Amascut salts effect time on both doses",
		position = 6, section = raidPotionsSection
	)
	default boolean smellingSalts()
	{
		return true;
	}

	@ConfigItem(
		keyName = "liquidAdrenaline", name = "Liquid adrenaline",
		description = "Show Tombs of Amascut special attack cost reduction time on both doses",
		position = 7, section = raidPotionsSection
	)
	default boolean liquidAdrenaline()
	{
		return true;
	}

	@ConfigItem(
		keyName = "silkDressing", name = "Silk dressing",
		description = "Show the remaining healing effect on both silk dressings",
		position = 8, section = raidPotionsSection
	)
	default boolean silkDressing()
	{
		return true;
	}

	@ConfigItem(
		keyName = "blessedCrystalScarab", name = "Blessed crystal scarab",
		description = "Show the remaining prayer restoration effect on both scarabs",
		position = 9, section = raidPotionsSection
	)
	default boolean blessedCrystalScarab()
	{
		return true;
	}

	@ConfigItem(
		keyName = "weakRaidAntipoison", name = "Chambers antipoison (-)",
		description = "Show poison immunity on weak Chambers antipoison doses",
		position = 10, section = raidPotionsSection
	)
	default boolean weakRaidAntipoison()
	{
		return true;
	}

	@ConfigItem(
		keyName = "raidAntipoison", name = "Chambers antipoison",
		description = "Show poison immunity on regular Chambers antipoison doses",
		position = 11, section = raidPotionsSection
	)
	default boolean raidAntipoison()
	{
		return true;
	}

	@ConfigItem(
		keyName = "strongRaidAntipoison", name = "Chambers antipoison (+)",
		description = "Show poison immunity on strong Chambers antipoison doses",
		position = 12, section = raidPotionsSection
	)
	default boolean strongRaidAntipoison()
	{
		return true;
	}
}
