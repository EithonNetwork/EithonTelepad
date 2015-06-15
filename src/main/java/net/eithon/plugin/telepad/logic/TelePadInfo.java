package net.eithon.plugin.telepad.logic;

import java.util.UUID;

import net.eithon.library.extensions.EithonLocation;
import net.eithon.library.extensions.EithonPlayer;
import net.eithon.library.json.Converter;
import net.eithon.library.json.IJson;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.json.simple.JSONObject;

public class TelePadInfo implements IJson<TelePadInfo> {
	private EithonLocation _sourceLocation;
	private EithonLocation _targetLocation;
	private Vector _velocity;
	private String _telePadName;
	private EithonPlayer _creator;
	private boolean _hasVelocity;

	private TelePadInfo(String name, Location sourceLocation, Player creator)
	{
		this._telePadName = name;
		this._sourceLocation = new EithonLocation(sourceLocation);
		if (creator != null)
		{
			this._creator = new EithonPlayer(creator);
		} else {
			this._creator = null;
		}
	}

	TelePadInfo(String name, Location sourceLocation, UUID creatorId, String creatorName)
	{
		this._telePadName = name;
		this._sourceLocation = new EithonLocation(sourceLocation);
		this._creator = new EithonPlayer(creatorId, creatorName);
	}

	public TelePadInfo(String name, Location sourceLocation, Location targetLocation, Player creator)
	{
		this(name, sourceLocation, creator);
		this._targetLocation = new EithonLocation(targetLocation);
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
		this._targetLocation = new EithonLocation(targetLocation);
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
	
	public boolean isJumpPad() { return hasVelocity(); }

	@Override
	public TelePadInfo factory() {
		return new TelePadInfo();
	}

	@Override
	public TelePadInfo fromJson(Object json) {
		JSONObject jsonObject = (JSONObject) json;
		this._telePadName = (String) jsonObject.get("name");
		this._sourceLocation = EithonLocation.getFromJson(jsonObject.get("sourceLocation"));
		this._hasVelocity = (boolean) jsonObject.get("hasVelocity");
		if (this._hasVelocity) {
			this._velocity = Converter.toVector((JSONObject)jsonObject.get("velocity"));
		} else {
			this._targetLocation = EithonLocation.getFromJson(jsonObject.get("targetLocation"));
		}
		this._creator = EithonPlayer.getFromJSon(jsonObject.get("creator"));
		return this;
	}
	
	public static TelePadInfo createFromJson(Object json) {
		TelePadInfo info = new TelePadInfo();
		return info.fromJson(json);
	}

	@SuppressWarnings("unchecked")
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("name", this._telePadName);
		json.put("sourceLocation", this._sourceLocation.toJson());
		json.put("hasVelocity", this._hasVelocity);
		if (this._hasVelocity) {
			json.put("velocity", Converter.fromVector(this._velocity));
		} else {
			json.put("targetLocation", this._targetLocation.toJson());
		}
		json.put("creator", this._creator.toJson());
		return json;
	}

	Location getTargetLocation() {
		return this._targetLocation.getLocation();
	}

	Vector getVelocity() {
		return this._velocity;
	}

	public void setTarget(Location location) {
		this._targetLocation = new EithonLocation(location);
		this._hasVelocity = false;
	}

	public void setVelocity(Vector velocity) {
		this._velocity = velocity;
		this._hasVelocity = true;;
	}

	String getTelePadName() {
		return this._telePadName;
	}

	Location getSource() {
		return this._sourceLocation.getLocation();
	}

	public Location getSourceAsTarget() {
		Location location = this._sourceLocation.getLocation().clone();
		location.setX(location.getX() + 0.5);
		location.setZ(location.getZ() + 0.5);
		return location;
	}

	String getBlockHash() {
		return TelePadInfo.toBlockHash(this._sourceLocation);
	}

	private static String toBlockHash(EithonLocation location)
	{
		if (location == null) return null;
		return toBlockHash(location.getLocation());
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
		return this._creator.getPlayer();
	}

	public String getPlayerName() {
		return this._creator.getName();
	}

	public String toString() {
		return String.format("%s (%s): from %s to %s", getTelePadName(), getPlayerName(), getSource().getBlock().toString(), getTargetLocation().toString());
	}
}
