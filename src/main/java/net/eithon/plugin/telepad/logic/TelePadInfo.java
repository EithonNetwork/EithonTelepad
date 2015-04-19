package net.eithon.plugin.telepad.logic;

import java.util.UUID;

import net.eithon.library.core.IUuidAndName;
import net.eithon.library.json.Converter;
import net.eithon.library.json.IJson;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.json.simple.JSONObject;

public class TelePadInfo implements IJson<TelePadInfo>, IUuidAndName {
	private Location _sourceLocation;
	private Location _targetLocation;
	private Vector _velocity;
	private String _telePadName;
	private UUID _creatorId;
	private String _creatorName;
	private boolean _hasVelocity;

	private TelePadInfo(String name, Location sourceLocation, Player creator)
	{
		this._telePadName = name;
		this._sourceLocation = sourceLocation;
		if (creator != null)
		{
			this._creatorId = creator.getUniqueId();
			this._creatorName = creator.getName();
		} else {
			this._creatorId = null;
			this._creatorName = null;
		}
	}

	TelePadInfo(String name, Location sourceLocation, UUID creatorId, String creatorName)
	{
		this._telePadName = name;
		this._sourceLocation = sourceLocation;
		this._creatorId = creatorId;
		this._creatorName = creatorName;
	}

	public TelePadInfo(String name, Location sourceLocation, Location targetLocation, Player creator)
	{
		this(name, sourceLocation, creator);
		this._targetLocation = targetLocation;
		this._hasVelocity = false;
	}

	public TelePadInfo(String name, Location sourceLocation, Vector velocity, Player creator)
	{
		this(name, sourceLocation, creator);
		this._velocity = velocity;
		this._hasVelocity = true;
	}

	TelePadInfo(String name, Location sourceLocation, Location targetLocation, UUID creatorId, String creatorName)
	{
		this(name, sourceLocation, creatorId, creatorName);
		this._targetLocation = targetLocation;
		this._hasVelocity = false;
	}

	TelePadInfo(String name, Location sourceLocation, Vector velocity, UUID creatorId, String creatorName)
	{
		this(name, sourceLocation, creatorId, creatorName);
		this._velocity = velocity;
		this._hasVelocity = true;
	}

	TelePadInfo() {
	}
	
	public boolean hasVelocity() { return this._hasVelocity; }

	@Override
	public TelePadInfo factory() {
		return new TelePadInfo();
	}

	@Override
	public void fromJson(Object json) {
		JSONObject jsonObject = (JSONObject) json;
		this._telePadName = (String) jsonObject.get("name");
		this._sourceLocation = Converter.toLocation((JSONObject)jsonObject.get("sourceLocation"), null);
		this._hasVelocity = (boolean) jsonObject.get("hasVelocity");
		if (this._hasVelocity) {
			this._velocity = Converter.toVector((JSONObject)jsonObject.get("velocity"));
		} else {
			this._targetLocation = Converter.toLocation((JSONObject)jsonObject.get("targetLocation"), null);
		}
		this._creatorId = Converter.toPlayerId((JSONObject) jsonObject.get("creator"));
		this._creatorName= Converter.toPlayerName((JSONObject) jsonObject.get("creator"));
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("name", this._telePadName);
		json.put("sourceLocation", Converter.fromLocation(this._sourceLocation, true));
		json.put("hasVelocity", this._hasVelocity);
		if (this._hasVelocity) {
			json.put("velocity", Converter.fromVector(this._velocity));
		} else {
			json.put("targetLocation", Converter.fromLocation(this._targetLocation, true));
		}
		json.put("creator", Converter.fromPlayer(this._creatorId, this._creatorName));
		return json;
	}

	Location getTargetLocation() {
		return this._targetLocation;
	}

	Vector getVelocity() {
		return this._velocity;
	}

	public void setTarget(Location location) {
		this._targetLocation = location;
	}

	public void setVelocity(Vector velocity) {
		this._velocity = velocity;
	}

	String getTelePadName() {
		return this._telePadName;
	}

	Location getSource() {
		return this._sourceLocation;
	}

	public Location getSourceAsTarget() {
		Location location = this._sourceLocation.clone();
		location.setX(location.getX() + 0.5);
		location.setZ(location.getZ() + 0.5);
		return location;
	}

	String getBlockHash() {
		return TelePadInfo.toBlockHash(this._sourceLocation);
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
		return Bukkit.getServer().getPlayer(this._creatorId);
	}

	public String getName() {
		return this._creatorName;
	}

	public UUID getUniqueId() {
		return this._creatorId;
	}

	public String toString() {
		return String.format("%s (%s): from %s toy %s", getTelePadName(), getName(), getSource().getBlock().toString(), getTargetLocation().toString());
	}
}
