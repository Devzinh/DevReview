package br.com.devplugins.rules;

import br.com.devplugins.staging.StagedCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

/**
 * Evaluates auto-approval and expiration rules for staged commands.
 * 
 * <p>This class implements the business logic for determining when commands should be
 * automatically approved or expired based on configurable rules.</p>
 * 
 * <h2>Auto-Approval:</h2>
 * <p>Commands can be automatically approved during configured time windows. This is useful
 * for low-traffic periods (e.g., overnight) when immediate command execution is acceptable.</p>
 * 
 * <p><b>Time Window Handling:</b> The engine correctly handles time windows that cross midnight
 * (e.g., 22:00 to 06:00). The logic checks if the current time falls within the window,
 * accounting for day boundaries.</p>
 * 
 * <h2>Expiration:</h2>
 * <p>Commands that remain in the pending queue beyond the configured duration are considered
 * expired and should be removed. This prevents the queue from growing indefinitely with
 * stale commands.</p>
 * 
 * <h2>Configuration:</h2>
 * <p>Rules are configured in rules.yml:</p>
 * <ul>
 *   <li><b>auto-approve.enabled</b>: Enable/disable auto-approval</li>
 *   <li><b>auto-approve.start-time</b>: Start of auto-approval window (HH:mm format)</li>
 *   <li><b>auto-approve.end-time</b>: End of auto-approval window (HH:mm format)</li>
 *   <li><b>expiration.enabled</b>: Enable/disable expiration</li>
 *   <li><b>expiration.duration-minutes</b>: Time until command expires</li>
 * </ul>
 * 
 * <h2>Error Handling:</h2>
 * <p>If time formats are invalid, auto-approval is automatically disabled and a warning
 * is logged. This prevents the plugin from failing to load due to configuration errors.</p>
 * 
 * <h2>Thread Safety:</h2>
 * <p>This class is thread-safe for read operations. Configuration is loaded once during
 * initialization and not modified afterward.</p>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Check if auto-approval is active
 * if (rulesEngine.shouldAutoApprove()) {
 *     // Execute command immediately
 * }
 * 
 * // Check if command has expired
 * if (rulesEngine.isExpired(command)) {
 *     // Remove from queue
 * }
 * }</pre>
 * 
 * @see StagedCommand
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
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

    /**
     * Determines if commands should be auto-approved at the current time.
     * 
     * <p>This method checks if the current time falls within the configured auto-approval
     * window. It correctly handles windows that cross midnight.</p>
     * 
     * <p><b>Examples:</b></p>
     * <ul>
     *   <li>Window 00:00-06:00: Auto-approve from midnight to 6 AM</li>
     *   <li>Window 22:00-06:00: Auto-approve from 10 PM to 6 AM (crosses midnight)</li>
     *   <li>Window 09:00-17:00: Auto-approve from 9 AM to 5 PM</li>
     * </ul>
     * 
     * @return true if auto-approval is enabled and current time is within the window
     */
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

    /**
     * Determines if a staged command has expired.
     * 
     * <p>A command is considered expired if the time elapsed since its timestamp exceeds
     * the configured expiration duration.</p>
     * 
     * <p><b>Calculation:</b> {@code currentTime - command.timestamp > expirationDuration}</p>
     * 
     * @param command the staged command to check
     * @return true if expiration is enabled and the command has exceeded the duration
     */
    public boolean isExpired(StagedCommand command) {
        if (!expirationEnabled)
            return false;
        return System.currentTimeMillis() - command.getTimestamp() > expirationMillis;
    }
}
