package br.com.devplugins.notifications;

import br.com.devplugins.gui.CommandStatusMenu;
import br.com.devplugins.lang.LanguageManager;
import br.com.devplugins.staging.StagedCommand;
import br.com.devplugins.staging.StagingManager;
import br.com.devplugins.utils.CategoryManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.function.Supplier;

/**
 * Manages multi-channel notifications for command staging events.
 * 
 * <p>This class provides a flexible notification system that can deliver messages through
 * multiple channels simultaneously. Notifications are sent for staging, approval, rejection,
 * and bypass events.</p>
 * 
 * <h2>Notification Channels:</h2>
 * <ul>
 *   <li><b>Chat</b>: Standard chat messages</li>
 *   <li><b>Action Bar</b>: Text above the hotbar</li>
 *   <li><b>Title</b>: Large text overlay on screen</li>
 *   <li><b>Sound</b>: Audio notification</li>
 * </ul>
 * 
 * <h2>Notification Types:</h2>
 * <ul>
 *   <li><b>Staging</b>: Sent to all admins when a command is staged</li>
 *   <li><b>Approval</b>: Sent to admins (optional) and command sender</li>
 *   <li><b>Rejection</b>: Sent to admins and command sender</li>
 *   <li><b>Bypass</b>: Sent to all admins when a command bypasses staging</li>
 * </ul>
 * 
 * <h2>Auto-Opening Status Menu:</h2>
 * <p>When a command is approved or rejected, the sender's status menu is automatically
 * opened after a 1-second delay (if the player is online).</p>
 * 
 * <h2>Circular Dependency Resolution:</h2>
 * <p>This class uses a {@link Supplier} pattern to lazily access StagingManager, avoiding
 * circular dependency issues during initialization.</p>
 * 
 * <h2>Configuration:</h2>
 * <p>Notification behavior is configured in notifications.yml with granular control over
 * each channel for each event type.</p>
 * 
 * <h2>Thread Safety:</h2>
 * <p>All notification methods should be called from the main thread to ensure proper
 * interaction with the Bukkit API.</p>
 * 
 * @see LanguageManager
 * @see StagingManager
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
public class NotificationManager {

    private final JavaPlugin plugin;
    private final LanguageManager languageManager;
    private final CategoryManager categoryManager;
    private YamlConfiguration config;
    // Use a supplier to lazily get StagingManager to avoid circular dependency
    private Supplier<StagingManager> stagingManagerSupplier;

    public NotificationManager(JavaPlugin plugin, LanguageManager languageManager, CategoryManager categoryManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        this.categoryManager = categoryManager;
        loadConfig();
    }

    /**
     * Set the staging manager supplier for opening status menus.
     * This uses a supplier pattern to avoid circular dependency.
     */
    public void setStagingManagerSupplier(Supplier<StagingManager> stagingManagerSupplier) {
        this.stagingManagerSupplier = stagingManagerSupplier;
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "notifications.yml");
        if (!configFile.exists()) {
            plugin.saveResource("notifications.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void notifyStaging(StagedCommand command) {
        notify("staging", command, "devreview.admin");
    }

    public void notifyApproval(StagedCommand command) {
        ConfigurationSection section = config.getConfigurationSection("notifications.approval");
        boolean broadcast = section != null && section.getBoolean("broadcast-admin", true);

        if (broadcast) {
            notify("approval", command, "devreview.admin");
        }

        // Notify sender specifically if online
        Player sender = Bukkit.getPlayer(command.getSenderId());
        if (sender != null && sender.isOnline()) {
            sendNotificationToPlayer(sender, "approval", command);
            // Open status menu after a short delay
            if (stagingManagerSupplier != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (sender.isOnline()) {
                        StagingManager stagingManager = stagingManagerSupplier.get();
                        if (stagingManager != null) {
                            new CommandStatusMenu(stagingManager, languageManager, sender, categoryManager).open(sender);
                        }
                    }
                }, 20L);
            }
        }
    }

    public void notifyRejection(StagedCommand command) {
        notify("rejection", command, "devreview.admin");

        Player sender = Bukkit.getPlayer(command.getSenderId());
        if (sender != null && sender.isOnline()) {
            sendNotificationToPlayer(sender, "rejection", command);
            // Open status menu after a short delay
            if (stagingManagerSupplier != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (sender.isOnline()) {
                        StagingManager stagingManager = stagingManagerSupplier.get();
                        if (stagingManager != null) {
                            new CommandStatusMenu(stagingManager, languageManager, sender, categoryManager).open(sender);
                        }
                    }
                }, 20L);
            }
        }
    }

    public void notifyBypass(String player, String command) {
        // Custom handling for bypass since it doesn't have a StagedCommand yet or is
        // bypassed
        // For simplicity, we'll just broadcast to admins
        // Actually, let's use the config structure

        ConfigurationSection section = config.getConfigurationSection("notifications.bypass");
        if (section == null)
            return;

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("devreview.admin"))
                .forEach(p -> {
                    String msg = languageManager.getMessage(p, "messages.bypass-admin-notification")
                            .replace("%player%", player)
                            .replace("%command%", command);

                    if (section.getBoolean("chat"))
                        p.sendMessage(msg);
                    if (section.getBoolean("action-bar"))
                        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
                    if (section.getBoolean("title"))
                        p.sendTitle(ChatColor.RED + "Bypass Alert", msg, 10, 70, 20);

                    String soundName = section.getString("sound");
                    if (soundName != null && !soundName.isEmpty()) {
                        try {
                            p.playSound(p.getLocation(), Sound.valueOf(soundName), 1f, 1f);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                });
    }

    private void notify(String type, StagedCommand command, String permission) {
        ConfigurationSection section = config.getConfigurationSection("notifications." + type);
        if (section == null)
            return;

        Bukkit.getOnlinePlayers().stream()
                .filter(p -> permission == null || p.hasPermission(permission))
                .forEach(p -> sendNotificationToPlayer(p, type, command));
    }

    private void sendNotificationToPlayer(Player p, String type, StagedCommand command) {
        ConfigurationSection section = config.getConfigurationSection("notifications." + type);
        if (section == null)
            return;

        String msgKey = "messages." + type + "-notification";
        // Mapping types to existing keys or new ones
        if (type.equals("staging"))
            msgKey = "messages.staging-notification";
        if (type.equals("approval"))
            msgKey = "messages.approval-notification";
        if (type.equals("rejection"))
            msgKey = "messages.rejection-notification";

        String msg = languageManager.getMessage(p, msgKey)
                .replace("%player%", command.getSenderName())
                .replace("%command%", command.getCommandLine());

        if (command.getJustification() != null) {
            msg += " (" + command.getJustification() + ")";
        }

        if (section.getBoolean("chat"))
            p.sendMessage(msg);
        if (section.getBoolean("action-bar"))
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
        if (section.getBoolean("title"))
            p.sendTitle("", msg, 10, 70, 20);

        String soundName = section.getString("sound");
        if (soundName != null && !soundName.isEmpty()) {
            try {
                p.playSound(p.getLocation(), Sound.valueOf(soundName), 1f, 1f);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }
}
