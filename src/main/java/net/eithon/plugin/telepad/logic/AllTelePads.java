package net.eithon.plugin.telepad.logic;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.json.FileContent;
import net.eithon.library.plugin.Logger.DebugPrintLevel;
import net.eithon.library.time.TimeMisc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class AllTelePads {

	private HashMap<String, TelePadInfo> telePadsByBlock = null;
	private HashMap<String, TelePadInfo> telePadsByName = null;

	private EithonPlugin _eithonPlugin;

	public AllTelePads(EithonPlugin eithonPlugin) {
		this._eithonPlugin = eithonPlugin;
		this.telePadsByBlock = new HashMap<String, TelePadInfo>();
		this.telePadsByName = new HashMap<String, TelePadInfo>();
	}
	
	public void add(TelePadInfo info) {
		this.telePadsByName.put(info.getTelePadName(), info);
		this.telePadsByBlock.put(info.getBlockHash(), info);
	}

	public void remove(TelePadInfo info) {
		this.telePadsByName.remove(info.getTelePadName());
		this.telePadsByBlock.remove(info.getBlockHash());
	}

	public Collection<TelePadInfo> getAll() {
		return this.telePadsByName.values();
	}

	TelePadInfo getByLocation(Location location) {
		debug("AllTelePads.getByLocation", "Enter");
		if (this.telePadsByBlock == null) {
			debug("AllTelePads.getByLocation", "telePadsByBlock == null");
			return null;
		}
		String position = TelePadInfo.toBlockHash(location);
		if (!this.telePadsByBlock.containsKey(position)) {
			debug("AllTelePads.getByLocation", "No telepads at position " + position);
			for (TelePadInfo info : this.telePadsByBlock.values()) {
				debug("AllTelePads.getByLocation", String.format("Telepad: %s", info.toString()));
			}
			return null;
		}
		TelePadInfo info = this.telePadsByBlock.get(position);
		debug("AllTelePads.getByLocation", String.format("Found a telepad: %s.", info.toString()));
		return info;
	}

	public TelePadInfo getByName(String name) {
		if (!this.telePadsByName.containsKey(name)) return null;
		return this.telePadsByName.get(name);
	}

	public void delayedSave(JavaPlugin plugin, double seconds)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				save();
			}
		}, TimeMisc.secondsToTicks(seconds));		
	}

	public void delayedLoad(JavaPlugin plugin, double seconds)
	{
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				load();
			}
		}, TimeMisc.secondsToTicks(seconds));		
	}

	@SuppressWarnings("unchecked")
	public
	void save() {
		JSONArray telePads = new JSONArray();
		for (TelePadInfo telePadInfo : getAll()) {
			telePads.add(telePadInfo.toJson());
		}
		if ((telePads == null) || (telePads.size() == 0)) {
			this._eithonPlugin.getEithonLogger().info("No TelePads saved.");
			return;
		}
		this._eithonPlugin.getEithonLogger().info("Saving %d TelePads", telePads.size());
		File file = getTelepadStorageFile();
		
		FileContent fileContent = new FileContent("TelePad", 1, telePads);
		fileContent.save(file);
	}

	private File getTelepadStorageFile() {
		File file = this._eithonPlugin.getDataFile("telepads.json");
		return file;
	}

	void load() {
		File file = getTelepadStorageFile();
		FileContent fileContent = FileContent.loadFromFile(file);
		if (fileContent == null) {
			this._eithonPlugin.getEithonLogger().debug(DebugPrintLevel.MAJOR, "File was empty.");
			return;			
		}
		JSONArray array = (JSONArray) fileContent.getPayload();
		if ((array == null) || (array.size() == 0)) {
			this._eithonPlugin.getEithonLogger().debug(DebugPrintLevel.MAJOR, "The list of TelePads was empty.");
			return;
		}
		this._eithonPlugin.getEithonLogger().info("Restoring %d TelePads from loaded file.", array.size());
		this.telePadsByBlock = new HashMap<String, TelePadInfo>();
		this.telePadsByName = new HashMap<String, TelePadInfo>();
		for (int i = 0; i < array.size(); i++) {
			TelePadInfo info = new TelePadInfo();
			info.fromJson((JSONObject) array.get(i));
			this.add(info);
		}
	}

	void debug(String method, String message) {
		this._eithonPlugin.getEithonLogger().debug(DebugPrintLevel.VERBOSE, "%s: %s", method, message);
	}
}
