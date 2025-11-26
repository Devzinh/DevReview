package br.com.devplugins.staging;

import java.util.UUID;

/**
 * Represents a command that has been intercepted and placed in the review queue.
 * 
 * <p>This class is the core data model for the staging system. Each staged command contains
 * all information needed to track, review, and execute (or reject) a command.</p>
 * 
 * <h2>Immutable Fields:</h2>
 * <p>The following fields are immutable and set at creation time:</p>
 * <ul>
 *   <li><b>id</b>: Unique identifier (UUID) for this command</li>
 *   <li><b>senderId</b>: UUID of the player who issued the command</li>
 *   <li><b>senderName</b>: Name of the player at the time of staging</li>
 *   <li><b>commandLine</b>: The full command text (with or without leading slash)</li>
 *   <li><b>timestamp</b>: Unix timestamp (milliseconds) when command was staged</li>
 * </ul>
 * 
 * <h2>Mutable Fields:</h2>
 * <p>The following fields are set during the review process:</p>
 * <ul>
 *   <li><b>justification</b>: Reason provided by reviewer for approval/rejection</li>
 *   <li><b>reviewerId</b>: UUID of the player who reviewed the command</li>
 *   <li><b>reviewerName</b>: Name of the reviewer at the time of review</li>
 *   <li><b>status</b>: Current status (PENDING, APPROVED, REJECTED)</li>
 * </ul>
 * 
 * <h2>Lifecycle:</h2>
 * <ol>
 *   <li>Created with status PENDING</li>
 *   <li>Persisted to repository</li>
 *   <li>Displayed in review GUI</li>
 *   <li>Reviewer provides justification</li>
 *   <li>Status updated to APPROVED or REJECTED</li>
 *   <li>Reviewer information recorded</li>
 *   <li>Added to command history</li>
 *   <li>Removed from repository</li>
 * </ol>
 * 
 * <h2>Thread Safety:</h2>
 * <p>This class is NOT thread-safe. Instances should only be modified from the main thread.
 * The immutable fields provide safe read access from any thread.</p>
 * 
 * @see StagingManager
 * @see StagedCommandRepository
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
public class StagedCommand {
    private final UUID id;
    private final UUID senderId;
    private final String senderName;
    private final String commandLine;
    private final long timestamp;
    private String justification;
    private UUID reviewerId;
    private String reviewerName;
    private Status status;

    /**
     * Represents the current status of a staged command.
     */
    public enum Status {
        /** Command is awaiting review */
        PENDING,
        /** Command has been approved and executed */
        APPROVED,
        /** Command has been rejected and not executed */
        REJECTED
    }

    /**
     * Creates a new staged command with a generated UUID and current timestamp.
     * 
     * <p>This constructor is used when a command is first intercepted and staged.
     * The command is created with status PENDING and no reviewer information.</p>
     * 
     * @param senderId the UUID of the player who issued the command
     * @param senderName the name of the player at the time of staging
     * @param commandLine the full command text (with or without leading slash)
     */
    public StagedCommand(UUID senderId, String senderName, String commandLine) {
        this.id = UUID.randomUUID();
        this.senderId = senderId;
        this.senderName = senderName;
        this.commandLine = commandLine;
        this.timestamp = System.currentTimeMillis();
        this.status = Status.PENDING;
    }

    /**
     * Creates a staged command from persisted data.
     * 
     * <p>This constructor is used when loading commands from the repository (JSON or SQL).
     * All fields are provided explicitly, including the original UUID and timestamp.</p>
     * 
     * @param id the unique identifier for this command
     * @param senderId the UUID of the player who issued the command
     * @param senderName the name of the player at the time of staging
     * @param commandLine the full command text
     * @param timestamp the Unix timestamp (milliseconds) when command was staged
     * @param justification the justification provided by the reviewer (may be null)
     */
    public StagedCommand(UUID id, UUID senderId, String senderName, String commandLine, long timestamp,
            String justification) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.commandLine = commandLine;
        this.timestamp = timestamp;
        this.justification = justification;
        this.status = Status.PENDING;
    }

    /**
     * Gets the unique identifier for this command.
     * 
     * @return the command UUID
     */
    /**
     * Gets the unique identifier for this command.
     * 
     * @return the command UUID
     */
    public UUID getId() {
        return id;
    }

    /**
     * Gets the UUID of the player who issued this command.
     * 
     * @return the sender's UUID
     */
    public UUID getSenderId() {
        return senderId;
    }

    /**
     * Gets the name of the player who issued this command.
     * 
     * @return the sender's name at the time of staging
     */
    public String getSenderName() {
        return senderName;
    }

    /**
     * Gets the full command text.
     * 
     * @return the command line (with or without leading slash)
     */
    public String getCommandLine() {
        return commandLine;
    }

    /**
     * Gets the timestamp when this command was staged.
     * 
     * @return Unix timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the justification provided by the reviewer.
     * 
     * @return the justification text, or null if not yet provided
     */
    public String getJustification() {
        return justification;
    }

    /**
     * Sets the justification for approving or rejecting this command.
     * 
     * @param justification the reason for the decision
     */
    public void setJustification(String justification) {
        this.justification = justification;
    }

    /**
     * Gets the UUID of the player who reviewed this command.
     * 
     * @return the reviewer's UUID, or null if not yet reviewed
     */
    public UUID getReviewerId() {
        return reviewerId;
    }

    /**
     * Sets the UUID of the player reviewing this command.
     * 
     * @param reviewerId the reviewer's UUID
     */
    public void setReviewerId(UUID reviewerId) {
        this.reviewerId = reviewerId;
    }

    /**
     * Gets the name of the player who reviewed this command.
     * 
     * @return the reviewer's name, or null if not yet reviewed
     */
    public String getReviewerName() {
        return reviewerName;
    }

    /**
     * Sets the name of the player reviewing this command.
     * 
     * @param reviewerName the reviewer's name
     */
    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    /**
     * Gets the current status of this command.
     * 
     * @return the command status (PENDING, APPROVED, or REJECTED)
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the status of this command.
     * 
     * @param status the new status
     */
    public void setStatus(Status status) {
        this.status = status;
    }
}
