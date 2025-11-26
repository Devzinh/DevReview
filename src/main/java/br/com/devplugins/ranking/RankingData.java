package br.com.devplugins.ranking;

import java.util.UUID;

/**
 * Represents ranking statistics for a reviewer.
 * 
 * <p>This class tracks the number of approvals and rejections performed by a reviewer,
 * providing data for the ranking leaderboard system.</p>
 * 
 * <h2>Immutable Fields:</h2>
 * <ul>
 *   <li><b>uuid</b>: The reviewer's unique identifier</li>
 * </ul>
 * 
 * <h2>Mutable Fields:</h2>
 * <ul>
 *   <li><b>name</b>: The reviewer's current name (updated on each action)</li>
 *   <li><b>approvals</b>: Total number of commands approved</li>
 *   <li><b>rejections</b>: Total number of commands rejected</li>
 * </ul>
 * 
 * <h2>Ranking Order:</h2>
 * <p>Reviewers are ranked by approval count in descending order. This encourages
 * active participation in the review process.</p>
 * 
 * <h2>Thread Safety:</h2>
 * <p>This class is NOT thread-safe. Instances should only be modified from the main thread.</p>
 * 
 * @see RankingManager
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
public class RankingData {
    private final UUID uuid;
    private String name;
    private int approvals;
    private int rejections;

    /**
     * Creates new ranking data with zero statistics.
     * 
     * @param uuid the reviewer's UUID
     * @param name the reviewer's name
     */
    public RankingData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.approvals = 0;
        this.rejections = 0;
    }

    /**
     * Creates ranking data with existing statistics.
     * 
     * <p>This constructor is used when loading data from persistent storage.</p>
     * 
     * @param uuid the reviewer's UUID
     * @param name the reviewer's name
     * @param approvals the number of approvals
     * @param rejections the number of rejections
     */
    public RankingData(UUID uuid, String name, int approvals, int rejections) {
        this.uuid = uuid;
        this.name = name;
        this.approvals = approvals;
        this.rejections = rejections;
    }

    /**
     * Gets the reviewer's UUID.
     * 
     * @return the UUID
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Gets the reviewer's current name.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Updates the reviewer's name.
     * 
     * <p>This is called on each action to keep the name current in case of player renames.</p>
     * 
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the total number of approvals.
     * 
     * @return the approval count
     */
    public int getApprovals() {
        return approvals;
    }

    /**
     * Increments the approval count by one.
     * 
     * <p>This method is called when the reviewer approves a command.</p>
     */
    public void addApproval() {
        this.approvals++;
    }

    /**
     * Gets the total number of rejections.
     * 
     * @return the rejection count
     */
    public int getRejections() {
        return rejections;
    }

    /**
     * Increments the rejection count by one.
     * 
     * <p>This method is called when the reviewer rejects a command.</p>
     */
    public void addRejection() {
        this.rejections++;
    }
}
