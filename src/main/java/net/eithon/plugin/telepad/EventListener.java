package net.eithon.plugin.telepad;

import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.plugin.Logger.DebugPrintLevel;
import net.eithon.plugin.telepad.logic.Controller;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public final class EventListener implements Listener {
	
	private Controller _controller;
	private EithonPlugin _eithonPlugin;
	public EventListener(EithonPlugin eithonPlugin, Controller controller) {
		this._controller = controller;
		this._eithonPlugin = eithonPlugin;
	}

	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		debug("onPlayerInteractEvent", "Enter");
		if (event.isCancelled()) {
			debug("onPlayerInteractEvent", "Event was already cancelled");
			return;
		}
		if (event.getAction() != Action.PHYSICAL) {
			debug("onPlayerInteractEvent", "Event was not Action.PHYSICAL");
			return;
		}
		Player player = event.getPlayer();
		Block pressurePlate = event.getClickedBlock();
		if (pressurePlate == null) {
			debug("onPlayerInteractEvent", "Not a clicked block");
			return;
		}
		if (pressurePlate.getType() != Material.STONE_PLATE) {
			debug("onPlayerInteractEvent", "Not a STONE_PLATE");
			return;
		}
		debug("onPlayerInteractEvent", "We are ready for teleport");
		this._controller.maybeTele(player, pressurePlate);
	}

	@EventHandler
	public void onPlayerInteractEvent(PlayerMoveEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		this._controller.maybeStopTele(player);
	}

	private void debug(String method, String message) {
		this._eithonPlugin.getEithonLogger().debug(DebugPrintLevel.VERBOSE, "%s: %s", method, message);
	}
}
