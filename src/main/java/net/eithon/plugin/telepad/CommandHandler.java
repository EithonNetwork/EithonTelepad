package net.eithon.plugin.telepad;

import net.eithon.library.extensions.EithonLocation;
import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.plugin.CommandParser;
import net.eithon.library.plugin.ConfigurableMessage;
import net.eithon.library.plugin.Configuration;
import net.eithon.library.plugin.ICommandHandler;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements ICommandHandler {
	private static final String ADD_COMMAND = "/telepad add <name>";
	private static final String GOTO_COMMAND = "/telepad goto <name>";
	private static final String LIST_COMMAND = "/telepad list";
	private static final String REMOVE_COMMAND = "/telepad remove <name>";
	private static final String LINK_COMMAND = "/telepad link <name 1> <name 2>";
	private static final String RULES_COMMAND_BEGINNING = "/rules";

	private static ConfigurableMessage telePadAddedMessage;	
	private static ConfigurableMessage nextStepAfterAddMessage;	
	private static ConfigurableMessage telePadRemovedMessage;
	private static ConfigurableMessage telePadsLinkedMessage;
	private static ConfigurableMessage gotoTelePadMessage;

	private EithonPlugin _eithonPlugin = null;
	private AllTelePads _allTelePads = null;
	private Controller _controller;

	public CommandHandler(EithonPlugin eithonPlugin, Controller controller) {
		this._controller = controller;
		Configuration config = eithonPlugin.getConfiguration();
		double seconds = config.getDouble("SecondsBeforeLoad", 5.0);
		this._allTelePads.delayedLoad(eithonPlugin, seconds);
		telePadAddedMessage = config.getConfigurableMessage("TelePadAdded", 1,
				"TelePad %s has been added.");
		nextStepAfterAddMessage = config.getConfigurableMessage("NextStepAfterAdd", 1,
				"Now link this telepad with command /telepad link %s <other telepad name>.");
		telePadRemovedMessage = config.getConfigurableMessage("TelePadRemoved", 1,
				"TelePad %s has been removed.");
		telePadsLinkedMessage = config.getConfigurableMessage("TelePadsLinked", 2,
				"TelePad %s and %s has been linked.");
		gotoTelePadMessage = config.getConfigurableMessage("GotoTelepad", 1,
				"You have been teleported to TelePad %s.");
	}

	void disable() {
		this._allTelePads.save();
	}

	@Override
	public boolean onCommand(CommandParser commandParser) {
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(1)) return true;
		Player player = commandParser.getPlayerOrInformSender();
		if (player == null) return true;

		String command = commandParser.getArgumentStringAsLowercase(0);
		if (command.equals("add")) {
			addCommand(commandParser);
		} else if (command.equals("link")) {
			linkCommand(commandParser);
		} else if (command.equals("remove")) {
			removeCommand(commandParser);
		} else if (command.equals("list")) {
			listCommand(commandParser);
		} else if (command.equals("goto")) {
			gotoCommand(commandParser);
		} else {
			commandParser.showCommandSyntax();
		}
		return true;
	}

	void addCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.add")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(2, 2)) return;

		String name =commandParser.getArgumentStringAsLowercase(1);
		Player player = commandParser.getPlayer();
		if (!verifyNameIsNew(player, name)) return;	

		double upSpeed = 0.0;
		double forwardSpeed = 0.0;

		createOrUpdateTelePad(player, name, upSpeed, forwardSpeed);
		CommandHandler.telePadAddedMessage.sendMessage(player, name);
		CommandHandler.nextStepAfterAddMessage.sendMessage(player, name);
		this._allTelePads.delayedSave(this._eithonPlugin, 0.0);
	}

	void removeCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.remove")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(2, 2)) return;

		Player player = commandParser.getPlayer();
		String name =commandParser.getArgumentStringAsLowercase(1);
		TelePadInfo info = this._allTelePads.getByName(name);
		if (info == null)
		{
			player.sendMessage("Unknown telepad: " + name);
			return;	
		}
		this._allTelePads.remove(info);
		CommandHandler.telePadRemovedMessage.sendMessage(player, name);
		this._allTelePads.delayedSave(this._eithonPlugin, 0.0);
	}

	void linkCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.link")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(3, 3)) return;

		Player player = commandParser.getPlayer();
		String name1 = commandParser.getArgumentStringAsLowercase(1);
		TelePadInfo info1 = this._allTelePads.getByName(name1);
		if (info1 == null)
		{
			player.sendMessage("Unknown telepad: " + name1);
			return;	
		}
		String name2 = commandParser.getArgumentStringAsLowercase(2);
		TelePadInfo info2 = this._allTelePads.getByName(name2);
		if (info2 == null)
		{
			player.sendMessage("Unknown telepad: " + name2);
			return;	
		}

		info1.setTarget(info2.getSourceAsTarget());
		info2.setTarget(info1.getSourceAsTarget());
		CommandHandler.telePadsLinkedMessage.sendMessage(player, name1, name2);
		this._allTelePads.delayedSave(this._eithonPlugin, 0.0);
	}

	void gotoCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.goto")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(2, 2)) return;

		Player player = commandParser.getPlayer();
		String name =commandParser.getArgumentStringAsLowercase(1);
		TelePadInfo info = this._allTelePads.getByName(name);
		if (info == null)
		{
			player.sendMessage("Unknown telepad: " + name);
			return;			
		}
		player.teleport(info.getSourceAsTarget());
		this._controller.coolDown(player);
		CommandHandler.gotoTelePadMessage.sendMessage(player, name);
	}

	void listCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.list")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(1, 1)) return;

		Player player = commandParser.getPlayer();

		player.sendMessage("Tele pads:");
		for (TelePadInfo info : this._allTelePads.getAll()) {
			player.sendMessage(info.toString());
		}
	}

	private boolean verifyNameIsNew(Player player, String name) {
		TelePadInfo info = this._allTelePads.getByName(name);
		if (info != null)
		{
			player.sendMessage("Telepad already exists: " + name);
			return true;		
		}
		return true;
	}

	private void createOrUpdateTelePad(Player player, String name, double upSpeed, double forwardSpeed) {
		EithonLocation eithonLocation = new EithonLocation(player.getLocation());
		Block pressurePlate = eithonLocation.searchForFirstBlockOfMaterial(Material.STONE_PLATE, 3);
		if (pressurePlate == null) {
			player.sendMessage("No stone plate within 3 blocks.");
			return;
		}

		Location playerLocation = player.getLocation();
		Location padLocation = pressurePlate.getLocation();
		// Remember where the player looked when the telepad was created/updated
		padLocation.setYaw(playerLocation.getYaw());
		padLocation.setPitch(playerLocation.getPitch());
		try {
			TelePadInfo newInfo = new TelePadInfo(name, padLocation, padLocation, player);
			this._allTelePads.add(newInfo);
			if (player != null) this._controller.coolDown(player);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	void listenToCommands(Player player, String message) {
		if (message.toLowerCase().startsWith(RULES_COMMAND_BEGINNING))
		{
			player.sendMessage("Getting permission");
			player.addAttachment(this._eithonPlugin, "telepad.jump", true);
		}
	}

	@Override
	public void showCommandSyntax(CommandSender sender, String command) {

		if (command.equals("add")) {
			sender.sendMessage(ADD_COMMAND);
		} else if (command.equals("link")) {
			sender.sendMessage(LINK_COMMAND);
		} else if (command.equals("remove")) {
			sender.sendMessage(REMOVE_COMMAND);
		} else if (command.equals("list")) {
			sender.sendMessage(LIST_COMMAND);
		} else if (command.equals("goto")) {
			sender.sendMessage(GOTO_COMMAND);
		} else {
			sender.sendMessage(String.format("Unknown command: %s.", command));
		}	
	}
}
