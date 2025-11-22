package br.com.devplugins.staging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class StagingManager {

    private final JavaPlugin plugin;
    private final List<StagedCommand> pendingCommands;
    private final File storageFile;
    private final Gson gson;
    private final br.com.devplugins.lang.LanguageManager languageManager;

    public StagingManager(JavaPlugin plugin, br.com.devplugins.lang.LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        this.pendingCommands = new ArrayList<>();
        this.storageFile = new File(plugin.getDataFolder(), "staged_commands.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadCommands();
    }

    public void stageCommand(Player sender, String commandLine) {
        StagedCommand command = new StagedCommand(sender.getUniqueId(), sender.getName(), commandLine);
        pendingCommands.add(command);
        saveCommands();

        String msg = languageManager.getMessage(sender, "messages.command-staged");
        sender.sendMessage(msg.replace("%command%", commandLine));

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("devreview.admin"))
                .forEach(p -> {
                    String notify = languageManager.getMessage(p, "messages.staging-notification");
                    p.sendMessage(notify.replace("%player%", sender.getName()).replace("%command%", commandLine));
                });
    }

    public void approveCommand(StagedCommand command) {
        if (!pendingCommands.contains(command))
            return;

        Player sender = Bukkit.getPlayer(command.getSenderId());
        if (sender != null && sender.isOnline()) {
            String cmdToRun = command.getCommandLine();
            if (cmdToRun.startsWith("/")) {
                cmdToRun = cmdToRun.substring(1);
            }
            Bukkit.dispatchCommand(sender, cmdToRun);
            plugin.getLogger().info(
                    "Executed staged command for online player " + sender.getName() + ": " + command.getCommandLine());
        } else {
            plugin.getLogger().warning("Executing staged command for OFFLINE player " + command.getSenderName() + ": "
                    + command.getCommandLine());

            String cmdToRun = command.getCommandLine();
            if (cmdToRun.startsWith("/")) {
                cmdToRun = cmdToRun.substring(1);
            }
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdToRun);
        }

        pendingCommands.remove(command);
        saveCommands();
    }

    public void rejectCommand(StagedCommand command) {
        pendingCommands.remove(command);
        saveCommands();
    }

    public List<StagedCommand> getPendingCommands() {
        return new ArrayList<>(pendingCommands);
    }

    private void loadCommands() {
        if (!storageFile.exists())
            return;

        try (Reader reader = new FileReader(storageFile)) {
            Type listType = new TypeToken<ArrayList<StagedCommand>>() {
            }.getType();
            List<StagedCommand> loaded = gson.fromJson(reader, listType);
            if (loaded != null) {
                pendingCommands.addAll(loaded);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load staged commands", e);
        }
    }

    public void saveCommands() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        try (Writer writer = new FileWriter(storageFile)) {
            gson.toJson(pendingCommands, writer);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save staged commands", e);
        }
    }
}
