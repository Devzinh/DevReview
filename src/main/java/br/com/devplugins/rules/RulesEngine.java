package br.com.devplugins.rules;

import br.com.devplugins.staging.StagedCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class RulesEngine {

    private final JavaPlugin plugin;
    private boolean autoApproveEnabled;
    private LocalTime autoApproveStart;
    private LocalTime autoApproveEnd;
    private boolean expirationEnabled;
    private long expirationMillis;

    public RulesEngine(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "rules.yml");
        if (!configFile.exists()) {
            plugin.saveResource("rules.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        this.autoApproveEnabled = config.getBoolean("auto-approve.enabled", false);
        try {
            this.autoApproveStart = LocalTime.parse(config.getString("auto-approve.start-time", "00:00"));
            this.autoApproveEnd = LocalTime.parse(config.getString("auto-approve.end-time", "06:00"));
        } catch (DateTimeParseException e) {
            plugin.getLogger().warning("Invalid time format in rules.yml. Auto-approve disabled.");
            this.autoApproveEnabled = false;
        }

        this.expirationEnabled = config.getBoolean("expiration.enabled", true);
        this.expirationMillis = config.getLong("expiration.duration-minutes", 1440) * 60 * 1000;
    }

    public boolean shouldAutoApprove() {
        if (!autoApproveEnabled)
            return false;
        LocalTime now = LocalTime.now();
        if (autoApproveStart.isBefore(autoApproveEnd)) {
            return now.isAfter(autoApproveStart) && now.isBefore(autoApproveEnd);
        } else {
            return now.isAfter(autoApproveStart) || now.isBefore(autoApproveEnd);
        }
    }

    public boolean isExpired(StagedCommand command) {
        if (!expirationEnabled)
            return false;
        return System.currentTimeMillis() - command.getTimestamp() > expirationMillis;
    }
}
