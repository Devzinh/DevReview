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

    public GuiListener(StagingManager stagingManager) {
        this.stagingManager = stagingManager;
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
        for (String line : lore) {
            String text = ChatColor.stripColor(line);
            if (text.startsWith("ID: ")) {
                idString = text.substring(4).trim();
                break;
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
                    new CommandDetailMenu(stagingManager, target).open((Player) event.getWhoClicked());
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
            player.sendMessage(ChatColor.GREEN + "Command approved.");
            player.closeInventory();
            new ReviewMenu(stagingManager).open(player);
        } else if (item.getType() == Material.RED_WOOL) {
            stagingManager.rejectCommand(command);
            player.sendMessage(ChatColor.RED + "Command rejected.");
            player.closeInventory();
            new ReviewMenu(stagingManager).open(player);
        }
    }
}
