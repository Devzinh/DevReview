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
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * GUI for reviewing pending staged commands.
 * 
 * <p>This inventory-based GUI displays all pending commands in a 54-slot chest interface,
 * allowing administrators to see an overview of commands awaiting review. Clicking on a
 * command opens the detailed review menu.</p>
 * 
 * <h2>Display Format:</h2>
 * <p>Each command is represented by a PAPER item with:</p>
 * <ul>
 *   <li><b>Display Name</b>: Category color + sender name</li>
 *   <li><b>Lore</b>: Category, command text, timestamp, UUID, and click prompt</li>
 * </ul>
 * 
 * <h2>Ordering:</h2>
 * <p>Commands are sorted by:</p>
 * <ol>
 *   <li>Category priority (ascending) - Critical commands first</li>
 *   <li>Timestamp (ascending) - Oldest commands first</li>
 * </ol>
 * 
 * <p>This ensures that high-priority, time-sensitive commands appear at the top of the list.</p>
 * 
 * <h2>Capacity:</h2>
 * <p>The GUI can display up to 54 commands (6 rows). If more commands are pending, only
 * the first 54 (after sorting) are shown.</p>
 * 
 * <h2>Data Storage:</h2>
 * <p>Each item stores the command UUID in its PersistentDataContainer, allowing the
 * GuiListener to identify which command was clicked.</p>
 * 
 * <h2>Internationalization:</h2>
 * <p>All text is localized based on the viewer's locale using LanguageManager.</p>
 * 
 * <h2>Thread Safety:</h2>
 * <p>This class should only be instantiated and used on the main thread, as it interacts
 * with the Bukkit inventory API.</p>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * ReviewMenu menu = new ReviewMenu(stagingManager, languageManager, player, categoryManager);
 * menu.open(player);
 * }</pre>
 * 
 * @see CommandDetailMenu
 * @see br.com.devplugins.listener.GuiListener
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
public class ReviewMenu implements InventoryHolder {

    private final StagingManager stagingManager;
    private final Inventory inventory;
    private final br.com.devplugins.lang.LanguageManager languageManager;
    private final Player viewer;
    private final CategoryManager categoryManager;

    public ReviewMenu(StagingManager stagingManager, br.com.devplugins.lang.LanguageManager languageManager,
            Player viewer, CategoryManager categoryManager) {
        this.stagingManager = stagingManager;
        this.languageManager = languageManager;
        this.viewer = viewer;
        this.categoryManager = categoryManager;
        this.inventory = Bukkit.createInventory(this, 54, languageManager.getMessage(viewer, "gui.review-title"));
        loadItems();
    }

    private void loadItems() {
        inventory.clear();
        List<StagedCommand> commands = stagingManager.getPendingCommands();

        // Sort by category priority, then timestamp
        commands.sort(Comparator
                .comparingInt((StagedCommand c) -> categoryManager.getCategory(c.getCommandLine()).getPriority())
                .thenComparingLong(StagedCommand::getTimestamp));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        int slot = 0;
        for (StagedCommand cmd : commands) {
            if (slot >= 54)
                break;

            CategoryManager.Category category = categoryManager.getCategory(cmd.getCommandLine());

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', category.getDisplayName()) + " "
                        + ChatColor.YELLOW + cmd.getSenderName());

                String listCommand = languageManager.getMessage(viewer, "gui.items.list-command").replace("%command%",
                        cmd.getCommandLine());
                String listTime = languageManager.getMessage(viewer, "gui.items.list-time").replace("%time%",
                        sdf.format(new Date(cmd.getTimestamp())));
                String listId = languageManager.getMessage(viewer, "gui.items.list-id").replace("%id%",
                        cmd.getId().toString());
                String clickReview = languageManager.getMessage(viewer, "gui.items.click-to-review");

                String categoryLabel = languageManager.getMessage(viewer, "messages.category-label");
                String categoryLine = categoryLabel + category.getDisplayName();

                meta.setLore(Arrays.asList(
                        categoryLine,
                        listCommand,
                        listTime,
                        listId,
                        "",
                        clickReview));

                // Store ID in PersistentDataContainer
                org.bukkit.persistence.PersistentDataContainer data = meta.getPersistentDataContainer();
                org.bukkit.NamespacedKey key = new org.bukkit.NamespacedKey(
                        org.bukkit.plugin.java.JavaPlugin.getProvidingPlugin(getClass()), "command_id");
                data.set(key, org.bukkit.persistence.PersistentDataType.STRING, cmd.getId().toString());

                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }
    }

    /**
     * Opens this GUI for a player.
     * 
     * @param player the player to show the GUI to
     */
    public void open(Player player) {
        player.openInventory(inventory);
    }

    /**
     * Gets the inventory for this GUI.
     * 
     * <p>This method is required by the InventoryHolder interface and allows the
     * GuiListener to identify this GUI when handling click events.</p>
     * 
     * @return the inventory instance
     */
    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
