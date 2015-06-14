package net.eithon.plugin.telepad.logic;

import java.util.ArrayList;

import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.move.IBlockMoverFollower;
import net.eithon.library.move.MoveEventHandler;
import net.eithon.library.plugin.Configuration;
import net.eithon.library.plugin.Logger.DebugPrintLevel;
import net.eithon.library.time.CoolDown;
import net.eithon.plugin.telepad.Config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class Controller implements IBlockMoverFollower {

	private net.eithon.library.core.PlayerCollection<JumperInfo> _playersAboutToTele = null;
	CoolDown _coolDown = null;
	private AllTelePads _allTelePads = null;
	private EithonPlugin _eithonPlugin = null;

	public Controller(EithonPlugin eithonPlugin){
		this._eithonPlugin = eithonPlugin;
		Configuration config = eithonPlugin.getConfiguration();
		this._coolDown = new CoolDown("telepad", Config.V.secondsToPauseBeforeNextTeleport);
		this._playersAboutToTele = new net.eithon.library.core.PlayerCollection<JumperInfo>();
		this._allTelePads = new AllTelePads(eithonPlugin);
		double seconds = config.getDouble("SecondsBeforeLoad", 5.0);
		this._allTelePads.delayedLoad(eithonPlugin, seconds);
	}

	public void maybeTele(Player player, Block pressurePlate) {
		debug("maybeTele", "Enter");
		
		if (isAboutToTele(player)) {
			debug("maybeTele", "Player already waiting for teleport to happen");
			return;
		}

		if (isInCoolDownPeriod(player)) {
			debug("maybeTele", "Player is in cool down period");
			return;
		}
		
		Location location = pressurePlate.getLocation();
		TelePadInfo info = this._allTelePads.getByLocation(location);
		if (info == null) return;

		debug("maybeTele", "Teleport sequence is starting");
		teleSoon(player, info);
	}

	boolean isAboutToTele(Player player) {
		JumperInfo jumperInfo = this._playersAboutToTele.get(player);
		if (jumperInfo == null) return false;
		return jumperInfo.isAboutToTele();
	}

	private void teleSoon(Player player, TelePadInfo info) {
		debug("teleSoon", "Enter");
		JumperInfo jumperInfo = new JumperInfo(player);
		this._playersAboutToTele.put(player,  jumperInfo);
		MoveEventHandler.addBlockMover(player, this);
		
		ArrayList<PotionEffect> effects = new ArrayList<PotionEffect>();
		PotionEffect nausea = null;
		if (Config.V.nauseaTicks > 0) {
			debug("teleSoon", "Add nausea");
			nausea = new PotionEffect(PotionEffectType.CONFUSION, (int) Config.V.nauseaTicks, 4);
			effects.add(nausea);
			jumperInfo.setNausea(true);
		}
		PotionEffect slowness = null;
		if (Config.V.nauseaTicks > 0) {
			debug("teleSoon", "Add slowness");
			slowness = new PotionEffect(PotionEffectType.SLOW, (int) Config.V.slownessTicks, 4);
			effects.add(slowness);
			jumperInfo.setSlowness(true);
		}
		PotionEffect blindness = null;
		if (Config.V.blindnessTicks > 0) {
			debug("teleSoon", "Add blindness");
			blindness = new PotionEffect(PotionEffectType.BLINDNESS, (int) Config.V.blindnessTicks, 4);
			effects.add(blindness);
			jumperInfo.setBlindness(true);
		}
		player.addPotionEffects(effects);
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(this._eithonPlugin, new Runnable() {
			public void run() {
				debug("teleSoon (delayed1)", "Remove effects");
				removeEffects(player, jumperInfo);
			}
		}, Config.V.disableEffectsAfterTicks);
		scheduler.scheduleSyncDelayedTask(this._eithonPlugin, new Runnable() {
			public void run() {
				debug("teleSoon (delayed2)", "Last chance to change our mind");
				if (!isAboutToTele(player)) {
					debug("teleSoon (delayed2)", "The teleport seems to have been cancelled");
					return;
				}
				debug("teleSoon (delayed2)", "Mark the player for teleportation");
				jumpOrTele(player, info, jumperInfo);
			}
		}, Config.V.ticksBeforeTele);
	}

	void removeEffects(Player player, JumperInfo jumperInfo) {
		jumperInfo.removeEffects();
		if (jumperInfo.canBeRemoved()) this._playersAboutToTele.remove(player);
	}

	private float stopPlayer(Player player) {
		float walkSpeed = player.getWalkSpeed();
		player.setWalkSpeed(0.0F);
		player.setVelocity(new Vector(0.0, 0.0, 0.0));
		return walkSpeed;
	}

	void jumpOrTele(Player player, TelePadInfo info, JumperInfo jumperInfo) {
		jumperInfo.setAboutToTele(false);
		if (jumperInfo.canBeRemoved()) this._playersAboutToTele.remove(player);
		MoveEventHandler.removeBlockMover(player, this);
		coolDown(player);
		debug("jumpOrTele", "Enter");
		if (info.hasVelocity()) {
			debug("jumpOrTele", "JUMP!");
			jump(player, info);
		}
		else {
			debug("jumpOrTele", "TELEPORT!");
			tele(player, info);
		}
	}

	private void tele(Player player, TelePadInfo info) {
		Location targetLocation = info.getTargetLocation();
		player.teleport(targetLocation);
	}

	private void jump(Player player, TelePadInfo info) {
		Vector jumpPadVelocity = info.getVelocity();
		Vector velocity = new Vector(jumpPadVelocity.getX(), jumpPadVelocity.getY(), jumpPadVelocity.getZ());
		player.setVelocity(velocity);
	}

	public void coolDown(Player player) {
		this._coolDown.addPlayer(player);
	}

	public boolean isInCoolDownPeriod(Player player) {
		return this._coolDown.isInCoolDownPeriod(player);
	}

	public void maybeStopTele(Player player) {
		if (!isAboutToTele(player)) return;
		Block block = player.getLocation().getBlock();
		if ((block != null) && (block.getType() == Material.STONE_PLATE)) return;
		this._playersAboutToTele.remove(player);
		Config.M.movedOffTelePad.sendMessage(player);
	}

	void debug(String method, String message) {
		this._eithonPlugin.getEithonLogger().debug(DebugPrintLevel.VERBOSE, "%s: %s", method, message);
	}

	@Override
	public void moveEventHandler(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		JumperInfo jumperInfo = this._playersAboutToTele.get(player);
		if ((jumperInfo == null) || !jumperInfo.isAboutToTele()) {
			return;
		}
		jumperInfo.setAboutToTele(false);
		removeEffects(player, jumperInfo);
		// TODO: Inform player about change of plans
	}

	@Override
	public String getName() {
		return this._eithonPlugin.getName();
	}
}
