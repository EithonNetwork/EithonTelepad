package net.eithon.plugin.telepad;

import net.eithon.library.extensions.EithonPlugin;
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
	
	private net.eithon.plugin.telepad.logic.Controller _controller;
	public EventListener(EithonPlugin eithonPlugin, Controller controller) {
		this._controller = controller;
	}

	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (event.isCancelled()) return;
		if (event.getAction() != Action.PHYSICAL) return;
		Player player = event.getPlayer();
		Block pressurePlate = event.getClickedBlock();
		if (pressurePlate == null) return;
		if (pressurePlate.getType() != Material.STONE_PLATE) return;
		this._controller.maybeTele(player, pressurePlate);
	}

	@EventHandler
	public void onPlayerInteractEvent(PlayerMoveEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		this._controller.maybeStopTele(player);
	}
}
