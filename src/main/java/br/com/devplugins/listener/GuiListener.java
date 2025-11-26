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

public class GuiListener implements Listener {

    private final StagingManager stagingManager;
    private final br.com.devplugins.lang.LanguageManager languageManager;
    private final CategoryManager categoryManager;
    private final Map<UUID, Consumer<String>> awaitingJustification = new ConcurrentHashMap<>();

    public GuiListener(StagingManager stagingManager, br.com.devplugins.lang.LanguageManager languageManager,
            CategoryManager categoryManager) {
        this.stagingManager = stagingManager;
        this.languageManager = languageManager;
        this.categoryManager = categoryManager;
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
        if (awaitingJustification.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            Consumer<String> action = awaitingJustification.remove(player.getUniqueId());

            org.bukkit.Bukkit.getScheduler().runTask(org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()),
                    () -> {
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

        } else if (item.getType() == Material.RED_WOOL) {
            player.closeInventory();
            player.sendMessage(languageManager.getMessage(player, "messages.enter-justification-reject"));
            awaitingJustification.put(player.getUniqueId(), (justification) -> {
                command.setJustification(justification);
                stagingManager.rejectCommand(command, player);
                player.sendMessage(languageManager.getMessage(player, "messages.command-rejected"));
                new ReviewMenu(stagingManager, languageManager, player, categoryManager).open(player);
            });
        }
    }
}
