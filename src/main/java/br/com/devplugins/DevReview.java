package br.com.devplugins;

import org.bukkit.plugin.java.JavaPlugin;
import br.com.devplugins.staging.StagingManager;

public final class DevReview extends JavaPlugin {

    private StagingManager stagingManager;
    private br.com.devplugins.lang.LanguageManager languageManager;

    @Override
    public void onEnable() {
        int pluginId = 28104;
        new org.bstats.bukkit.Metrics(this, pluginId);

        saveDefaultConfig();

        this.languageManager = new br.com.devplugins.lang.LanguageManager(this);

        this.stagingManager = new StagingManager(this, languageManager);

        getServer().getPluginManager().registerEvents(
                new br.com.devplugins.listener.CommandInterceptor(stagingManager, this, languageManager),
                this);
        getServer().getPluginManager()
                .registerEvents(new br.com.devplugins.listener.GuiListener(stagingManager, languageManager), this);

        getCommand("review").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage("§cOnly players can use this command.");
                return true;
            }
            new br.com.devplugins.gui.ReviewMenu(stagingManager, languageManager, (org.bukkit.entity.Player) sender)
                    .open((org.bukkit.entity.Player) sender);
            return true;
        });
    }

    @Override
    public void onDisable() {
        if (stagingManager != null) {
            stagingManager.saveCommands();
        }
    }
}
