package br.com.devplugins.staging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class JsonStagedCommandRepository implements StagedCommandRepository {

    private final JavaPlugin plugin;
    private final File storageFile;
    private final Gson gson;
    private final List<StagedCommand> cache;

    public JsonStagedCommandRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "staged_commands.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cache = java.util.Collections.synchronizedList(new ArrayList<>());
        loadFromDisk();
    }

    private synchronized void loadFromDisk() {
        if (!storageFile.exists())
            return;

        try (Reader reader = new FileReader(storageFile)) {
            Type listType = new TypeToken<ArrayList<StagedCommand>>() {
            }.getType();
            List<StagedCommand> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                cache.clear();
                cache.addAll(loaded);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load staged commands from JSON", e);
        }
    }

    private synchronized void saveToDisk() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        try (Writer writer = new FileWriter(storageFile)) {
            synchronized (cache) {
                gson.toJson(cache, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save staged commands to JSON", e);
        }
    }

    @Override
    public void save(StagedCommand command) {
        // If it exists, update it (remove old, add new)
        cache.removeIf(c -> c.getId().equals(command.getId()));
        cache.add(command);
        saveToDisk();
    }

    @Override
    public void delete(StagedCommand command) {
        cache.removeIf(c -> c.getId().equals(command.getId()));
        saveToDisk();
    }

    @Override
    public List<StagedCommand> loadAll() {
        synchronized (cache) {
            return new ArrayList<>(cache);
        }
    }
}
