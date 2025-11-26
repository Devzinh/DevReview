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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class CommandStatusMenu implements InventoryHolder {

    private final StagingManager stagingManager;
    private final Inventory inventory;
    private final br.com.devplugins.lang.LanguageManager languageManager;
    private final Player viewer;
    private final CategoryManager categoryManager;

    public CommandStatusMenu(StagingManager stagingManager,
            br.com.devplugins.lang.LanguageManager languageManager, Player viewer, CategoryManager categoryManager) {
        this.stagingManager = stagingManager;
        this.languageManager = languageManager;
        this.viewer = viewer;
        this.categoryManager = categoryManager;
        this.inventory = Bukkit.createInventory(this, 54, languageManager.getMessage(viewer, "gui.status-title"));
        loadItems();
    }

    private void loadItems() {
        inventory.clear();
        List<StagedCommand> history = stagingManager.getHistoryManager()
                .getRecentHistoryForPlayer(viewer.getUniqueId(), 45);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        int slot = 0;
        for (StagedCommand cmd : history) {
            if (slot >= 45)
                break;

            CategoryManager.Category category = categoryManager.getCategory(cmd.getCommandLine());
            StagedCommand.Status status = cmd.getStatus();

            Material material;
            String statusColor;
            String statusText;

            if (status == StagedCommand.Status.APPROVED) {
                material = Material.LIME_CONCRETE;
                statusColor = "&a";
                statusText = languageManager.getMessage(viewer, "gui.status.approved");
            } else if (status == StagedCommand.Status.REJECTED) {
                material = Material.RED_CONCRETE;
                statusColor = "&c";
                statusText = languageManager.getMessage(viewer, "gui.status.rejected");
            } else {
                material = Material.YELLOW_CONCRETE;
                statusColor = "&e";
                statusText = languageManager.getMessage(viewer, "gui.status.pending");
            }

            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                String displayName = ChatColor.translateAlternateColorCodes('&',
                        statusColor + statusText + " &7- &f" + cmd.getCommandLine());
                meta.setDisplayName(displayName);

                List<String> lore = new ArrayList<>();
                lore.add("");

                String categoryLabel = languageManager.getMessage(viewer, "messages.category-label");
                lore.add(categoryLabel + category.getDisplayName());

                String commandLine = languageManager.getMessage(viewer, "gui.items.info-command")
                        .replace("%command%", cmd.getCommandLine());
                lore.add(commandLine);

                String timeLine = languageManager.getMessage(viewer, "gui.items.info-time")
                        .replace("%time%", sdf.format(new Date(cmd.getTimestamp())));
                lore.add(timeLine);

                if (cmd.getReviewerName() != null) {
                    lore.add("");
                    String reviewerLabel = languageManager.getMessage(viewer, "gui.status.reviewer-label");
                    lore.add(reviewerLabel + "&f" + cmd.getReviewerName());
                }

                if (cmd.getJustification() != null && !cmd.getJustification().isEmpty()) {
                    lore.add("");
                    String reasonLabel = languageManager.getMessage(viewer, "gui.status.reason-label");
                    lore.add(reasonLabel);
                    String[] reasonLines = cmd.getJustification().split("\\n");
                    for (String line : reasonLines) {
                        lore.add("&7" + line);
                    }
                }

                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }

        if (history.isEmpty()) {
            ItemStack emptyItem = new ItemStack(Material.BARRIER);
            ItemMeta emptyMeta = emptyItem.getItemMeta();
            if (emptyMeta != null) {
                emptyMeta.setDisplayName(ChatColor.RED + languageManager.getMessage(viewer, "gui.status.no-history"));
                emptyMeta.setLore(Arrays.asList(languageManager.getMessage(viewer, "gui.status.no-history-lore")));
                emptyItem.setItemMeta(emptyMeta);
            }
            inventory.setItem(22, emptyItem);
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

