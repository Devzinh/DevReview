package br.com.devplugins.staging;

import java.util.List;

/**
 * Repository interface for persisting staged commands.
 * 
 * <p>This interface abstracts the persistence layer, allowing for multiple implementations
 * (JSON file storage, SQL database, etc.). The repository pattern provides a clean separation
 * between business logic and data access.</p>
 * 
 * <h2>Implementations:</h2>
 * <ul>
 *   <li><b>JsonStagedCommandRepository</b>: File-based storage using JSON serialization</li>
 *   <li><b>SqlStagedCommandRepository</b>: Database storage using JDBC (MySQL/MariaDB)</li>
 * </ul>
 * 
 * <h2>Thread Safety:</h2>
 * <p>Implementations should be thread-safe or designed to be called from async tasks.
 * The StagingManager calls these methods asynchronously to avoid blocking the main thread.</p>
 * 
 * <h2>Data Persistence:</h2>
 * <p>The following fields must be persisted:</p>
 * <ul>
 *   <li>id (UUID)</li>
 *   <li>senderId (UUID)</li>
 *   <li>senderName (String)</li>
 *   <li>commandLine (String)</li>
 *   <li>timestamp (long)</li>
 *   <li>justification (String, nullable)</li>
 * </ul>
 * 
 * @see StagedCommand
 * @see br.com.devplugins.staging.JsonStagedCommandRepository
 * @see br.com.devplugins.staging.SqlStagedCommandRepository
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
public interface StagedCommandRepository {
    /**
     * Persists a staged command to the repository.
     * 
     * <p>If a command with the same ID already exists, it should be updated.
     * This method should be called asynchronously to avoid blocking the main thread.</p>
     * 
     * @param command the command to save
     */
    void save(StagedCommand command);

    /**
     * Removes a staged command from the repository.
     * 
     * <p>This method is called when a command is approved or rejected and no longer
     * needs to be persisted. If the command doesn't exist, this method should
     * complete without error.</p>
     * 
     * <p>This method should be called asynchronously to avoid blocking the main thread.</p>
     * 
     * @param command the command to delete
     */
    void delete(StagedCommand command);

    /**
     * Loads all staged commands from the repository.
     * 
     * <p>This method is called during plugin initialization to restore the pending
     * command queue. It should return an empty list if no commands are persisted.</p>
     * 
     * <p>This method should be called asynchronously to avoid blocking the main thread.</p>
     * 
     * @return a list of all persisted staged commands
     */
    List<StagedCommand> loadAll();
    
    /**
     * Persists multiple staged commands to the repository in a batch operation.
     * 
     * <p>This method provides better performance than calling save() multiple times
     * by reducing the number of I/O operations or database transactions.</p>
     * 
     * <p>Default implementation calls save() for each command. Implementations should
     * override this method to provide optimized batch operations.</p>
     * 
     * @param commands the list of commands to save
     */
    default void saveAll(List<StagedCommand> commands) {
        for (StagedCommand command : commands) {
            save(command);
        }
    }
    
    /**
     * Removes multiple staged commands from the repository in a batch operation.
     * 
     * <p>This method provides better performance than calling delete() multiple times
     * by reducing the number of I/O operations or database transactions.</p>
     * 
     * <p>Default implementation calls delete() for each command. Implementations should
     * override this method to provide optimized batch operations.</p>
     * 
     * @param commands the list of commands to delete
     */
    default void deleteAll(List<StagedCommand> commands) {
        for (StagedCommand command : commands) {
            delete(command);
        }
    }
}
