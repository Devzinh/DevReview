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

    public ReviewMenu(StagingManager stagingManager) {
        this.stagingManager = stagingManager;
        this.inventory = Bukkit.createInventory(this, 54, "Pending Reviews");
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
                meta.setLore(Arrays.asList(
                        ChatColor.GRAY + "Command: " + ChatColor.WHITE + cmd.getCommandLine(),
                        ChatColor.GRAY + "Time: " + ChatColor.WHITE + sdf.format(new Date(cmd.getTimestamp())),
                        ChatColor.GRAY + "ID: " + ChatColor.DARK_GRAY + cmd.getId(),
                        "",
                        ChatColor.GREEN + "Click to review"));
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
