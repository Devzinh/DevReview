package br.com.devplugins.audit;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
