package br.com.devplugins.gui;

import br.com.devplugins.staging.StagedCommand;
import br.com.devplugins.staging.StagingManager;
import br.com.devplugins.utils.CategoryManager;
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
    private final br.com.devplugins.lang.LanguageManager languageManager;
    private final Player viewer;
    private final CategoryManager categoryManager;

    public CommandDetailMenu(StagingManager stagingManager, StagedCommand command,
            br.com.devplugins.lang.LanguageManager languageManager, Player viewer, CategoryManager categoryManager) {
        this.command = command;
        this.languageManager = languageManager;
        this.viewer = viewer;
        this.categoryManager = categoryManager;
        this.inventory = Bukkit.createInventory(this, 27, languageManager.getMessage(viewer, "gui.detail-title"));
        loadItems();
    }

    private void loadItems() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        CategoryManager.Category category = categoryManager.getCategory(command.getCommandLine());

        // Info Item
        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.setDisplayName(languageManager.getMessage(viewer, "gui.items.info-name"));

            String infoPlayer = languageManager.getMessage(viewer, "gui.items.info-player").replace("%player%",
                    command.getSenderName());
            String infoCommand = languageManager.getMessage(viewer, "gui.items.info-command").replace("%command%",
                    command.getCommandLine());
            String infoTime = languageManager.getMessage(viewer, "gui.items.info-time").replace("%time%",
                    sdf.format(new Date(command.getTimestamp())));
            String categoryLine = ChatColor.GRAY + "Category: " + category.getDisplayName();

            infoMeta.setLore(Arrays.asList(
                    categoryLine,
                    infoPlayer,
                    infoCommand,
                    infoTime));
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(13, info);

        // Confirm Item
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        if (confirmMeta != null) {
            confirmMeta.setDisplayName(languageManager.getMessage(viewer, "gui.items.approve-name"));
            confirmMeta.setLore(Arrays.asList(languageManager.getMessage(viewer, "gui.items.approve-lore")));
            confirm.setItemMeta(confirmMeta);
        }
        inventory.setItem(11, confirm);

        // Deny Item
        ItemStack deny = new ItemStack(Material.RED_WOOL);
        ItemMeta denyMeta = deny.getItemMeta();
        if (denyMeta != null) {
            denyMeta.setDisplayName(languageManager.getMessage(viewer, "gui.items.reject-name"));
            denyMeta.setLore(Arrays.asList(languageManager.getMessage(viewer, "gui.items.reject-lore")));
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
