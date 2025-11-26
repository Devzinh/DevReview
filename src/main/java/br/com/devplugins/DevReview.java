package br.com.devplugins;

import br.com.devplugins.audit.AuditManager;
import br.com.devplugins.metrics.MetricsManager;
import br.com.devplugins.notifications.NotificationManager;
import br.com.devplugins.rules.RulesEngine;
import br.com.devplugins.scheduler.SchedulerManager;
import br.com.devplugins.ranking.RankingManager;
import br.com.devplugins.staging.*;
import br.com.devplugins.utils.CategoryManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * DevReview - Main plugin class for the command staging and review system.
 * 
 * <p>This plugin intercepts critical administrative commands and places them in a review queue,
 * allowing authorized administrators to approve or reject commands before execution. This provides
 * an additional layer of security and accountability for sensitive server operations.</p>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Command interception and staging for review</li>
 *   <li>GUI-based review interface for administrators</li>
 *   <li>Comprehensive audit logging of all actions</li>
 *   <li>Multi-language support (7 languages)</li>
 *   <li>Flexible notification system (chat, action bar, title, sound)</li>
 *   <li>Reviewer ranking system</li>
 *   <li>Auto-approval rules based on time windows</li>
 *   <li>Command expiration handling</li>
 *   <li>Dual persistence: JSON or SQL (MySQL/MariaDB)</li>
 * </ul>
 * 
 * <h2>Architecture:</h2>
 * <p>The plugin follows a modular architecture with clear separation of concerns:</p>
 * <ul>
 *   <li><b>StagingManager</b>: Core business logic for command lifecycle</li>
 *   <li><b>Repository Layer</b>: Abstracted persistence (JSON/SQL)</li>
 *   <li><b>AuditManager</b>: Comprehensive logging system</li>
 *   <li><b>NotificationManager</b>: Multi-channel notification delivery</li>
 *   <li><b>RulesEngine</b>: Auto-approval and expiration logic</li>
 *   <li><b>RankingManager</b>: Reviewer statistics and leaderboards</li>
 *   <li><b>GUI Components</b>: Inventory-based user interfaces</li>
 * </ul>
 * 
 * <h2>Thread Safety:</h2>
 * <p>The plugin is designed with thread safety in mind:</p>
 * <ul>
 *   <li>All I/O operations (file, database) are executed asynchronously</li>
 *   <li>Concurrent data structures used where appropriate (CopyOnWriteArrayList, ConcurrentHashMap)</li>
 *   <li>Main thread used for Bukkit API calls and event handling</li>
 * </ul>
 * 
 * <h2>Configuration:</h2>
 * <p>The plugin uses multiple YAML configuration files:</p>
 * <ul>
 *   <li><b>config.yml</b>: Main plugin configuration</li>
 *   <li><b>database.yml</b>: Persistence backend selection</li>
 *   <li><b>categories.yml</b>: Command categorization and priorities</li>
 *   <li><b>rules.yml</b>: Auto-approval and expiration rules</li>
 *   <li><b>audit-config.yml</b>: Audit logging configuration</li>
 *   <li><b>notifications.yml</b>: Notification channel settings</li>
 *   <li><b>scheduler.yml</b>: Scheduled command configuration</li>
 *   <li><b>lang/*.yml</b>: Language files for internationalization</li>
 * </ul>
 * 
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
public final class DevReview extends JavaPlugin {

    private StagingManager stagingManager;
    private br.com.devplugins.lang.LanguageManager languageManager;
    private AuditManager auditManager;
    private NotificationManager notificationManager;
    private RulesEngine rulesEngine;
    private CategoryManager categoryManager;
    private RankingManager rankingManager;
    private MetricsManager metricsManager;

    private StagedCommandRepository repository;
    private CommandHistoryRepository historyRepository;

    @Override
    public void onEnable() {
        int pluginId = 28104;
        new org.bstats.bukkit.Metrics(this, pluginId);

        saveDefaultConfig();

        this.languageManager = new br.com.devplugins.lang.LanguageManager(this);
        this.auditManager = new AuditManager(this);
        this.rulesEngine = new RulesEngine(this);
        this.categoryManager = new CategoryManager(this);
        this.rankingManager = new RankingManager(this);
        this.metricsManager = new MetricsManager(this);

        // Initialize Repository based on config
        File dbConfig = new File(getDataFolder(), "database.yml");
        if (!dbConfig.exists())
            saveResource("database.yml", false);
        YamlConfiguration dbYaml = YamlConfiguration.loadConfiguration(dbConfig);

        StagedCommandRepository baseRepository;
        if (dbYaml.getBoolean("enabled", false)) {
            baseRepository = new SqlStagedCommandRepository(this);
            this.historyRepository = new SqlCommandHistoryRepository(this);
        } else {
            baseRepository = new JsonStagedCommandRepository(this);
            this.historyRepository = new JsonCommandHistoryRepository(this);
        }
        
        // Wrap repository with retry logic if enabled
        File retryConfig = new File(getDataFolder(), "retry-config.yml");
        if (!retryConfig.exists())
            saveResource("retry-config.yml", false);
        YamlConfiguration retryYaml = YamlConfiguration.loadConfiguration(retryConfig);
        
        if (retryYaml.getBoolean("enabled", true)) {
            int maxRetries = retryYaml.getInt("max-retries", 3);
            long baseDelayMs = retryYaml.getLong("base-delay-ms", 100);
            long maxDelayMs = retryYaml.getLong("max-delay-ms", 5000);
            
            boolean circuitBreakerEnabled = retryYaml.getBoolean("circuit-breaker.enabled", true);
            int failureThreshold = retryYaml.getInt("circuit-breaker.failure-threshold", 5);
            long cooldownMs = retryYaml.getLong("circuit-breaker.cooldown-ms", 30000);
            
            if (circuitBreakerEnabled) {
                this.repository = new RetryableRepository(baseRepository, this, 
                    maxRetries, baseDelayMs, maxDelayMs, failureThreshold, cooldownMs);
                getLogger().info("Repository retry logic enabled with circuit breaker (max retries: " + maxRetries + 
                               ", failure threshold: " + failureThreshold + ")");
            } else {
                this.repository = new RetryableRepository(baseRepository, this, 
                    maxRetries, baseDelayMs, maxDelayMs, Integer.MAX_VALUE, cooldownMs);
                getLogger().info("Repository retry logic enabled without circuit breaker (max retries: " + maxRetries + ")");
            }
        } else {
            this.repository = baseRepository;
            getLogger().info("Repository retry logic disabled");
        }

        // Create notification manager without staging manager dependency
        this.notificationManager = new NotificationManager(this, languageManager, categoryManager);
        this.stagingManager = new StagingManager(this, repository, historyRepository, languageManager, auditManager,
                rulesEngine, rankingManager, metricsManager);
        // Set staging manager supplier for notification manager (for opening status menus)
        this.notificationManager.setStagingManagerSupplier(() -> stagingManager);

        // Register event listener to connect staging events to notifications
        getServer().getPluginManager().registerEvents(
                new br.com.devplugins.listener.StagingEventListener(notificationManager),
                this);

        new SchedulerManager(this, stagingManager);

        // Schedule periodic expiration check (every 5 minutes)
        getServer().getScheduler().runTaskTimer(this, () -> {
            stagingManager.pruneExpiredCommands();
        }, 6000L, 6000L);

        getServer().getPluginManager().registerEvents(
                new br.com.devplugins.listener.CommandInterceptor(stagingManager, this, languageManager, auditManager,
                        notificationManager),
                this);
        getServer().getPluginManager()
                .registerEvents(
                        new br.com.devplugins.listener.GuiListener(stagingManager, languageManager, categoryManager, this),
                        this);

        getCommand("review").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(languageManager.getMessage(sender, "messages.only-players"));
                return true;
            }
            new br.com.devplugins.gui.ReviewMenu(stagingManager, languageManager, (org.bukkit.entity.Player) sender,
                    categoryManager)
                    .open((org.bukkit.entity.Player) sender);
            return true;
        });

        getCommand("mystatus").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage(languageManager.getMessage(sender, "messages.only-players"));
                return true;
            }
            if (!sender.hasPermission("devreview.mystatus")) {
                sender.sendMessage(languageManager.getMessage(sender, "messages.no-permission"));
                return true;
            }
            new br.com.devplugins.gui.CommandStatusMenu(stagingManager, languageManager,
                    (org.bukkit.entity.Player) sender,
                    categoryManager)
                    .open((org.bukkit.entity.Player) sender);
            return true;
        });

        getCommand("devrank")
                .setExecutor(new br.com.devplugins.commands.RankingCommand(rankingManager, languageManager));

        getCommand("devreview")
                .setExecutor(new br.com.devplugins.commands.StatsCommand(metricsManager, languageManager, this));

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            if (new br.com.devplugins.placeholders.RankingPlaceholderExpansion(this, rankingManager, languageManager)
                    .register()) {
                getLogger().info("Ranking placeholders registered successfully!");
            }
        }
    }

    @Override
    public void onDisable() {
        if (auditManager != null) {
            auditManager.shutdown();
        }
    }
    
    /**
     * Gets the staged command repository instance.
     * 
     * @return the repository instance (may be wrapped with RetryableRepository)
     */
    public StagedCommandRepository getRepository() {
        return repository;
    }
    
    /**
     * Gets the staging manager instance.
     * 
     * @return the staging manager
     */
    public StagingManager getStagingManager() {
        return stagingManager;
    }
}
