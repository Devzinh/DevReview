package br.com.devplugins.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryManager {

    private final JavaPlugin plugin;
    private final Map<String, Category> categories = new HashMap<>();
    private Category defaultCategory;

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

    public Category getCategory(String commandLine) {
        String cmd = commandLine.toLowerCase();
        if (cmd.startsWith("/"))
            cmd = cmd.substring(1);
        final String finalCmd = cmd;

        for (Category cat : categories.values()) {
            for (String c : cat.commands) {
                if (finalCmd.startsWith(c.toLowerCase())) {
                    return cat;
                }
            }
        }
        return defaultCategory;
    }

    public static class Category {
        private final String id;
        private final int priority;
        private final String displayName;
        private final List<String> commands;

        public Category(String id, int priority, String displayName, List<String> commands) {
            this.id = id;
            this.priority = priority;
            this.displayName = displayName;
            this.commands = commands;
        }

        public String getId() {
            return id;
        }

        public int getPriority() {
            return priority;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
