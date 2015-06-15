package net.eithon.plugin.telepad;

import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.plugin.ConfigurableMessage;
import net.eithon.library.plugin.Configuration;

public class Config {
	public static void load(EithonPlugin plugin)
	{
		Configuration config = plugin.getConfiguration();
		V.load(config);
		C.load(config);
		M.load(config);

	}
	public static class V {
		public static long ticksBeforeTele;
		public static long ticksBeforeJump;
		public static long nauseaTicks;
		public static long slownessTicks;
		public static long blindnessTicks;
		public static long disableEffectsAfterTicks;
		public static int secondsToPauseBeforeNextTeleport;
		
		static void load(Configuration config) {
			ticksBeforeTele = config.getInt("TeleportAfterTicks", 100);
			ticksBeforeJump = config.getInt("JumpAfterTicks", 0);
			nauseaTicks = config.getInt("NauseaTicks", 200);
			slownessTicks = config.getInt("SlownessTicks", 0);
			blindnessTicks = config.getInt("BlindnessTicks", 0);
			disableEffectsAfterTicks = config.getInt("DisableEffectsAfterTicks", 120);
			secondsToPauseBeforeNextTeleport = config.getInt("SecondsToPauseBeforeNextTeleport", 5);
		}

	}
	public static class C {

		static void load(Configuration config) {
		}

	}
	public static class M {
		public static ConfigurableMessage telePadAdded;	
		public static ConfigurableMessage nextStepAfterAdd;	
		public static ConfigurableMessage telePadRemoved;
		public static ConfigurableMessage telePadsLinked;
		public static ConfigurableMessage gotoTelePad;
		public static ConfigurableMessage movedOffTelePad;
		public static ConfigurableMessage unknownTelePad;

		static void load(Configuration config) {
			telePadAdded = config.getConfigurableMessage("TelePadAdded", 1,
					"TelePad %s has been added.");
			nextStepAfterAdd = config.getConfigurableMessage("NextStepAfterAdd", 1,
					"Now link this telepad with command /telepad link %s <other telepad name>.");
			telePadRemoved = config.getConfigurableMessage("TelePadRemoved", 1,
					"TelePad %s has been removed.");
			telePadsLinked = config.getConfigurableMessage("TelePadsLinked", 2,
					"TelePad %s and %s has been linked.");
			gotoTelePad = config.getConfigurableMessage("GotoTelepad", 1,
					"You have been teleported to TelePad %s.");
			movedOffTelePad = config.getConfigurableMessage("MovedOffTelePad_0", 0,
					"You moved away from the telepad, cancelling teleport.");
			unknownTelePad = config.getConfigurableMessage("UnknownTelePad_1", 1,
					"Unknown telepad (%s).");
		}		
	}

}
