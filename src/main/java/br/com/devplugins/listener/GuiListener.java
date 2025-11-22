package br.com.devplugins.listener;

import br.com.devplugins.gui.CommandDetailMenu;
import br.com.devplugins.gui.ReviewMenu;
import br.com.devplugins.staging.StagedCommand;
import br.com.devplugins.staging.StagingManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class GuiListener implements Listener {

    private final StagingManager stagingManager;
    private final br.com.devplugins.lang.LanguageManager languageManager;

    public GuiListener(StagingManager stagingManager, br.com.devplugins.lang.LanguageManager languageManager) {
        this.stagingManager = stagingManager;
        this.languageManager = languageManager;
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
        }
    }

    private void handleReviewMenuClick(InventoryClickEvent event, ReviewMenu menu) {
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR)
            return;

        List<String> lore = item.getItemMeta().getLore();
        if (lore == null)
            return;

        String idString = null;

        if (lore.size() > 2) {
            String line = lore.get(2);

            String text = ChatColor.stripColor(line);
            int lastSpace = text.lastIndexOf(" ");
            if (lastSpace != -1) {
                idString = text.substring(lastSpace + 1).trim();
            }
        }

        if (idString != null) {
            try {
                UUID id = UUID.fromString(idString);
                StagedCommand target = stagingManager.getPendingCommands().stream()
                        .filter(c -> c.getId().equals(id))
                        .findFirst()
                        .orElse(null);

                if (target != null) {
                    new CommandDetailMenu(stagingManager, target, languageManager, (Player) event.getWhoClicked())
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
            stagingManager.approveCommand(command);
            player.sendMessage(languageManager.getMessage(player, "messages.command-approved"));
            player.closeInventory();
            new ReviewMenu(stagingManager, languageManager, player).open(player);
        } else if (item.getType() == Material.RED_WOOL) {
            stagingManager.rejectCommand(command);
            player.sendMessage(languageManager.getMessage(player, "messages.command-rejected"));
            player.closeInventory();
            new ReviewMenu(stagingManager, languageManager, player).open(player);
        }
    }
}
