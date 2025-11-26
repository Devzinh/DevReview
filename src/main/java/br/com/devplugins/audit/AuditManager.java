package br.com.devplugins.audit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages comprehensive audit logging for all system actions.
 * 
 * <p>This class provides a robust audit trail for security and compliance purposes. All significant
 * actions (staging, approval, rejection, bypass, expiration) are logged with timestamps, actors,
 * and details.</p>
 * 
 * <h2>Logging Destinations:</h2>
 * <ul>
 *   <li><b>File</b>: Persistent log file (configurable name, default: audit.log)</li>
 *   <li><b>Console</b>: Server console output for real-time monitoring</li>
 * </ul>
 * 
 * <h2>Event Types:</h2>
 * <ul>
 *   <li><b>stage</b>: Command intercepted and added to queue</li>
 *   <li><b>approve</b>: Command approved and executed</li>
 *   <li><b>reject</b>: Command rejected and discarded</li>
 *   <li><b>bypass</b>: Command executed without staging (permission bypass)</li>
 *   <li><b>expire</b>: Command removed due to expiration</li>
 * </ul>
 * 
 * <h2>Thread Safety:</h2>
 * <p>This class uses {@link java.util.concurrent.ConcurrentLinkedQueue} for thread-safe log
 * queuing. File writes are performed asynchronously with periodic flushing (every 1 second)
 * to avoid blocking the main thread.</p>
 * 
 * <h2>Configuration:</h2>
 * <p>Audit behavior is configured in audit-config.yml:</p>
 * <ul>
 *   <li>enabled: Master switch for audit logging</li>
 *   <li>log-to-file: Enable/disable file logging</li>
 *   <li>log-to-console: Enable/disable console logging</li>
 *   <li>file-name: Name of the log file</li>
 *   <li>events: Granular control over which events to log</li>
 * </ul>
 * 
 * <h2>Shutdown Handling:</h2>
 * <p>The {@link #shutdown()} method must be called during plugin disable to ensure all
 * queued log entries are flushed to disk.</p>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * auditManager.log("stage", "PlayerName", "Command staged: /op PlayerName");
 * auditManager.log("approve", "AdminName", "Command approved: /op PlayerName");
 * auditManager.log("bypass", "PlayerName", "Command bypassed: /stop");
 * }</pre>
 * 
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
public class AuditManager {

    private final JavaPlugin plugin;
    private File logFile;
    private boolean enabled;
    private boolean logToFile;
    private boolean logToConsole;
    private boolean logStage;
    private boolean logApprove;
    private boolean logReject;
    private boolean logBypass;

    private final java.util.Queue<String> logQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

    public AuditManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        startLogTask();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "audit-config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("audit-config.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        this.enabled = config.getBoolean("enabled", true);
        this.logToFile = config.getBoolean("log-to-file", true);
        this.logToConsole = config.getBoolean("log-to-console", true);
        String fileName = config.getString("file-name", "audit.log");
        this.logFile = new File(plugin.getDataFolder(), fileName);

        this.logStage = config.getBoolean("events.stage", true);
        this.logApprove = config.getBoolean("events.approve", true);
        this.logReject = config.getBoolean("events.reject", true);
        this.logBypass = config.getBoolean("events.bypass", true);
    }

    private void startLogTask() {
        // Only start the periodic flush task if file logging is enabled.
        // Console logging is handled directly in the log() method.
        if (!logToFile)
            return;

        org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::flushLogs, 20L, 20L);
    }

    /**
     * Shuts down the audit manager and flushes all pending log entries.
     * 
     * <p>This method must be called during plugin disable to ensure no log entries
     * are lost. It performs a final flush of the log queue to disk.</p>
     * 
     * <p><b>Important:</b> After calling this method, no new log entries should be added.</p>
     */
    public void shutdown() {
        flushLogs();
    }

    private void flushLogs() {
        if (logQueue.isEmpty())
            return;

        try (FileWriter fw = new FileWriter(logFile, true);
                PrintWriter pw = new PrintWriter(fw)) {
            while (!logQueue.isEmpty()) {
                pw.println(logQueue.poll());
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to write to audit log: " + e.getMessage());
        }
    }

    /**
     * Logs an audit event.
     * 
     * <p>This method adds an entry to the audit log with the current timestamp, event type,
     * actor, and details. The entry is queued for asynchronous writing to avoid blocking
     * the main thread.</p>
     * 
     * <p><b>Event Filtering:</b> Events can be selectively enabled/disabled in audit-config.yml.
     * If an event type is disabled, this method returns immediately without logging.</p>
     * 
     * <p><b>Format:</b> Log entries follow this format:<br>
     * {@code [timestamp] [EVENT] Actor: actor | Details: details}</p>
     * 
     * <p><b>Thread Safety:</b> This method is thread-safe and can be called from any thread.</p>
     * 
     * @param event the event type (stage, approve, reject, bypass, expire)
     * @param actor the player or system component performing the action
     * @param details additional information about the action
     */
    public void log(String event, String actor, String details) {
        if (!enabled)
            return;

        boolean shouldLog = switch (event.toLowerCase()) {
            case "stage" -> logStage;
            case "approve" -> logApprove;
            case "reject" -> logReject;
            case "bypass" -> logBypass;
            default -> true;
        };

        if (!shouldLog)
            return;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String message = String.format("[%s] [%s] Actor: %s | Details: %s", timestamp, event.toUpperCase(), actor,
                details);

        if (logToConsole) {
            plugin.getLogger().info("[Audit] " + message);
        }

        if (logToFile) {
            logQueue.add(message);
        }
    }
}
