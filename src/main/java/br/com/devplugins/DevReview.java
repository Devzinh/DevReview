package br.com.devplugins;

import br.com.devplugins.audit.AuditManager;
import br.com.devplugins.notifications.NotificationManager;
import br.com.devplugins.rules.RulesEngine;
import br.com.devplugins.scheduler.SchedulerManager;
import br.com.devplugins.ranking.RankingManager;
import br.com.devplugins.staging.*;
import br.com.devplugins.utils.CategoryManager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class DevReview extends JavaPlugin {

    private StagingManager stagingManager;
    private br.com.devplugins.lang.LanguageManager languageManager;
    private AuditManager auditManager;
    private NotificationManager notificationManager;
    private RulesEngine rulesEngine;
    private CategoryManager categoryManager;
    private RankingManager rankingManager;

    private StagedCommandRepository repository;

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

        // Initialize Repository based on config
        File dbConfig = new File(getDataFolder(), "database.yml");
        if (!dbConfig.exists())
            saveResource("database.yml", false);
        YamlConfiguration dbYaml = YamlConfiguration.loadConfiguration(dbConfig);

        if (dbYaml.getBoolean("enabled", false)) {
            this.repository = new SqlStagedCommandRepository(this);
        } else {
            this.repository = new JsonStagedCommandRepository(this);
        }

        // Create notification manager first (staging manager will be set later)
        this.notificationManager = new NotificationManager(this, languageManager, null, categoryManager);
        this.stagingManager = new StagingManager(this, repository, languageManager, auditManager, notificationManager,
                rulesEngine, rankingManager);
        // Update notification manager with staging manager reference
        this.notificationManager.setStagingManager(stagingManager);

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
                        new br.com.devplugins.listener.GuiListener(stagingManager, languageManager, categoryManager),
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
}
