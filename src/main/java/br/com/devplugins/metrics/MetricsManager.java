package br.com.devplugins.metrics;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Manager for tracking system metrics and statistics.
 * 
 * <p>This class provides comprehensive metrics tracking for the DevReview system, including:</p>
 * <ul>
 *   <li>Command counts (staged, approved, rejected)</li>
 *   <li>Average review time calculation</li>
 *   <li>Approval and rejection rates</li>
 * </ul>
 * 
 * <h2>Thread Safety:</h2>
 * <p>All counters use atomic operations to ensure thread-safe updates without explicit synchronization.</p>
 * 
 * <h2>Metrics Tracked:</h2>
 * <ul>
 *   <li><b>Staged Commands</b>: Total number of commands placed in review queue</li>
 *   <li><b>Approved Commands</b>: Total number of commands approved and executed</li>
 *   <li><b>Rejected Commands</b>: Total number of commands rejected</li>
 *   <li><b>Total Review Time</b>: Cumulative time spent reviewing commands (milliseconds)</li>
 *   <li><b>Reviewed Commands</b>: Total number of commands that have been reviewed (approved + rejected)</li>
 * </ul>
 * 
 * <h2>Calculated Metrics:</h2>
 * <ul>
 *   <li><b>Average Review Time</b>: Total review time / reviewed commands</li>
 *   <li><b>Approval Rate</b>: (Approved / Reviewed) * 100</li>
 *   <li><b>Rejection Rate</b>: (Rejected / Reviewed) * 100</li>
 * </ul>
 * 
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
public class MetricsManager {

    private final JavaPlugin plugin;
    
    // Atomic counters for thread-safe operations
    private final AtomicInteger stagedCount = new AtomicInteger(0);
    private final AtomicInteger approvedCount = new AtomicInteger(0);
    private final AtomicInteger rejectedCount = new AtomicInteger(0);
    private final AtomicLong totalReviewTimeMs = new AtomicLong(0);
    private final AtomicInteger reviewedCount = new AtomicInteger(0);

    public MetricsManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Records that a command was staged for review.
     * 
     * <p>This method should be called when a command is added to the pending queue.</p>
     */
    public void recordStaged() {
        stagedCount.incrementAndGet();
    }

    /**
     * Records that a command was approved.
     * 
     * <p>This method should be called when a command is approved and executed.</p>
     * 
     * @param reviewTimeMs the time taken to review the command in milliseconds
     */
    public void recordApproved(long reviewTimeMs) {
        approvedCount.incrementAndGet();
        reviewedCount.incrementAndGet();
        totalReviewTimeMs.addAndGet(reviewTimeMs);
    }

    /**
     * Records that a command was rejected.
     * 
     * <p>This method should be called when a command is rejected and discarded.</p>
     * 
     * @param reviewTimeMs the time taken to review the command in milliseconds
     */
    public void recordRejected(long reviewTimeMs) {
        rejectedCount.incrementAndGet();
        reviewedCount.incrementAndGet();
        totalReviewTimeMs.addAndGet(reviewTimeMs);
    }

    /**
     * Gets the total number of commands staged for review.
     * 
     * @return the total staged command count
     */
    public int getStagedCount() {
        return stagedCount.get();
    }

    /**
     * Gets the total number of commands approved.
     * 
     * @return the total approved command count
     */
    public int getApprovedCount() {
        return approvedCount.get();
    }

    /**
     * Gets the total number of commands rejected.
     * 
     * @return the total rejected command count
     */
    public int getRejectedCount() {
        return rejectedCount.get();
    }

    /**
     * Gets the total number of commands reviewed (approved + rejected).
     * 
     * @return the total reviewed command count
     */
    public int getReviewedCount() {
        return reviewedCount.get();
    }

    /**
     * Calculates the average review time in milliseconds.
     * 
     * <p>If no commands have been reviewed, returns 0.</p>
     * 
     * @return the average review time in milliseconds
     */
    public long getAverageReviewTimeMs() {
        int reviewed = reviewedCount.get();
        if (reviewed == 0) {
            return 0;
        }
        return totalReviewTimeMs.get() / reviewed;
    }

    /**
     * Calculates the approval rate as a percentage.
     * 
     * <p>Formula: (approved / reviewed) * 100</p>
     * <p>If no commands have been reviewed, returns 0.0.</p>
     * 
     * @return the approval rate as a percentage (0.0 to 100.0)
     */
    public double getApprovalRate() {
        int reviewed = reviewedCount.get();
        if (reviewed == 0) {
            return 0.0;
        }
        return (approvedCount.get() * 100.0) / reviewed;
    }

    /**
     * Calculates the rejection rate as a percentage.
     * 
     * <p>Formula: (rejected / reviewed) * 100</p>
     * <p>If no commands have been reviewed, returns 0.0.</p>
     * 
     * @return the rejection rate as a percentage (0.0 to 100.0)
     */
    public double getRejectionRate() {
        int reviewed = reviewedCount.get();
        if (reviewed == 0) {
            return 0.0;
        }
        return (rejectedCount.get() * 100.0) / reviewed;
    }

    /**
     * Formats the average review time as a human-readable string.
     * 
     * <p>Converts milliseconds to seconds, minutes, or hours as appropriate.</p>
     * 
     * @return formatted review time string (e.g., "45s", "2m 30s", "1h 15m")
     */
    public String getFormattedAverageReviewTime() {
        long ms = getAverageReviewTimeMs();
        if (ms == 0) {
            return "N/A";
        }
        
        long seconds = ms / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        if (minutes < 60) {
            return minutes + "m " + remainingSeconds + "s";
        }
        
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        return hours + "h " + remainingMinutes + "m";
    }
}
