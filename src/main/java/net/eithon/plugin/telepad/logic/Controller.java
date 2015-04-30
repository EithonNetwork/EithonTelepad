package net.eithon.plugin.telepad.logic;

import java.util.ArrayList;

import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.json.PlayerCollection;
import net.eithon.library.move.IBlockMoverFollower;
import net.eithon.library.move.MoveEventHandler;
import net.eithon.library.plugin.Configuration;
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
		if (pressurePlate.getType() != Material.STONE_PLATE) return;
		Location location = pressurePlate.getLocation();
		TelePadInfo info = this._allTelePads.getByLocation(location);
		if (info == null) return;
		if (isInCoolDownPeriod(player)) return;
		if (isAboutToTele(player)) return;

		float oldWalkSpeed = stopPlayer(player);
		teleSoon(player, info, oldWalkSpeed);
	}

	boolean isAboutToTele(Player player) {
		JumperInfo jumperInfo = this._playersAboutToTele.get(player);
		if (jumperInfo == null) return false;
		return jumperInfo.isAboutToTele();
	}

	private void teleSoon(Player player, TelePadInfo info, float oldWalkSpeed) {
		final float nextWalkSpeed =  (oldWalkSpeed > 0.0F ? oldWalkSpeed : 1.0F);
		JumperInfo jumperInfo = new JumperInfo(player);
		this._playersAboutToTele.put(player,  jumperInfo);
		MoveEventHandler.addBlockMover(player, this);
		
		ArrayList<PotionEffect> effects = new ArrayList<PotionEffect>();
		PotionEffect nausea = null;
		if (Config.V.nauseaTicks > 0) {
			nausea = new PotionEffect(PotionEffectType.CONFUSION, (int) Config.V.nauseaTicks, 4);
			effects.add(nausea);
			jumperInfo.setNausea(true);
		}
		PotionEffect slowness = null;
		if (Config.V.nauseaTicks > 0) {
			slowness = new PotionEffect(PotionEffectType.SLOW, (int) Config.V.slownessTicks, 4);
			effects.add(slowness);
			jumperInfo.setSlowness(true);
		}
		PotionEffect blindness = null;
		if (Config.V.blindnessTicks > 0) {
			blindness = new PotionEffect(PotionEffectType.BLINDNESS, (int) Config.V.blindnessTicks, 4);
			effects.add(blindness);
			jumperInfo.setBlindness(true);
		}
		player.addPotionEffects(effects);
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(this._eithonPlugin, new Runnable() {
			public void run() {
				player.setWalkSpeed(nextWalkSpeed);
				removeEffects(player, jumperInfo);
			}
		}, Config.V.disableEffectsAfterTicks);
		scheduler.scheduleSyncDelayedTask(this._eithonPlugin, new Runnable() {
			public void run() {
				if (!isAboutToTele(player)) return;
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
