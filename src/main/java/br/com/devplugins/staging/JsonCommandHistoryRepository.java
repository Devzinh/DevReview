package br.com.devplugins.staging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class JsonCommandHistoryRepository implements CommandHistoryRepository {

    private final JavaPlugin plugin;
    private final File storageFile;
    private final Gson gson;
    private final Map<UUID, List<StagedCommand>> cache;

    public JsonCommandHistoryRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "command_history.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cache = new ConcurrentHashMap<>();
    }

    private synchronized void loadFromDisk() {
        if (!storageFile.exists())
            return;

        try (Reader reader = new FileReader(storageFile)) {
            Type mapType = new TypeToken<HashMap<UUID, ArrayList<StagedCommand>>>() {
            }.getType();
            Map<UUID, List<StagedCommand>> loaded = gson.fromJson(reader, mapType);
            if (loaded != null) {
                cache.clear();
                cache.putAll(loaded);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load command history from JSON", e);
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
            plugin.getLogger().log(Level.SEVERE, "Failed to save command history to JSON", e);
        }
    }

    @Override
    public synchronized void save(UUID playerId, List<StagedCommand> history) {
        cache.put(playerId, new ArrayList<>(history));
        saveToDisk();
    }

    @Override
    public synchronized List<StagedCommand> load(UUID playerId) {
        if (cache.isEmpty()) {
            loadFromDisk();
        }
        return cache.getOrDefault(playerId, new ArrayList<>());
    }

    @Override
    public synchronized Map<UUID, List<StagedCommand>> loadAll() {
        loadFromDisk();
        Map<UUID, List<StagedCommand>> result = new HashMap<>();
        for (Map.Entry<UUID, List<StagedCommand>> entry : cache.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }

    @Override
    public synchronized void delete(UUID playerId) {
        cache.remove(playerId);
        saveToDisk();
    }
}
