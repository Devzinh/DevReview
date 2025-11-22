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

public class CommandDetailMenu implements InventoryHolder {

    private final StagedCommand command;
    private final Inventory inventory;

    public CommandDetailMenu(StagingManager stagingManager, StagedCommand command) {
        this.command = command;
        this.inventory = Bukkit.createInventory(this, 27, "Review Command");
        loadItems();
    }

    private void loadItems() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(ChatColor.YELLOW + "Command Details");
            infoMeta.setLore(Arrays.asList(
                    ChatColor.GRAY + "Player: " + ChatColor.WHITE + command.getSenderName(),
                    ChatColor.GRAY + "Command: " + ChatColor.WHITE + command.getCommandLine(),
                    ChatColor.GRAY + "Time: " + ChatColor.WHITE + sdf.format(new Date(command.getTimestamp()))));
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(13, info);

        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "APPROVE");
            confirmMeta.setLore(Arrays.asList(ChatColor.GRAY + "Execute this command"));
            confirm.setItemMeta(confirmMeta);
        }
        inventory.setItem(11, confirm);

        ItemStack deny = new ItemStack(Material.RED_WOOL);
        ItemMeta denyMeta = deny.getItemMeta();
        if (denyMeta != null) {
            denyMeta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + "REJECT");
            denyMeta.setLore(Arrays.asList(ChatColor.GRAY + "Discard this command"));
            deny.setItemMeta(denyMeta);
        }
        inventory.setItem(15, deny);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public StagedCommand getCommand() {
        return command;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
