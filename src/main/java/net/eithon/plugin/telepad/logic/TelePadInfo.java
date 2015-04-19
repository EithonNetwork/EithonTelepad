package net.eithon.plugin.telepad.logic;

import java.util.UUID;

import net.eithon.library.core.IUuidAndName;
import net.eithon.library.json.Converter;
import net.eithon.library.json.IJson;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;

public class TelePadInfo implements IJson<TelePadInfo>, IUuidAndName {
	private Location sourceLocation;
	private Location targetLocation;
	private String telePadName;
	private UUID creatorId;
	private String creatorName;

	public TelePadInfo(String name, Location sourceLocation, Location targetLocation, Player creator)
	{
		this.telePadName = name;
		this.sourceLocation = sourceLocation;
		this.targetLocation = targetLocation;
		if (creator != null)
		{
			this.creatorId = creator.getUniqueId();
			this.creatorName = creator.getName();
		} else {
			this.creatorId = null;
			this.creatorName = null;
		}
	}

	TelePadInfo(String name, Location sourceLocation, Location targetLocation, UUID creatorId, String creatorName)
	{
		this.telePadName = name;
		this.sourceLocation = sourceLocation;
		this.targetLocation = targetLocation;
		this.creatorId = creatorId;
		this.creatorName = creatorName;
	}

	TelePadInfo() {
	}

	@Override
	public TelePadInfo factory() {
		return new TelePadInfo();
	}

	@Override
	public void fromJson(Object json) {
		JSONObject jsonObject = (JSONObject) json;
		this.telePadName = (String) jsonObject.get("name");
		this.sourceLocation = Converter.toLocation((JSONObject)jsonObject.get("sourceLocation"), null);
		this.targetLocation = Converter.toLocation((JSONObject)jsonObject.get("targetLocation"), null);
		this.creatorId = Converter.toPlayerId((JSONObject) jsonObject.get("creator"));
		this.creatorName= Converter.toPlayerName((JSONObject) jsonObject.get("creator"));
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("name", this.telePadName);
		json.put("sourceLocation", Converter.fromLocation(this.sourceLocation, true));
		json.put("targetLocation", Converter.fromLocation(this.targetLocation, true));
		json.put("creator", Converter.fromPlayer(this.creatorId, this.creatorName));
		return json;
	}

	Location getTargetLocation() {
		return this.targetLocation;
	}

	public void setTarget(Location location) {
		this.targetLocation = location;
	}

	String getTelePadName() {
		return this.telePadName;
	}

	Location getSource() {
		return this.sourceLocation;
	}

	public Location getSourceAsTarget() {
		Location location = this.sourceLocation.clone();
		location.setX(location.getX() + 0.5);
		location.setZ(location.getZ() + 0.5);
		return location;
	}

	String getBlockHash() {
		return TelePadInfo.toBlockHash(this.sourceLocation);
	}

	static String toBlockHash(Location location)
	{
		if (location == null) return null;
		return toBlockHash(location.getBlock());
	}

	static String toBlockHash(Block block)
	{
		return String.format("%d;%d;%d", block.getX(), block.getY(), block.getZ());
	}

	Player getCreator()
	{
		return Bukkit.getServer().getPlayer(this.creatorId);
	}

	public String getName() {
		return this.creatorName;
	}

	public UUID getUniqueId() {
		return this.creatorId;
	}

	public String toString() {
		return String.format("%s (%s): from %s toy %s", getTelePadName(), getName(), getSource().getBlock().toString(), getTargetLocation().toString());
	}
}
