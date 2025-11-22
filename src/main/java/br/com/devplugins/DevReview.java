package br.com.devplugins;

import org.bukkit.plugin.java.JavaPlugin;
import br.com.devplugins.staging.StagingManager;

public final class DevReview extends JavaPlugin {

    private StagingManager stagingManager;

    @Override
    public void onEnable() {
        this.stagingManager = new StagingManager(this);

        getServer().getPluginManager().registerEvents(new br.com.devplugins.listener.CommandInterceptor(stagingManager),
                this);
        getServer().getPluginManager().registerEvents(new br.com.devplugins.listener.GuiListener(stagingManager), this);

        getCommand("review").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player)) {
                sender.sendMessage("§cOnly players can use this command.");
                return true;
            }
            new br.com.devplugins.gui.ReviewMenu(stagingManager).open((org.bukkit.entity.Player) sender);
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
