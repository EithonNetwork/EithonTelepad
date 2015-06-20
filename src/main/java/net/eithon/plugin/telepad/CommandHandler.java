package net.eithon.plugin.telepad;

import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.plugin.CommandParser;
import net.eithon.library.plugin.ICommandHandler;
import net.eithon.plugin.telepad.logic.Controller;
import net.eithon.plugin.telepad.logic.TelePadInfo;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements ICommandHandler {
	private static final String ADD_COMMAND = "/telepad add <name> [<up speed> <forward speed>]";
	private static final String VELOCITY_COMMAND = "/telepad velocity <name> <up speed> <forward speed>";
	private static final String GOTO_COMMAND = "/telepad goto <name>";
	private static final String LIST_COMMAND = "/telepad list";
	private static final String REMOVE_COMMAND = "/telepad remove <name>";
	private static final String LINK_COMMAND = "/telepad link <name 1> <name 2>";
	private static final String RULES_COMMAND_BEGINNING = "/rules";

	private EithonPlugin _eithonPlugin = null;
	private Controller _controller;

	public CommandHandler(EithonPlugin eithonPlugin, Controller controller) {
		this._controller = controller;
		this._eithonPlugin = eithonPlugin;
	}

	void disable() {
		this._controller.save();
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
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(2, 4)) return;

		String name =commandParser.getArgumentStringAsLowercase();
		Player player = commandParser.getPlayer();
		if (!this._controller.verifyNameIsNew(player, name)) return;

		double upSpeed = commandParser.getArgumentDouble(0.0);
		double forwardSpeed = commandParser.getArgumentDouble(0.0);

		boolean success = this._controller.createOrUpdateTelePad(player, name, upSpeed, forwardSpeed);
		if (success) {
			Config.M.telePadAdded.sendMessage(player, name);
			Config.M.nextStepAfterAdd.sendMessage(player, name);
		}
	}



	void velocityCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.velocity")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(3, 4)) return;

		String name =commandParser.getArgumentStringAsLowercase();
		Player player = commandParser.getPlayer();
		TelePadInfo info = this._controller.getByNameOrInformUser(player, name);
		if (info == null) return;

		double upSpeed = commandParser.getArgumentDouble(0.0);
		double forwardSpeed = commandParser.getArgumentDouble(0.0);

		this._controller.createOrUpdateTelePad(player, name, upSpeed, forwardSpeed);
	}

	void removeCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.remove")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(2, 2)) return;

		Player player = commandParser.getPlayer();
		String name =commandParser.getArgumentStringAsLowercase();
		TelePadInfo info = this._controller.getByNameOrInformUser(player, name);
		if (info == null) return;
		
		this._controller.remove(info);
		Config.M.telePadRemoved.sendMessage(player, name);
	}

	void linkCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.link")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(3, 3)) return;

		Player player = commandParser.getPlayer();
		String name1 = commandParser.getArgumentStringAsLowercase();
		TelePadInfo info1 = this._controller.getByNameOrInformUser(player, name1);
		if (info1 == null) return;
		String name2 = commandParser.getArgumentStringAsLowercase();
		TelePadInfo info2 = this._controller.getByNameOrInformUser(player, name2);
		if (info2 == null) return;

		this._controller.link(info1, info2);
		Config.M.telePadsLinked.sendMessage(player, name1, name2);
	}

	void gotoCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.goto")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(2, 2)) return;

		Player player = commandParser.getPlayer();
		String name =commandParser.getArgumentStringAsLowercase();
		TelePadInfo info = this._controller.getByNameOrInformUser(player, name);
		if (info == null) return;

		this._controller.gotoTelepad(player, info);
		Config.M.gotoTelePad.sendMessage(player, name);
	}

	void listCommand(CommandParser commandParser)
	{
		if (!commandParser.hasPermissionOrInformSender("telepad.list")) return;
		if (!commandParser.hasCorrectNumberOfArgumentsOrShowSyntax(1, 1)) return;

		Player player = commandParser.getPlayer();

		player.sendMessage("TelePads:");
		this._controller.listTelepads(player);
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
