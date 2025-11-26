package br.com.devplugins.staging;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages command history for players.
 * 
 * <p>This class maintains an in-memory cache of command history for each player, with a
 * configurable maximum size per player. History is persisted to a repository for durability
 * across server restarts.</p>
 * 
 * <h2>History Limit:</h2>
 * <p>Each player can have up to 50 commands in their history. When this limit is exceeded,
 * the oldest command is automatically removed (FIFO queue behavior).</p>
 * 
 * <h2>History Contents:</h2>
 * <p>The history includes all commands that have been approved or rejected, with full details:</p>
 * <ul>
 *   <li>Command text</li>
 *   <li>Timestamp</li>
 *   <li>Status (APPROVED or REJECTED)</li>
 *   <li>Reviewer information</li>
 *   <li>Justification</li>
 * </ul>
 * 
 * <h2>Thread Safety:</h2>
 * <p>This class uses {@link ConcurrentHashMap} for thread-safe access to the history cache.
 * Repository operations are executed asynchronously to avoid blocking the main thread.</p>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Add a command to history
 * historyManager.addToHistory(approvedCommand);
 * 
 * // Get full history for a player
 * List<StagedCommand> history = historyManager.getHistoryForPlayer(playerUUID);
 * 
 * // Get recent history (e.g., last 10 commands)
 * List<StagedCommand> recent = historyManager.getRecentHistoryForPlayer(playerUUID, 10);
 * }</pre>
 * 
 * @see StagedCommand
 * @see CommandHistoryRepository
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
public class CommandHistoryManager {
    private final JavaPlugin plugin;
    private final CommandHistoryRepository repository;
    private final Map<UUID, List<StagedCommand>> commandHistory = new ConcurrentHashMap<>();
    private final int maxHistoryPerPlayer = 50;

    /**
     * Creates a new command history manager.
     * 
     * @param plugin the plugin instance
     * @param repository the repository for persisting history
     */
    public CommandHistoryManager(JavaPlugin plugin, CommandHistoryRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    /**
     * Loads all command history from the repository into memory.
     * 
     * <p>This method should be called during plugin initialization to restore history
     * from persistent storage. The loading is performed asynchronously to avoid blocking
     * the main thread.</p>
     * 
     * <p><b>Thread Safety:</b> This method is thread-safe and can be called from any thread.</p>
     */
    public void loadHistory() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            Map<UUID, List<StagedCommand>> loadedHistory = repository.loadAll();
            commandHistory.putAll(loadedHistory);
            plugin.getLogger().info("Loaded command history for " + loadedHistory.size() + " players");
        });
    }

    /**
     * Adds a command to the player's history.
     * 
     * <p>This method performs the following operations:</p>
     * <ol>
     *   <li>Adds the command to the player's history list</li>
     *   <li>Removes the oldest command if the limit (50) is exceeded</li>
     *   <li>Persists the updated history to the repository (async)</li>
     * </ol>
     * 
     * <p><b>Thread Safety:</b> This method is thread-safe due to the use of ConcurrentHashMap
     * and computeIfAbsent. However, it should typically be called from the main thread to
     * maintain consistency with other game operations.</p>
     * 
     * @param command the command to add to history (should have status APPROVED or REJECTED)
     */
    public void addToHistory(StagedCommand command) {
        UUID senderId = command.getSenderId();
        commandHistory.computeIfAbsent(senderId, k -> new ArrayList<>()).add(command);
        
        List<StagedCommand> history = commandHistory.get(senderId);
        if (history.size() > maxHistoryPerPlayer) {
            history.remove(0);
        }

        // Persist asynchronously
        final List<StagedCommand> historyCopy = new ArrayList<>(history);
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            repository.save(senderId, historyCopy);
        });
    }

    /**
     * Gets the complete command history for a player, sorted by timestamp (newest first).
     * 
     * <p>The returned list is a new list containing commands sorted in descending order
     * by timestamp. If the player has no history, an empty list is returned.</p>
     * 
     * <p><b>Thread Safety:</b> This method is thread-safe and returns a new list to prevent
     * concurrent modification issues.</p>
     * 
     * @param playerId the UUID of the player
     * @return a sorted list of commands (newest first), or empty list if no history
     */
    public List<StagedCommand> getHistoryForPlayer(UUID playerId) {
        return commandHistory.getOrDefault(playerId, new ArrayList<>())
                .stream()
                .sorted(Comparator.comparingLong(StagedCommand::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Gets the most recent commands from a player's history.
     * 
     * <p>This is a convenience method that returns only the N most recent commands,
     * sorted by timestamp (newest first). Useful for displaying recent history in GUIs.</p>
     * 
     * <p><b>Thread Safety:</b> This method is thread-safe and returns a new list.</p>
     * 
     * @param playerId the UUID of the player
     * @param limit the maximum number of commands to return
     * @return a list of up to 'limit' most recent commands, or empty list if no history
     */
    public List<StagedCommand> getRecentHistoryForPlayer(UUID playerId, int limit) {
        return getHistoryForPlayer(playerId).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}

