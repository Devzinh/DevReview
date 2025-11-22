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
    private final br.com.devplugins.lang.LanguageManager languageManager;

    public CommandInterceptor(StagingManager stagingManager, org.bukkit.plugin.java.JavaPlugin plugin,
            br.com.devplugins.lang.LanguageManager languageManager) {
        this.stagingManager = stagingManager;
        this.plugin = plugin;
        this.languageManager = languageManager;
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String command = message.split(" ")[0].toLowerCase();

        List<String> criticalCommands = plugin.getConfig().getStringList("critical-commands");
        if (criticalCommands.isEmpty()) {
            criticalCommands = Arrays.asList("/op", "/stop", "/reload", "/restart", "/ban", "/deop");
        }

        if (criticalCommands.contains(command)) {
            if (event.getPlayer().hasPermission("devreview.bypass")) {
                String msg = languageManager.getMessage(event.getPlayer(), "messages.bypass-notification");
                event.getPlayer().sendMessage(msg.replace("%command%", message));
                return;
            }

            event.setCancelled(true);
            stagingManager.stageCommand(event.getPlayer(), message);

        }
    }
}
