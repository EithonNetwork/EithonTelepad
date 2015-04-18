package net.eithon.plugin.telepad;

import java.util.ArrayList;

import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.json.PlayerCollection;
import net.eithon.library.plugin.Configuration;
import net.eithon.library.time.CoolDown;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class Teleer {
	private static Teleer singleton = null;

	private PlayerCollection<TelePadInfo> _playersAboutToTele = null;
	CoolDown _coolDown = null;
	private AllTelePads _allTelePads = null;
	private EithonPlugin _eithonPlugin = null;
	private long _ticksBeforeTele;
	private long _nauseaTicks;
	private long _slownessTicks;
	private long _blindnessTicks;
	private long _disableEffectsAfterTicks;
	private int _secondsToPauseBeforeNextTeleport;

	private Teleer() {
		this._allTelePads = AllTelePads.get(this._eithonPlugin);
	}

	static Teleer get()
	{
		if (singleton == null) {
			singleton = new Teleer();
		}
		return singleton;
	}

	void enable(EithonPlugin eithonPlugin){
		this._eithonPlugin = eithonPlugin;
		Configuration config = eithonPlugin.getConfiguration();
		this._ticksBeforeTele = config.getInt("TeleportAfterTicks", 0);
		this._nauseaTicks = config.getInt("NauseaTicks", 0);
		this._slownessTicks = config.getInt("SlownessTicks", 0);
		this._blindnessTicks = config.getInt("BlindnessTicks", 0);
		this._disableEffectsAfterTicks = config.getInt("DisableEffectsAfterTicks", 0);
		this._secondsToPauseBeforeNextTeleport = config.getInt("SecondsToPauseBeforeNextTeleport", 5);
		this._coolDown = new CoolDown("telepad", this._secondsToPauseBeforeNextTeleport);
		this._playersAboutToTele = new PlayerCollection<TelePadInfo>(new TelePadInfo());
	}

	void disable() {
	}

	void maybeTele(Player player, Block pressurePlate) {
		if (pressurePlate.getType() != Material.STONE_PLATE) return;
		Location location = pressurePlate.getLocation();
		TelePadInfo info = this._allTelePads.getByLocation(location);
		if (info == null) return;
		
		/*
		if (!hasReadRules(player)) {
			maybeTellPlayerToReadTheRules(player);
			return;
		}
		*/
		if (isInCoolDownPeriod(player)) return;
		if (isAboutToTele(player)) return;
		
		float oldWalkSpeed = stopPlayer(player);
		teleSoon(player, info, oldWalkSpeed);
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

	private void teleSoon(Player player, TelePadInfo info, float oldWalkSpeed) {
		final float nextWalkSpeed =  (oldWalkSpeed > 0.0F ? oldWalkSpeed : 1.0F);
		setPlayerIsAboutToTele(player, info, true);
		ArrayList<PotionEffect> effects = new ArrayList<PotionEffect>();
		PotionEffect nausea = null;
		if (this._nauseaTicks > 0) {
			nausea = new PotionEffect(PotionEffectType.CONFUSION, (int) this._nauseaTicks, 4);
			effects.add(nausea);
		}
		final boolean hasNausea = nausea != null;
		PotionEffect slowness = null;
		if (this._nauseaTicks > 0) {
			slowness = new PotionEffect(PotionEffectType.SLOW, (int) this._slownessTicks, 4);
			effects.add(slowness);
		}
		final boolean hasSlowness = slowness != null;
		PotionEffect blindness = null;
		if (this._blindnessTicks > 0) {
			blindness = new PotionEffect(PotionEffectType.BLINDNESS, (int) this._blindnessTicks, 4);
			effects.add(blindness);
		}
		final boolean hasBlindness = blindness != null;
		player.addPotionEffects(effects);
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(this._eithonPlugin.getJavaPlugin(), new Runnable() {
			public void run() {
				player.setWalkSpeed(nextWalkSpeed);
				if (hasNausea) player.removePotionEffect(PotionEffectType.CONFUSION);
				if (hasSlowness) player.removePotionEffect(PotionEffectType.SLOW);
				if (hasBlindness) player.removePotionEffect(PotionEffectType.BLINDNESS);
			}
		}, this._disableEffectsAfterTicks);
		final Teleer instance = this;
		scheduler.scheduleSyncDelayedTask(this._eithonPlugin.getJavaPlugin(), new Runnable() {
			public void run() {
				if (!isAboutToTele(player)) return;
				setPlayerIsAboutToTele(player, info, false);
				instance.coolDown(player);
				tele(player, info);
			}
		}, this._ticksBeforeTele);
	}

	private float stopPlayer(Player player) {
		float walkSpeed = player.getWalkSpeed();
		player.setWalkSpeed(0.0F);
		player.setVelocity(new Vector(0.0, 0.0, 0.0));
		return walkSpeed;
	}

	void tele(Player player, TelePadInfo info) {
		Location targetLocation = info.getTargetLocation();
		player.teleport(targetLocation);
	}
	
	public void coolDown(Player player) {
		this._coolDown.addPlayer(player);
	}

	public boolean isInCoolDownPeriod(Player player) {
		return this._coolDown.isInCoolDownPeriod(player);
	}
}
