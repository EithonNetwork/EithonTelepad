package net.eithon.plugin.telepad;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;

import net.eithon.library.extensions.EithonPlugin;
import net.eithon.library.json.Converter;
import net.eithon.library.plugin.Logger.DebugPrintLevel;
import net.eithon.library.time.TimeMisc;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class AllTelePads {
	private static AllTelePads singleton = null;

	private HashMap<String, TelePadInfo> telePadsByBlock = null;
	private HashMap<String, TelePadInfo> telePadsByName = null;

	private EithonPlugin _eithonPlugin;

	private AllTelePads(EithonPlugin eithonPlugin) {
		this._eithonPlugin = eithonPlugin;
		this.telePadsByBlock = new HashMap<String, TelePadInfo>();
		this.telePadsByName = new HashMap<String, TelePadInfo>();
	}

	static AllTelePads get(EithonPlugin eithonPlugin) {
		if (singleton == null) {
			singleton = new AllTelePads(eithonPlugin);
		}
		return singleton;
	}

	void add(TelePadInfo info) {
		this.telePadsByBlock.put(info.getBlockHash(), info);
		this.telePadsByName.put(info.getTelePadName(), info);
	}

	void remove(TelePadInfo info) {
		this.telePadsByName.remove(info.getTelePadName());
		this.telePadsByBlock.remove(info.getBlockHash());
	}

	Collection<TelePadInfo> getAll() {
		return this.telePadsByName.values();
	}

	TelePadInfo getByLocation(Location location) {
		if (this.telePadsByBlock == null) return null;
		String position = TelePadInfo.toBlockHash(location);
		if (!this.telePadsByBlock.containsKey(position)) return null;
		return this.telePadsByBlock.get(position);
	}

	TelePadInfo getByName(String name) {
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
		Converter.save(file, Converter.fromBody("TelePad", 1, (Object) telePads));
	}

	private File getTelepadStorageFile() {
		File file = this._eithonPlugin.getDataFile("telepads.json");
		return file;
	}

	void load() {
		File file = getTelepadStorageFile();
		JSONObject data = Converter.load(this._eithonPlugin, file);
		if (data == null) {
			this._eithonPlugin.getEithonLogger().debug(DebugPrintLevel.MAJOR, "File was empty.");
			return;			
		}
		JSONArray array = (JSONArray) Converter.toBodyPayload(data);
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
}
