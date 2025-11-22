package br.com.devplugins.gui;

import br.com.devplugins.staging.StagedCommand;
import br.com.devplugins.staging.StagingManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ReviewMenu implements InventoryHolder {

    private final StagingManager stagingManager;
    private final Inventory inventory;
    private final br.com.devplugins.lang.LanguageManager languageManager;
    private final Player viewer;

    public ReviewMenu(StagingManager stagingManager, br.com.devplugins.lang.LanguageManager languageManager,
            Player viewer) {
        this.stagingManager = stagingManager;
        this.languageManager = languageManager;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 54, languageManager.getMessage(viewer, "gui.review-title"));
        loadItems();
    }

    private void loadItems() {
        inventory.clear();
        List<StagedCommand> commands = stagingManager.getPendingCommands();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        int slot = 0;
        for (StagedCommand cmd : commands) {
            if (slot >= 54)
                break;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.YELLOW + cmd.getSenderName());

                String listCommand = languageManager.getMessage(viewer, "gui.items.list-command").replace("%command%",
                        cmd.getCommandLine());
                String listTime = languageManager.getMessage(viewer, "gui.items.list-time").replace("%time%",
                        sdf.format(new Date(cmd.getTimestamp())));
                String listId = languageManager.getMessage(viewer, "gui.items.list-id").replace("%id%",
                        cmd.getId().toString());
                String clickReview = languageManager.getMessage(viewer, "gui.items.click-to-review");

                meta.setLore(Arrays.asList(
                        listCommand,
                        listTime,
                        listId,
                        "",
                        clickReview));
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
