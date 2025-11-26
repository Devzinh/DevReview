package br.com.devplugins.commands;

import br.com.devplugins.DevReview;
import br.com.devplugins.lang.LanguageManager;
import br.com.devplugins.metrics.MetricsManager;
import br.com.devplugins.staging.RetryableRepository;
import br.com.devplugins.staging.StagedCommandRepository;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Command executor for displaying system metrics and statistics.
 * 
 * <p>This command displays comprehensive statistics about the DevReview system, including:</p>
 * <ul>
 *   <li>Total commands staged, approved, and rejected</li>
 *   <li>Average review time</li>
 *   <li>Approval and rejection rates</li>
 * </ul>
 * 
 * <p><b>Usage:</b> /devreview stats</p>
 * <p><b>Permission:</b> devreview.stats</p>
 * 
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
public class StatsCommand implements CommandExecutor {

    private final MetricsManager metricsManager;
    private final LanguageManager languageManager;
    private final DevReview plugin;

    public StatsCommand(MetricsManager metricsManager, LanguageManager languageManager, DevReview plugin) {
        this.metricsManager = metricsManager;
        this.languageManager = languageManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("devreview.stats")) {
            sender.sendMessage(languageManager.getMessage(sender, "messages.no-permission"));
            return true;
        }

        // Send header
        sender.sendMessage(languageManager.getMessage(sender, "messages.stats-header"));
        
        // Send command counts
        String stagedMsg = languageManager.getMessage(sender, "messages.stats-staged")
                .replace("%count%", String.valueOf(metricsManager.getStagedCount()));
        sender.sendMessage(stagedMsg);
        
        String approvedMsg = languageManager.getMessage(sender, "messages.stats-approved")
                .replace("%count%", String.valueOf(metricsManager.getApprovedCount()));
        sender.sendMessage(approvedMsg);
        
        String rejectedMsg = languageManager.getMessage(sender, "messages.stats-rejected")
                .replace("%count%", String.valueOf(metricsManager.getRejectedCount()));
        sender.sendMessage(rejectedMsg);
        
        // Send average review time
        String avgTimeMsg = languageManager.getMessage(sender, "messages.stats-avg-time")
                .replace("%time%", metricsManager.getFormattedAverageReviewTime());
        sender.sendMessage(avgTimeMsg);
        
        // Send approval rate
        String approvalRateMsg = languageManager.getMessage(sender, "messages.stats-approval-rate")
                .replace("%rate%", String.format("%.1f", metricsManager.getApprovalRate()));
        sender.sendMessage(approvalRateMsg);
        
        // Send rejection rate
        String rejectionRateMsg = languageManager.getMessage(sender, "messages.stats-rejection-rate")
                .replace("%rate%", String.format("%.1f", metricsManager.getRejectionRate()));
        sender.sendMessage(rejectionRateMsg);
        
        // Send footer
        sender.sendMessage(languageManager.getMessage(sender, "messages.stats-footer"));
        
        // Send circuit breaker status if repository is retryable
        StagedCommandRepository repo = plugin.getRepository();
        if (repo instanceof RetryableRepository) {
            RetryableRepository retryRepo = (RetryableRepository) repo;
            sender.sendMessage("§e§l[Circuit Breaker Status]");
            sender.sendMessage("§7Consecutive Failures: §f" + retryRepo.getConsecutiveFailures());
            sender.sendMessage("§7Circuit Open: §f" + (retryRepo.isCircuitOpen() ? "§cYes" : "§aNo"));
        }
        
        return true;
    }
}
