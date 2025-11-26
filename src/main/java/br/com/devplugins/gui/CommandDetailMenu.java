package br.com.devplugins.gui;

import br.com.devplugins.staging.StagedCommand;
import br.com.devplugins.staging.StagingManager;
import br.com.devplugins.utils.CategoryManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

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

        inventory.setItem(4, createRequesterHead());

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
            String categoryLabel = languageManager.getMessage(viewer, "messages.category-label");
            String categoryLine = categoryLabel + category.getDisplayName();

            infoMeta.setLore(Arrays.asList(
                    categoryLine,
                    infoPlayer,
                    infoCommand,
                    infoTime));
            info.setItemMeta(infoMeta);
        }
        inventory.setItem(13, info);

        inventory.setItem(11, createActionItem(Material.LIME_WOOL, "gui.items.approve-name", "gui.items.approve-lore"));
        inventory.setItem(15, createActionItem(Material.RED_WOOL, "gui.items.reject-name", "gui.items.reject-lore"));
    }

    private ItemStack createRequesterHead() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
        if (skullMeta != null) {
            String senderName = command.getSenderName();
            skullMeta.setDisplayName(languageManager.getMessage(viewer, "gui.items.requester-name")
                    .replace("%player%", senderName));
            
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(command.getSenderId());
            if (offlinePlayer != null) {
                skullMeta.setOwningPlayer(offlinePlayer);
            }
            
            String requesterLabel = languageManager.getMessage(viewer, "gui.items.requester-label");
            skullMeta.setLore(Arrays.asList(requesterLabel, senderName));
            head.setItemMeta(skullMeta);
        }
        return head;
    }

    private ItemStack createActionItem(Material material, String nameKey, String loreKey) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(languageManager.getMessage(viewer, nameKey));
            meta.setLore(Arrays.asList(languageManager.getMessage(viewer, loreKey)));
            item.setItemMeta(meta);
        }
        return item;
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
