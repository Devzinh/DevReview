package br.com.devplugins.staging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;

public class JsonStagedCommandRepository implements StagedCommandRepository {

    private final JavaPlugin plugin;
    private final File storageFile;
    private final Gson gson;
    private final List<StagedCommand> cache;
    private final ReadWriteLock lock;

    public JsonStagedCommandRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.storageFile = new File(plugin.getDataFolder(), "staged_commands.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.cache = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
        loadFromDisk();
    }

    private void loadFromDisk() {
        if (!storageFile.exists())
            return;

        lock.writeLock().lock();
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
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void saveToDisk() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        lock.readLock().lock();
        try (Writer writer = new FileWriter(storageFile)) {
            gson.toJson(cache, writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save staged commands to JSON", e);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void save(StagedCommand command) {
        lock.writeLock().lock();
        try {
            // If it exists, update it (remove old, add new)
            cache.removeIf(c -> c.getId().equals(command.getId()));
            cache.add(command);
        } finally {
            lock.writeLock().unlock();
        }
        saveToDisk();
    }

    @Override
    public void delete(StagedCommand command) {
        lock.writeLock().lock();
        try {
            cache.removeIf(c -> c.getId().equals(command.getId()));
        } finally {
            lock.writeLock().unlock();
        }
        saveToDisk();
    }

    @Override
    public List<StagedCommand> loadAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(cache);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void saveAll(List<StagedCommand> commands) {
        if (commands.isEmpty()) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            for (StagedCommand command : commands) {
                // If it exists, update it (remove old, add new)
                cache.removeIf(c -> c.getId().equals(command.getId()));
                cache.add(command);
            }
        } finally {
            lock.writeLock().unlock();
        }
        // Single disk write for all commands
        saveToDisk();
    }
    
    @Override
    public void deleteAll(List<StagedCommand> commands) {
        if (commands.isEmpty()) {
            return;
        }
        
        lock.writeLock().lock();
        try {
            for (StagedCommand command : commands) {
                cache.removeIf(c -> c.getId().equals(command.getId()));
            }
        } finally {
            lock.writeLock().unlock();
        }
        // Single disk write for all deletions
        saveToDisk();
    }
}
