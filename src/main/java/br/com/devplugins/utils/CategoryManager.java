package br.com.devplugins.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages command categorization and prioritization.
 * 
 * <p>This class categorizes commands based on prefix matching and assigns priorities for
 * display ordering. Categories help organize commands in the review GUI and provide visual
 * distinction between different types of commands.</p>
 * 
 * <h2>Category Structure:</h2>
 * <p>Each category has:</p>
 * <ul>
 *   <li><b>ID</b>: Unique identifier (e.g., "critical", "moderation")</li>
 *   <li><b>Priority</b>: Numeric priority (lower = higher priority, displayed first)</li>
 *   <li><b>Display Name</b>: Formatted name with color codes</li>
 *   <li><b>Commands</b>: List of command prefixes to match</li>
 * </ul>
 * 
 * <h2>Matching Logic:</h2>
 * <p>Commands are matched against category prefixes using case-insensitive comparison.
 * The leading slash is automatically removed before matching. The first matching category
 * is returned.</p>
 * 
 * <p><b>Example:</b></p>
 * <ul>
 *   <li>Command: "/op PlayerName" → Matches "op" prefix → Returns "critical" category</li>
 *   <li>Command: "/ban PlayerName" → Matches "ban" prefix → Returns "moderation" category</li>
 *   <li>Command: "/unknown" → No match → Returns default category</li>
 * </ul>
 * 
 * <h2>Default Category:</h2>
 * <p>If no category matches, the default category is returned. This ensures every command
 * has a category for consistent display.</p>
 * 
 * <h2>Priority-Based Ordering:</h2>
 * <p>Categories with lower priority numbers are displayed first in the review GUI. This
 * allows critical commands (e.g., /op, /stop) to appear at the top of the list.</p>
 * 
 * <h2>Configuration:</h2>
 * <p>Categories are configured in categories.yml with support for color codes using '&amp;'.</p>
 * 
 * <h2>Thread Safety:</h2>
 * <p>This class is thread-safe for read operations. Configuration is loaded once during
 * initialization and not modified afterward.</p>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * Category category = categoryManager.getCategory("/op PlayerName");
 * int priority = category.getPriority();
 * String displayName = category.getDisplayName();
 * }</pre>
 * 
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
public class CategoryManager {

    private final JavaPlugin plugin;
    private final Map<String, Category> categories = new HashMap<>();
    private Category defaultCategory;
    
    // Cache for frequently accessed categories (thread-safe)
    private final Map<String, Category> categoryCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100;

    public CategoryManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "categories.yml");
        if (!configFile.exists()) {
            plugin.saveResource("categories.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection catsSection = config.getConfigurationSection("categories");

        if (catsSection != null) {
            for (String key : catsSection.getKeys(false)) {
                ConfigurationSection section = catsSection.getConfigurationSection(key);
                if (section != null) {
                    int priority = section.getInt("priority", 99);
                    String displayName = ChatColor.translateAlternateColorCodes('&',
                            section.getString("display-name", key));
                    List<String> commands = section.getStringList("commands");
                    categories.put(key, new Category(key, priority, displayName, commands));
                }
            }
        }

        ConfigurationSection defaultSection = config.getConfigurationSection("default");
        if (defaultSection != null) {
            int priority = defaultSection.getInt("priority", 99);
            String displayName = ChatColor.translateAlternateColorCodes('&',
                    defaultSection.getString("display-name", "General"));
            defaultCategory = new Category("default", priority, displayName, List.of());
        } else {
            defaultCategory = new Category("default", 99, "General", List.of());
        }
    }

    /**
     * Gets the category for a command based on prefix matching.
     * 
     * <p>This method performs case-insensitive prefix matching against all configured
     * categories. The leading slash is automatically removed before matching.</p>
     * 
     * <p><b>Matching Process:</b></p>
     * <ol>
     *   <li>Check cache for previously matched command</li>
     *   <li>Remove leading slash if present</li>
     *   <li>Convert to lowercase</li>
     *   <li>Check each category's command prefixes</li>
     *   <li>Return first matching category</li>
     *   <li>Return default category if no match</li>
     *   <li>Store result in cache for future lookups</li>
     * </ol>
     * 
     * <p><b>Determinism:</b> This method is deterministic - the same command will always
     * return the same category (Property 10 in design document).</p>
     * 
     * <p><b>Performance:</b> Frequently accessed commands are cached to avoid repeated
     * prefix matching. Cache is limited to 100 entries to prevent memory issues.</p>
     * 
     * @param commandLine the full command line (with or without leading slash)
     * @return the matching category, or default category if no match
     */
    public Category getCategory(String commandLine) {
        // Check cache first
        Category cached = categoryCache.get(commandLine);
        if (cached != null) {
            return cached;
        }
        
        String cmd = commandLine.toLowerCase();
        if (cmd.startsWith("/"))
            cmd = cmd.substring(1);
        final String finalCmd = cmd;

        Category result = null;
        for (Category cat : categories.values()) {
            for (String c : cat.commands) {
                if (finalCmd.startsWith(c.toLowerCase())) {
                    result = cat;
                    break;
                }
            }
            if (result != null) break;
        }
        
        if (result == null) {
            result = defaultCategory;
        }
        
        // Cache the result (with size limit)
        if (categoryCache.size() < MAX_CACHE_SIZE) {
            categoryCache.put(commandLine, result);
        }
        
        return result;
    }

    /**
     * Represents a command category with priority and display information.
     * 
     * <p>This immutable class encapsulates all information about a category, including
     * its identifier, priority for ordering, display name with color codes, and the
     * list of command prefixes it matches.</p>
     * 
     * @author DevPlugins
     * @version 1.0
     * @since 1.0
     */
    public static class Category {
        private final String id;
        private final int priority;
        private final String displayName;
        private final List<String> commands;

        /**
         * Creates a new category.
         * 
         * @param id unique identifier for this category
         * @param priority numeric priority (lower = higher priority)
         * @param displayName formatted display name with color codes
         * @param commands list of command prefixes to match
         */
        public Category(String id, int priority, String displayName, List<String> commands) {
            this.id = id;
            this.priority = priority;
            this.displayName = displayName;
            this.commands = commands;
        }

        /**
         * Gets the category identifier.
         * 
         * @return the category ID
         */
        public String getId() {
            return id;
        }

        /**
         * Gets the category priority for ordering.
         * 
         * <p>Lower numbers indicate higher priority and appear first in lists.</p>
         * 
         * @return the priority value
         */
        public int getPriority() {
            return priority;
        }

        /**
         * Gets the formatted display name with color codes.
         * 
         * @return the display name (color codes already translated)
         */
        public String getDisplayName() {
            return displayName;
        }
    }
}
