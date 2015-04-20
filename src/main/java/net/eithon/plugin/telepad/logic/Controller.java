package net.eithon.plugin.telepad.logic;

import java.util.ArrayList;

import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.json.PlayerCollection;
import net.eithon.library.plugin.Configuration;
import net.eithon.library.time.CoolDown;
import net.eithon.plugin.telepad.Config;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class Controller {

	private PlayerCollection<TelePadInfo> _playersAboutToTele = null;
	CoolDown _coolDown = null;
	private AllTelePads _allTelePads = null;
	private EithonPlugin _eithonPlugin = null;

	public Controller(EithonPlugin eithonPlugin){
		this._eithonPlugin = eithonPlugin;
		Configuration config = eithonPlugin.getConfiguration();
		this._coolDown = new CoolDown("telepad", Config.V.secondsToPauseBeforeNextTeleport);
		this._playersAboutToTele = new PlayerCollection<TelePadInfo>(new TelePadInfo());
		this._allTelePads = new AllTelePads(eithonPlugin);
		double seconds = config.getDouble("SecondsBeforeLoad", 5.0);
		this._allTelePads.delayedLoad(eithonPlugin, seconds);
	}

	public void maybeTele(Player player, Block pressurePlate) {
		if (pressurePlate.getType() != Material.STONE_PLATE) return;
		Location location = pressurePlate.getLocation();
		TelePadInfo info = this._allTelePads.getByLocation(location);
		if (info == null) return;

		if (isInCoolDownPeriod(player)) return;
		if (isAboutToTele(player)) return;
		
		teleSoon(player, info);
	}

	boolean isAboutToTele(Player player) {
		return this._playersAboutToTele.hasInformation(player);
	}

	void setPlayerIsAboutToTele(Player player, TelePadInfo info, boolean isAboutToTele) {
		if (isAboutToTele) {
			if (isAboutToTele(player)) return;
			this._playersAboutToTele.put(player, info);
		} else {
			if (!isAboutToTele(player)) return;
			this._playersAboutToTele.remove(player);
		}
	}

	private void teleSoon(Player player, TelePadInfo info) {
		setPlayerIsAboutToTele(player, info, true);
		ArrayList<PotionEffect> effects = new ArrayList<PotionEffect>();
		PotionEffect nausea = null;
		if (Config.V.nauseaTicks > 0) {
			nausea = new PotionEffect(PotionEffectType.CONFUSION, (int) Config.V.nauseaTicks, 4);
			effects.add(nausea);
		}
		final boolean hasNausea = nausea != null;
		PotionEffect slowness = null;
		if (Config.V.nauseaTicks > 0) {
			slowness = new PotionEffect(PotionEffectType.SLOW, (int) Config.V.slownessTicks, 4);
			effects.add(slowness);
		}
		final boolean hasSlowness = slowness != null;
		PotionEffect blindness = null;
		if (Config.V.blindnessTicks > 0) {
			blindness = new PotionEffect(PotionEffectType.BLINDNESS, (int) Config.V.blindnessTicks, 4);
			effects.add(blindness);
		}
		final boolean hasBlindness = blindness != null;
		player.addPotionEffects(effects);
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(this._eithonPlugin, new Runnable() {
			public void run() {
				if (hasNausea) player.removePotionEffect(PotionEffectType.CONFUSION);
				if (hasSlowness) player.removePotionEffect(PotionEffectType.SLOW);
				if (hasBlindness) player.removePotionEffect(PotionEffectType.BLINDNESS);
			}
		}, Config.V.disableEffectsAfterTicks);
		final Controller instance = this;
		scheduler.scheduleSyncDelayedTask(this._eithonPlugin, new Runnable() {
			public void run() {
				if (!isAboutToTele(player)) return;
				setPlayerIsAboutToTele(player, info, false);
				instance.coolDown(player);
				jumpOrTele(player, info);
			}
		}, Config.V.ticksBeforeTele);
	}

	private float stopPlayer(Player player) {
		float walkSpeed = player.getWalkSpeed();
		player.setWalkSpeed(0.0F);
		player.setVelocity(new Vector(0.0, 0.0, 0.0));
		return walkSpeed;
	}

	void jumpOrTele(Player player, TelePadInfo info) {
		if (info.hasVelocity()) jump(player, info);
		else tele(player, info);
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
		setPlayerIsAboutToTele(player, null, false);
		Config.M.movedOffTelePad.sendMessage(player);
	}
}
