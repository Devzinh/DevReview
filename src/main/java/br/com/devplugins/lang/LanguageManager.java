package br.com.devplugins.lang;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final JavaPlugin plugin;
    private final Map<String, FileConfiguration> languages = new HashMap<>();
    private final String[] supportedLocales = { "en_us", "pt_br", "ru_ru", "es_es", "fr_fr", "pl_pl", "de_de" };

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadLanguages();
    }

    private void loadLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        for (String locale : supportedLocales) {
            File langFile = new File(langFolder, locale + ".yml");
            if (!langFile.exists()) {
                plugin.saveResource("lang/" + locale + ".yml", false);
            }
            languages.put(locale, YamlConfiguration.loadConfiguration(langFile));
        }
    }

    public String getMessage(org.bukkit.command.CommandSender sender, String key) {
        if (sender instanceof Player) {
            return getMessage((Player) sender, key);
        }
        return getMessage("en_us", key);
    }

    public String getMessage(Player player, String key) {
        String locale = player.getLocale().toLowerCase();
        return getMessage(locale, key);
    }

    public String getMessage(String locale, String key) {
        FileConfiguration config = languages.get(locale);

        if (config == null) {
            if (locale.length() > 2) {
                String langCode = locale.substring(0, 2);
                for (String supported : supportedLocales) {
                    if (supported.startsWith(langCode)) {
                        config = languages.get(supported);
                        break;
                    }
                }
            }
        }

        if (config == null) {
            config = languages.get("en_us");
        }

        if (config != null) {
            String message = config.getString(key);
            if (message != null) {
                return message.replace("&", "§");
            }
        }

        FileConfiguration defaultConfig = languages.get("en_us");
        if (defaultConfig != null) {
            String message = defaultConfig.getString(key);
            if (message != null) {
                return message.replace("&", "§");
            }
        }

        return "Missing key: " + key;
    }
}
