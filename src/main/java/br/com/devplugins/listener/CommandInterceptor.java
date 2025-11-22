package br.com.devplugins.listener;

import br.com.devplugins.staging.StagingManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.List;

public class CommandInterceptor implements Listener {

    private final StagingManager stagingManager;
    private final org.bukkit.plugin.java.JavaPlugin plugin;

    public CommandInterceptor(StagingManager stagingManager, org.bukkit.plugin.java.JavaPlugin plugin) {
        this.stagingManager = stagingManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String command = message.split(" ")[0].toLowerCase();

        List<String> criticalCommands = plugin.getConfig().getStringList("critical-commands");
        if (criticalCommands.isEmpty()) {
            // Fallback if config is broken or empty
            criticalCommands = Arrays.asList("/op", "/stop", "/reload", "/restart", "/ban", "/deop");
        }

        if (criticalCommands.contains(command)) {
            if (event.getPlayer().hasPermission("staffreview.bypass")) {
                return;
            }

            event.setCancelled(true);
            stagingManager.stageCommand(event.getPlayer(), message);

            String msg = plugin.getConfig().getString("messages.command-staged",
                    "§eCommand staged for review: %command%");
            event.getPlayer().sendMessage(msg.replace("%command%", message));
        }
    }
}
