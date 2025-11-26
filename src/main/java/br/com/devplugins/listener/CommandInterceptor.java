package br.com.devplugins.listener;

import br.com.devplugins.audit.AuditManager;
import br.com.devplugins.notifications.NotificationManager;
import br.com.devplugins.staging.StagingManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Intercepts critical commands before execution for review.
 * 
 * <p>This listener is the entry point for the command staging system. It monitors all player
 * commands and intercepts those configured as "critical" in config.yml, preventing immediate
 * execution and routing them through the review process.</p>
 * 
 * <h2>Interception Logic:</h2>
 * <ol>
 *   <li>Player executes a command</li>
 *   <li>Check if command is in critical-commands list</li>
 *   <li>Check if player has bypass permission</li>
 *   <li>If bypass: allow execution, log, and notify admins</li>
 *   <li>If no bypass: cancel event and stage command for review</li>
 * </ol>
 * 
 * <h2>Bypass Permission:</h2>
 * <p>Players with the "devreview.bypass" permission can execute critical commands immediately
 * without staging. This is useful for trusted administrators who need unrestricted access.</p>
 * 
 * <p><b>Security Note:</b> Bypass actions are logged to the audit system and all online
 * administrators are notified to maintain accountability.</p>
 * 
 * <h2>Critical Commands:</h2>
 * <p>The list of critical commands is configured in config.yml under "critical-commands".
 * If not configured, a default list is used: /op, /deop, /stop, /reload, /restart, /ban, /kick</p>
 * 
 * <h2>Command Matching:</h2>
 * <p>Only the command name (first word) is matched, not arguments. Matching is case-insensitive.
 * The leading slash is included in the comparison.</p>
 * 
 * <h2>Thread Safety:</h2>
 * <p>This listener is called on the main thread by Bukkit's event system. All operations
 * are thread-safe within this context.</p>
 * 
 * @see StagingManager
 * @see AuditManager
 * @see NotificationManager
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
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

    /**
     * Handles command preprocessing to intercept critical commands.
     * 
     * <p>This method is called for every player command before execution. It checks if the
     * command is critical and either allows it (with bypass), or cancels it and stages it
     * for review.</p>
     * 
     * <p><b>Event Cancellation:</b> If a command is staged, the event is cancelled to prevent
     * immediate execution. The command will only execute if approved by a reviewer.</p>
     * 
     * @param event the command preprocess event
     */
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
