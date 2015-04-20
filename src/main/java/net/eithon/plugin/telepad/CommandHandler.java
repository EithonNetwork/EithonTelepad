package net.eithon.plugin.telepad;

import net.eithon.library.extensions.EithonLocation;
import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.plugin.CommandParser;
import net.eithon.library.plugin.ICommandHandler;
import net.eithon.plugin.telepad.logic.AllTelePads;
import net.eithon.plugin.telepad.logic.Controller;
import net.eithon.plugin.telepad.logic.TelePadInfo;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class CommandHandler implements ICommandHandler {
	private static final String ADD_COMMAND = "/telepad add <name>";
	private static final String VELOCITY_COMMAND = "/telepad velocity <name> <up speed> <forward speed>";
	private static final String GOTO_COMMAND = "/telepad goto <name>";
	private static final String LIST_COMMAND = "/telepad list";
	private static final String REMOVE_COMMAND = "/telepad remove <name>";
	private static final String LINK_COMMAND = "/telepad link <name 1> <name 2>";
	private static final String RULES_COMMAND_BEGINNING = "/rules";

	private EithonPlugin _eithonPlugin = null;
	private AllTelePads _allTelePads = null;
	private Controller _controller;

	public CommandHandler(EithonPlugin eithonPlugin, Controller controller) {
		this._controller = controller;
		this._allTelePads = new AllTelePads(eithonPlugin);
	}

	void disable() {
		this._allTelePads.save();
	}

	@Override
	public boolean onCommand(CommandParser commandParser) {
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(1)) return true;
		Player player = commandParser.getPlayerOrInformSender();
		if (player == null) return true;

		String command = commandParser.getArgumentCommand();
		if (command.equals("add")) {
			addCommand(commandParser);
		} else if (command.equals("link")) {
			linkCommand(commandParser);
		} else if (command.equals("velocity")) {
			velocityCommand(commandParser);
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

		String name =commandParser.getArgumentStringAsLowercase();
		Player player = commandParser.getPlayer();
		if (!verifyNameIsNew(player, name)) return;	

		double upSpeed = 0.0;
		double forwardSpeed = 0.0;

		createOrUpdateTelePad(player, name, upSpeed, forwardSpeed);
		Config.M.telePadAdded.sendMessage(player, name);
		Config.M.nextStepAfterAdd.sendMessage(player, name);
		this._allTelePads.delayedSave(this._eithonPlugin, 0.0);
	}



	void velocityCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.velocity")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(3, 4)) return;

		String name =commandParser.getArgumentStringAsLowercase();
		Player player = commandParser.getPlayer();
		TelePadInfo info = getByNameOrInformUser(player, name);
		if (info == null) return;

		double upSpeed = commandParser.getArgumentDouble(0.0);
		double forwardSpeed = commandParser.getArgumentDouble(0.0);

		createOrUpdateTelePad(player, name, upSpeed, forwardSpeed);
	}

	private TelePadInfo getByNameOrInformUser(CommandSender sender, String name) {
		TelePadInfo info = this._allTelePads.getByName(name);
		if (info != null) return info;
		Config.M.unknownTelePad.sendMessage(sender, name);
		return null;
	}

	void removeCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.remove")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(2, 2)) return;

		Player player = commandParser.getPlayer();
		String name =commandParser.getArgumentStringAsLowercase();
		TelePadInfo info = getByNameOrInformUser(player, name);
		if (info == null) return;

		this._allTelePads.remove(info);
		Config.M.telePadRemoved.sendMessage(player, name);
		this._allTelePads.delayedSave(this._eithonPlugin, 0.0);
	}

	void linkCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.link")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(3, 3)) return;

		Player player = commandParser.getPlayer();
		String name1 = commandParser.getArgumentStringAsLowercase();
		TelePadInfo info1 = getByNameOrInformUser(player, name1);
		if (info1 == null) return;
		String name2 = commandParser.getArgumentStringAsLowercase();
		TelePadInfo info2 = getByNameOrInformUser(player, name2);
		if (info2 == null) return;

		info1.setTarget(info2.getSourceAsTarget());
		info2.setTarget(info1.getSourceAsTarget());
		Config.M.telePadsLinked.sendMessage(player, name1, name2);
		this._allTelePads.delayedSave(this._eithonPlugin, 0.0);
	}

	void gotoCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.goto")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(2, 2)) return;

		Player player = commandParser.getPlayer();
		String name =commandParser.getArgumentStringAsLowercase();
		TelePadInfo info = getByNameOrInformUser(player, name);
		if (info == null) return;

		player.teleport(info.getSourceAsTarget());
		this._controller.coolDown(player);
		Config.M.gotoTelePad.sendMessage(player, name);
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

		TelePadInfo telePadInfo = this._allTelePads.getByName(name);
		if ((upSpeed != 0.0) || (forwardSpeed != 0.0)) {
			Vector velocity;
			velocity = convertToVelocityVector(playerLocation.getYaw(), upSpeed, forwardSpeed);
			if (telePadInfo != null) telePadInfo.setVelocity(velocity);
			else telePadInfo = new TelePadInfo(name, padLocation, velocity, player);
		} else {
			if (telePadInfo != null) telePadInfo.setTarget(padLocation);
			else telePadInfo = new TelePadInfo(name, padLocation, padLocation, player);
		}
		this._allTelePads.add(telePadInfo);
		if (player != null) this._controller.coolDown(player);
	}

	private Vector convertToVelocityVector(double yaw, double upSpeed, double forwardSpeed) {
		double rad = yaw*Math.PI/180.0;
		double vectorX = -Math.sin(rad)*forwardSpeed;
		double vectorY = upSpeed;
		double vectorZ = Math.cos(rad)*forwardSpeed;
		Vector jumpVector = new Vector(vectorX, vectorY, vectorZ);
		return jumpVector;
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
		} else if (command.equals("velocity")) {
			sender.sendMessage(VELOCITY_COMMAND);
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
