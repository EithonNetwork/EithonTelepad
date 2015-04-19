package net.eithon.plugin.telepad.logic;

import net.eithon.library.core.PlayerCollection;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Vector;

public class Jumper {

	private PlayerCollection<Object> playersThatHasBeenInformedToReadTheRules = null;
	private PlayerCollection<TelePadInfo> playersAboutToJump = null;
	private PlayerCollection<Object> playersWithTemporaryJumpPause = null;
	private AllTelePads allJumpPads = null;
	private JavaPlugin plugin = null;

	public Jumper(JavaPlugin plugin){
		this.plugin = plugin;
		this.playersThatHasBeenInformedToReadTheRules = new PlayerCollection<Object>();
		this.playersWithTemporaryJumpPause = new PlayerCollection<Object>();
		this.playersAboutToJump = new PlayerCollection<TelePadInfo>();
	}

	void disable() {
	}

	void maybeJump(Player player, Block pressurePlate) {
		if (pressurePlate.getType() != Material.STONE_PLATE) return;
		Location location = pressurePlate.getLocation();
		TelePadInfo info = this.allJumpPads.getByLocation(location);
		if (info == null) {
			mustReadRules(player, true);
			playerCanJump(player, true);
			return;
		}
		
		if (!hasReadRules(player)) {
			maybeTellPlayerToReadTheRules(player);
			return;
		}
		if (hasTemporaryJumpPause(player)) return;
		if (isAboutToJump(player)) return;
		
		float oldWalkSpeed = stopPlayer(player);
		jumpSoon(player, info, oldWalkSpeed);
	}

	boolean isAboutToJump(Player player) {
		return this.playersAboutToJump.hasInformation(player);
	}

	void setPlayerIsAboutToJump(Player player, TelePadInfo info, boolean isAboutToJump) {
		if (isAboutToJump) {
			if (isAboutToJump(player)) return;
			this.playersAboutToJump.put(player, info);
		} else {
			if (!isAboutToJump(player)) return;
			this.playersAboutToJump.remove(player);
		}
	}

	private void jumpSoon(Player player, TelePadInfo info, float oldWalkSpeed) {
		setPlayerIsAboutToJump(player, info, true);
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(this.plugin, new Runnable() {
			public void run() {
				if (!isAboutToJump(player)) return;
				setPlayerIsAboutToJump(player, info, false);
				player.setWalkSpeed(oldWalkSpeed);
				jump(player, info);
			}
		}, 20L);
	}

	private float stopPlayer(Player player) {
		player.setVelocity(new Vector(0.0, 0.0, 0.0));
		float walkSpeed = player.getWalkSpeed();
		player.setWalkSpeed(0.0F);
		return walkSpeed;
	}

	void jump(Player player, TelePadInfo info) {
		Vector jumpPadVelocity = info.getVelocity();
		Vector velocity = new Vector(jumpPadVelocity.getX(), jumpPadVelocity.getY(), jumpPadVelocity.getZ());
		player.setVelocity(velocity);
	}

	boolean isGoingDown(Location from, Location to) {
		return to.getY() < from.getY();
	}

	private void maybeTellPlayerToReadTheRules(Player player) {
		if (shouldReadRules(player)) {
			player.sendMessage("Please read the global rules (/rules) to get access to the jump pads.");
			mustReadRules(player, true);
		}
	}

	void playerCanJump(Player player, boolean canJump) {
		if (canJump){
			if (hasTemporaryJumpPause(player)) {
				this.playersWithTemporaryJumpPause.remove(player);
			}
		} else {
			if (!hasTemporaryJumpPause(player)) {
				this.playersWithTemporaryJumpPause.put(player, 1);
			}
		}
	}

	private void mustReadRules(Player player, boolean mustReadRules) {
		if (mustReadRules) {
			if (!shouldReadRules(player)) {
				this.playersThatHasBeenInformedToReadTheRules.put(player, 1);
			}
		} else {
			if (shouldReadRules(player)) {
				this.playersThatHasBeenInformedToReadTheRules.remove(player);
			}
		}
	}

	private boolean shouldReadRules(Player player) {
		return !this.playersThatHasBeenInformedToReadTheRules.hasInformation(player);
	}

	private boolean hasTemporaryJumpPause(Player player) {
		return this.playersWithTemporaryJumpPause.hasInformation(player);
	}

	private boolean hasReadRules(Player player) {
		return player.hasPermission("jumppad.jump");
	}
}
