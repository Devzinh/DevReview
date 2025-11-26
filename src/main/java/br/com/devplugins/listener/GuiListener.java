package br.com.devplugins.listener;

import br.com.devplugins.gui.CommandDetailMenu;
import br.com.devplugins.gui.CommandStatusMenu;
import br.com.devplugins.gui.RankingMenu;
import br.com.devplugins.gui.ReviewMenu;
import br.com.devplugins.staging.StagedCommand;
import br.com.devplugins.staging.StagingManager;
import br.com.devplugins.utils.CategoryManager;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class GuiListener implements Listener {

    private final StagingManager stagingManager;
    private final br.com.devplugins.lang.LanguageManager languageManager;
    private final CategoryManager categoryManager;
    private final Plugin plugin;
    private final Map<UUID, Consumer<String>> awaitingJustification = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> justificationTimeouts = new ConcurrentHashMap<>();

    public GuiListener(StagingManager stagingManager, br.com.devplugins.lang.LanguageManager languageManager,
            CategoryManager categoryManager, Plugin plugin) {
        this.stagingManager = stagingManager;
        this.languageManager = languageManager;
        this.categoryManager = categoryManager;
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null)
            return;

        if (holder instanceof ReviewMenu) {
            event.setCancelled(true);
            handleReviewMenuClick(event, (ReviewMenu) holder);
        } else if (holder instanceof CommandDetailMenu) {
            event.setCancelled(true);
            handleDetailMenuClick(event, (CommandDetailMenu) holder);
        } else if (holder instanceof CommandStatusMenu) {
            event.setCancelled(true);
        } else if (holder instanceof RankingMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        if (awaitingJustification.containsKey(playerId)) {
            event.setCancelled(true);
            Consumer<String> action = awaitingJustification.remove(playerId);
            
            // Cancel the timeout task since player responded
            BukkitTask timeoutTask = justificationTimeouts.remove(playerId);
            if (timeoutTask != null) {
                timeoutTask.cancel();
            }

            org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> {
                action.accept(event.getMessage());
            });
        }
    }

    private void handleReviewMenuClick(InventoryClickEvent event, ReviewMenu menu) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        org.bukkit.inventory.meta.ItemMeta meta = item.getItemMeta();
        if (meta == null)
            return;

        List<String> lore = meta.getLore();
        if (lore == null)
            return;

        String idString = null;

        org.bukkit.persistence.PersistentDataContainer data = meta.getPersistentDataContainer();
        org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(
                org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), "command_id");

        if (data.has(key, org.bukkit.persistence.PersistentDataType.STRING)) {
            idString = data.get(key, org.bukkit.persistence.PersistentDataType.STRING);
        }

        if (idString != null) {
            try {
                UUID id = UUID.fromString(idString);
                StagedCommand target = stagingManager.getPendingCommands().stream()
                        .filter(c -> c.getId().equals(id))
                        .findFirst()
                        .orElse(null);

                if (target != null) {
                    new CommandDetailMenu(stagingManager, target, languageManager, (Player) event.getWhoClicked(),
                            categoryManager)
                            .open((Player) event.getWhoClicked());
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void handleDetailMenuClick(InventoryClickEvent event, CommandDetailMenu menu) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        Player player = (Player) event.getWhoClicked();
        StagedCommand command = menu.getCommand();

        if (item.getType() == Material.LIME_WOOL) {
            player.closeInventory();
            player.sendMessage(languageManager.getMessage(player, "messages.enter-justification-approve"));
            awaitingJustification.put(player.getUniqueId(), (justification) -> {
                command.setJustification(justification);
                stagingManager.approveCommand(command, player);
                player.sendMessage(languageManager.getMessage(player, "messages.command-approved"));
                new ReviewMenu(stagingManager, languageManager, player, categoryManager).open(player);
            });
            scheduleJustificationTimeout(player);

        } else if (item.getType() == Material.RED_WOOL) {
            player.closeInventory();
            player.sendMessage(languageManager.getMessage(player, "messages.enter-justification-reject"));
            awaitingJustification.put(player.getUniqueId(), (justification) -> {
                command.setJustification(justification);
                stagingManager.rejectCommand(command, player);
                player.sendMessage(languageManager.getMessage(player, "messages.command-rejected"));
                new ReviewMenu(stagingManager, languageManager, player, categoryManager).open(player);
            });
            scheduleJustificationTimeout(player);
        }
    }

    /**
     * Schedules a timeout task for justification input.
     * After 60 seconds, clears the awaiting state, notifies the player, and reopens ReviewMenu.
     */
    private void scheduleJustificationTimeout(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Cancel any existing timeout for this player
        BukkitTask existingTask = justificationTimeouts.get(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        
        // Schedule new timeout task (60 seconds = 1200 ticks)
        BukkitTask timeoutTask = org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // Remove from awaiting map
            awaitingJustification.remove(playerId);
            justificationTimeouts.remove(playerId);
            
            // Notify player if still online
            Player onlinePlayer = org.bukkit.Bukkit.getPlayer(playerId);
            if (onlinePlayer != null && onlinePlayer.isOnline()) {
                onlinePlayer.sendMessage(languageManager.getMessage(onlinePlayer, "messages.justification-timeout"));
                // Reopen ReviewMenu
                new ReviewMenu(stagingManager, languageManager, onlinePlayer, categoryManager).open(onlinePlayer);
            }
        }, 1200L); // 60 seconds
        
        justificationTimeouts.put(playerId, timeoutTask);
    }
}
