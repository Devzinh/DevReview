package br.com.devplugins.lang;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages internationalization (i18n) for the plugin.
 * 
 * <p>This class provides multi-language support with automatic fallback handling. Messages
 * are loaded from YAML files and selected based on player locale preferences.</p>
 * 
 * <h2>Supported Languages:</h2>
 * <ul>
 *   <li><b>en_us</b>: English (United States) - Default fallback</li>
 *   <li><b>pt_br</b>: Portuguese (Brazil)</li>
 *   <li><b>ru_ru</b>: Russian (Russia)</li>
 *   <li><b>es_es</b>: Spanish (Spain)</li>
 *   <li><b>fr_fr</b>: French (France)</li>
 *   <li><b>pl_pl</b>: Polish (Poland)</li>
 *   <li><b>de_de</b>: German (Germany)</li>
 * </ul>
 * 
 * <h2>Fallback Chain:</h2>
 * <p>The language resolution follows this priority:</p>
 * <ol>
 *   <li>Exact locale match (e.g., "pt_br")</li>
 *   <li>Language code match (e.g., "pt" → "pt_br")</li>
 *   <li>Default fallback ("en_us")</li>
 * </ol>
 * 
 * <p><b>Example:</b> A player with locale "pt_PT" (Portuguese - Portugal) will receive
 * messages from "pt_br" (closest match), not "en_us".</p>
 * 
 * <h2>Color Code Translation:</h2>
 * <p>All messages automatically convert '&amp;' color codes to '§' section symbols for
 * Minecraft formatting. This allows language files to use the more readable '&amp;' syntax.</p>
 * 
 * <h2>Missing Keys:</h2>
 * <p>If a message key doesn't exist in any language file, the method returns
 * "Missing key: {key}" to help identify configuration issues.</p>
 * 
 * <h2>Console Support:</h2>
 * <p>The console always receives messages in English (en_us) since it has no locale.</p>
 * 
 * <h2>Thread Safety:</h2>
 * <p>This class is thread-safe for read operations. Language files are loaded once during
 * initialization and not modified afterward.</p>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Get message for player (uses player's locale)
 * String msg = languageManager.getMessage(player, "messages.command-staged");
 * 
 * // Get message for console
 * String msg = languageManager.getMessage(console, "messages.plugin-enabled");
 * 
 * // Get message with placeholder replacement
 * String msg = languageManager.getMessage(player, "messages.approval-notification")
 *     .replace("%player%", playerName)
 *     .replace("%command%", commandLine);
 * }</pre>
 * 
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
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

    /**
     * Gets a localized message for a command sender.
     * 
     * <p>This method automatically detects if the sender is a player or console and
     * uses the appropriate locale. Console always receives English messages.</p>
     * 
     * @param sender the command sender (player or console)
     * @param key the message key (e.g., "messages.command-staged")
     * @return the localized message with color codes translated
     */
    public String getMessage(org.bukkit.command.CommandSender sender, String key) {
        if (sender instanceof Player) {
            return getMessage((Player) sender, key);
        }
        return getMessage("en_us", key);
    }

    /**
     * Gets a localized message for a player using their locale.
     * 
     * <p>The player's locale is obtained from {@link Player#getLocale()} and used
     * to select the appropriate language file.</p>
     * 
     * @param player the player
     * @param key the message key (e.g., "messages.command-staged")
     * @return the localized message with color codes translated
     */
    public String getMessage(Player player, String key) {
        String locale = player.getLocale().toLowerCase();
        return getMessage(locale, key);
    }

    /**
     * Gets a localized message for a specific locale.
     * 
     * <p>This method implements the fallback chain:</p>
     * <ol>
     *   <li>Try exact locale match</li>
     *   <li>Try language code match (first 2 characters)</li>
     *   <li>Fall back to en_us</li>
     *   <li>Return "Missing key: {key}" if not found</li>
     * </ol>
     * 
     * <p>All messages have '&amp;' color codes automatically converted to '§'.</p>
     * 
     * @param locale the locale string (e.g., "pt_br", "en_us")
     * @param key the message key (e.g., "messages.command-staged")
     * @return the localized message with color codes translated, or error message if key not found
     */
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
