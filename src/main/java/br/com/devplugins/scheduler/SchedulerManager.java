package br.com.devplugins.scheduler;

import br.com.devplugins.staging.StagingManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Set;

public class SchedulerManager {

    private final JavaPlugin plugin;
    private final StagingManager stagingManager;

    public SchedulerManager(JavaPlugin plugin, StagingManager stagingManager) {
        this.plugin = plugin;
        this.stagingManager = stagingManager;
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "scheduler.yml");
        if (!configFile.exists()) {
            plugin.saveResource("scheduler.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection schedules = config.getConfigurationSection("schedules");

        if (schedules != null) {
            Set<String> keys = schedules.getKeys(false);
            for (String key : keys) {
                ConfigurationSection section = schedules.getConfigurationSection(key);
                if (section == null)
                    continue;

                String command = section.getString("command");
                boolean requireApproval = section.getBoolean("require-approval", false);
                int intervalSeconds = section.getInt("interval-seconds", -1);

                if (intervalSeconds > 0) {
                    scheduleInterval(key, command, requireApproval, intervalSeconds);
                }

                // Cron support is not currently implemented.
                // Only interval-seconds is supported.
            }
        }
    }

    private void scheduleInterval(String id, String command, boolean requireApproval, int intervalSeconds) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (requireApproval) {
                stagingManager.stageCommand(Bukkit.getConsoleSender(), command);
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            }
        }, intervalSeconds * 20L, intervalSeconds * 20L);
    }
}
