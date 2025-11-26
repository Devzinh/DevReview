package br.com.devplugins.staging;

import java.util.List;
import java.util.UUID;

public interface CommandHistoryRepository {
    /**
     * Save the entire history for a player
     * @param playerId The player's UUID
     * @param history The list of commands in the player's history
     */
    void save(UUID playerId, List<StagedCommand> history);

    /**
     * Load the history for a specific player
     * @param playerId The player's UUID
     * @return The list of commands in the player's history
     */
    List<StagedCommand> load(UUID playerId);

    /**
     * Load all history for all players
     * @return A map of player UUIDs to their command history
     */
    java.util.Map<UUID, List<StagedCommand>> loadAll();

    /**
     * Delete the history for a specific player
     * @param playerId The player's UUID
     */
    void delete(UUID playerId);
}
