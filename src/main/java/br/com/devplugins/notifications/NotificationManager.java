package br.com.devplugins.notifications;

import br.com.devplugins.lang.LanguageManager;
import br.com.devplugins.staging.StagedCommand;
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

public class NotificationManager {

    private final JavaPlugin plugin;
    private final LanguageManager languageManager;
    private YamlConfiguration config;

    public NotificationManager(JavaPlugin plugin, LanguageManager languageManager) {
        this.plugin = plugin;
        this.languageManager = languageManager;
        loadConfig();
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
        }
    }

    public void notifyRejection(StagedCommand command) {
        notify("rejection", command, "devreview.admin");

        Player sender = Bukkit.getPlayer(command.getSenderId());
        if (sender != null && sender.isOnline()) {
            sendNotificationToPlayer(sender, "rejection", command);
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
