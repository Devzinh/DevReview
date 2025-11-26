package br.com.devplugins.listener;

import br.com.devplugins.audit.AuditManager;
import br.com.devplugins.notifications.NotificationManager;
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
    private final AuditManager auditManager;
    private final NotificationManager notificationManager;

    public CommandInterceptor(StagingManager stagingManager, org.bukkit.plugin.java.JavaPlugin plugin,
            br.com.devplugins.lang.LanguageManager languageManager, AuditManager auditManager,
            NotificationManager notificationManager) {
        this.stagingManager = stagingManager;
        this.plugin = plugin;
        this.languageManager = languageManager;
        this.auditManager = auditManager;
        this.notificationManager = notificationManager;
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String command = message.split(" ")[0].toLowerCase();

        List<String> criticalCommands = plugin.getConfig().getStringList("critical-commands");
        if (criticalCommands.isEmpty()) {
            criticalCommands = Arrays.asList("/op", "/deop", "/stop", "/reload", "/restart", "/ban", "/kick");
        }

        if (criticalCommands.contains(command)) {
            if (event.getPlayer().hasPermission("devreview.bypass")) {
                auditManager.log("bypass", event.getPlayer().getName(), "Bypassed review for: " + message);
                notificationManager.notifyBypass(event.getPlayer().getName(), message);

                // Optional: still send feedback to the player
                String msg = languageManager.getMessage(event.getPlayer(), "messages.bypass-player-notification");
                event.getPlayer().sendMessage(msg.replace("%command%", message));
                return;
            }

            event.setCancelled(true);
            stagingManager.stageCommand(event.getPlayer(), message);
        }
    }
}
