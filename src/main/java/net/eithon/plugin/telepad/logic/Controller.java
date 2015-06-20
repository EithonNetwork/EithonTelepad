package net.eithon.plugin.telepad.logic;

import java.util.ArrayList;

import net.eithon.library.extensions.EithonLocation;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class Controller implements IBlockMoverFollower {

	net.eithon.library.core.PlayerCollection<JumperInfo> _playersAboutToTele = null;
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

	public boolean createOrUpdateTelePad(Player player, String name, double upSpeed, double forwardSpeed) {
		EithonLocation eithonLocation = new EithonLocation(player.getLocation());
		Block pressurePlate = eithonLocation.searchForFirstBlockOfMaterial(Material.STONE_PLATE, 3);
		if (pressurePlate == null) {
			player.sendMessage("No stone plate within 3 blocks.");
			return false;
		}

		Location playerLocation = player.getLocation();
		Location padLocation = pressurePlate.getLocation();
		// Remember where the player looked when the telepad was created/updated
		padLocation.setYaw(playerLocation.getYaw());
		padLocation.setPitch(playerLocation.getPitch());

		TelePadInfo telePadInfo = this._allTelePads.getByName(name);
		if ((upSpeed != 0.0) || (forwardSpeed != 0.0)) {
			if (telePadInfo != null) telePadInfo.setVelocity(upSpeed, forwardSpeed, playerLocation.getYaw());
			else telePadInfo = new TelePadInfo(name, padLocation, upSpeed, forwardSpeed, playerLocation.getYaw(), player);
		} else {
			if (telePadInfo != null) telePadInfo.setTarget(telePadInfo);
			else telePadInfo = new TelePadInfo(name, padLocation, padLocation, player);
		}
		this._allTelePads.add(telePadInfo);
		if (player != null) coolDown(player);
		save();
		return true;
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
		if (!info.isJumpPad()) {
			debug("teleSoon", "Add potion effects for teleport.");
			addPotionEffects(player, jumperInfo);
			delayedRemoveEffects(player, jumperInfo);
			debug("teleSoon", "Call delayedTeleport");
			delayedTeleport(player, info, jumperInfo);
		} else {
			debug("teleSoon", "No potion effects for jump.");
			debug("teleSoon", "Call delayedJump");
			delayedJump(player, info, jumperInfo);			
		}
		debug("teleSoon", "Leave");
	}

	private void addPotionEffects(Player player, JumperInfo jumperInfo) {
		ArrayList<PotionEffect> effects = new ArrayList<PotionEffect>();
		PotionEffect nausea = null;
		if (Config.V.nauseaTicks > 0) {
			debug("addPotionEffects", "Add nausea");
			nausea = new PotionEffect(PotionEffectType.CONFUSION, (int) Config.V.nauseaTicks, 4);
			effects.add(nausea);
			jumperInfo.setNausea(true);
		}
		PotionEffect slowness = null;
		if (Config.V.nauseaTicks > 0) {
			debug("addPotionEffects", "Add slowness");
			slowness = new PotionEffect(PotionEffectType.SLOW, (int) Config.V.slownessTicks, 4);
			effects.add(slowness);
			jumperInfo.setSlowness(true);
		}
		PotionEffect blindness = null;
		if (Config.V.blindnessTicks > 0) {
			debug("addPotionEffects", "Add blindness");
			blindness = new PotionEffect(PotionEffectType.BLINDNESS, (int) Config.V.blindnessTicks, 4);
			effects.add(blindness);
			jumperInfo.setBlindness(true);
		}
		player.addPotionEffects(effects);
	}

	private void delayedRemoveEffects(
			Player player,
			JumperInfo jumperInfo) {
		Controller thisController = this;
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(this._eithonPlugin, new Runnable() {
			public void run() {
				JumperInfo latestJumperInfo = thisController._playersAboutToTele.get(player);
				if (!jumperInfo.isSame(latestJumperInfo)){
					debug("delayedRemoveEffects", "There seems to exist a new teleport. Skip this.");
					return;
				}				debug("delayedRemoveEffects", "Remove effects");
				removeEffects(player, jumperInfo);
			}
		}, Config.V.disableEffectsAfterTicks);
	}

	private void delayedJump(
			Player player, 
			TelePadInfo info,
			JumperInfo jumperInfo) {
		debug("delayedJump", "Enter");
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(this._eithonPlugin, new Runnable() {
			public void run() {
				debug("delayedJump task", "Prepare");
				prepareForJumpOrTele(player, info, jumperInfo);
				debug("delayedJump task", "JUMP!");
				jump(player, info);
			}
		}, Config.V.ticksBeforeJump);
		debug("delayedJump", "Leave");
	}

	private void delayedTeleport(
			Player player, 
			TelePadInfo info,
			JumperInfo jumperInfo) {
		debug("delayedTeleport", "Enter");
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(this._eithonPlugin, new Runnable() {
			public void run() {
				debug("delayedTeleport task", "Prepare");
				prepareForJumpOrTele(player, info, jumperInfo);
				debug("delayedTeleport task", "TELEPORT!");
				tele(player, info);
			}
		}, Config.V.ticksBeforeTele);
		debug("delayedTeleport", "Leave");
	}

	void prepareForJumpOrTele(Player player, TelePadInfo info, JumperInfo jumperInfo) {
		debug("prepareForJumpOrTele", "Enter");
		JumperInfo latestJumperInfo = this._playersAboutToTele.get(player);
		if (!jumperInfo.isSame(latestJumperInfo)){
			debug("prepareForJumpOrTele", "There seems to exist a another new jump/teleport. Skip this one.");
			return;
		}
		debug("prepareForJumpOrTele", "Last chance to change our mind");
		if (!isAboutToTele(player)) {
			debug("delayedTeleport", "The jump/teleport seems to have been cancelled");
			return;
		}
		jumperInfo.setAboutToTele(false);
		if (jumperInfo.canBeRemoved()) this._playersAboutToTele.remove(player);
		MoveEventHandler.removeBlockMover(player, this);
		coolDown(player);
	}

	void removeEffects(Player player, JumperInfo jumperInfo) {
		jumperInfo.removeEffects();
		if (jumperInfo.canBeRemoved()) this._playersAboutToTele.remove(player);
	}
	
	/*
	private float stopPlayer(Player player) {
		float walkSpeed = player.getWalkSpeed();
		player.setWalkSpeed(0.0F);
		player.setVelocity(new Vector(0.0, 0.0, 0.0));
		return walkSpeed;
	}
	*/

	void tele(Player player, TelePadInfo info) {
		Location targetLocation = info.getTargetLocation();
		player.teleport(targetLocation);
	}

	void jump(Player player, TelePadInfo info) {
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

	public boolean maybeStopTele(Player player) {
		debug("maybeStopTele", String.format("Enter (for player %s)", player.getName()));
		JumperInfo jumperInfo = this._playersAboutToTele.get(player);
		if ((jumperInfo == null) || !jumperInfo.isAboutToTele()) {
			debug("maybeStopTele", "User is not about to jump.");
			debug("maybeStopTele", "Leave");
			return false;
		}
		Block block = player.getLocation().getBlock();
		if ((block != null) && (block.getType() == Material.STONE_PLATE)) {
			debug("maybeStopTele", "User is still on stone plate.");
			debug("maybeStopTele", "Leave");
			return false;
		}
		jumperInfo.setAboutToTele(false);
		this._playersAboutToTele.remove(player);
		removeEffects(player, jumperInfo);
		this._coolDown.removePlayer(player);
		Config.M.movedOffTelePad.sendMessage(player);
		debug("maybeStopTele", "Leave");
		return true;
	}

	void debug(String method, String message) {
		this._eithonPlugin.getEithonLogger().debug(DebugPrintLevel.VERBOSE, "%s: %s", method, message);
	}

	@Override
	public void moveEventHandler(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		debug("TelePad.moveEventHandler", String.format("Enter (for player %s)", player.getName()));
		if (maybeStopTele(player)) {
			debug("TelePad.moveEventHandler", String.format("Stop following player %s", player.getName()));
			MoveEventHandler.removeBlockMover(player, this);
		}
		debug("TelePad.moveEventHandler", "Leave");
	}

	public TelePadInfo getByNameOrInformUser(CommandSender sender, String name) {
		TelePadInfo info = this._allTelePads.getByName(name);
		if (info != null) return info;
		Config.M.unknownTelePad.sendMessage(sender, name);
		return null;
	}

	public boolean verifyNameIsNew(Player player, String name) {
		TelePadInfo info = this._allTelePads.getByName(name);
		if (info != null)
		{
			player.sendMessage("Telepad already exists: " + name);
			return true;		
		}
		return true;
	}

	@Override
	public String getName() {
		return this._eithonPlugin.getName();
	}

	public void save() {
		this._allTelePads.delayedSave(this._eithonPlugin, 0.0);	}

	public void remove(TelePadInfo info) {
		this._allTelePads.remove(info);	
		save();
	}

	public void link(TelePadInfo info1, TelePadInfo info2) {
		info1.setTarget(info2);
		info2.setTarget(info1);
		save();
	}

	public void gotoTelepad(Player player, TelePadInfo info) {	
		player.teleport(info.getSourceAsTarget());
		coolDown(player);
	}

	public void listTelepads(Player player) {
		for (TelePadInfo info : this._allTelePads.getAll()) {
			player.sendMessage(info.toString());
		}
	}
}
