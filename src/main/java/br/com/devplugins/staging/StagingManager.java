package br.com.devplugins.staging;

import br.com.devplugins.audit.AuditManager;
import br.com.devplugins.events.CommandApprovedEvent;
import br.com.devplugins.events.CommandRejectedEvent;
import br.com.devplugins.events.CommandStagedEvent;
import br.com.devplugins.metrics.MetricsManager;
import br.com.devplugins.ranking.RankingManager;
import br.com.devplugins.rules.RulesEngine;
import br.com.devplugins.utils.CommandValidator;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Core manager for the command staging and review system.
 * 
 * <p>This class coordinates the entire lifecycle of staged commands, from initial interception
 * through approval/rejection to execution. It integrates with multiple subsystems including
 * persistence, audit logging, notifications, ranking, and rule evaluation.</p>
 * 
 * <h2>Command Lifecycle:</h2>
 * <ol>
 *   <li><b>Staging</b>: Command is intercepted and added to pending queue</li>
 *   <li><b>Persistence</b>: Command is saved to repository (async)</li>
 *   <li><b>Review</b>: Administrator reviews command via GUI</li>
 *   <li><b>Decision</b>: Command is approved or rejected with justification</li>
 *   <li><b>Execution/Discard</b>: Approved commands execute, rejected commands are discarded</li>
 *   <li><b>History</b>: Command is added to sender's history</li>
 *   <li><b>Cleanup</b>: Command is removed from pending queue and repository</li>
 * </ol>
 * 
 * <h2>Auto-Approval:</h2>
 * <p>Commands can be automatically approved based on time-window rules configured in rules.yml.
 * When auto-approval is active, commands bypass the staging queue and execute immediately.</p>
 * 
 * <h2>Expiration:</h2>
 * <p>Commands that remain in the pending queue beyond the configured expiration duration are
 * automatically removed. The {@link #pruneExpiredCommands()} method should be called periodically.</p>
 * 
 * <h2>Thread Safety:</h2>
 * <p>This class uses {@link java.util.concurrent.CopyOnWriteArrayList} for the pending commands
 * list, providing thread-safe iteration without explicit synchronization. Repository operations
 * are executed asynchronously to avoid blocking the main thread.</p>
 * 
 * <h2>Example Usage:</h2>
 * <pre>{@code
 * // Stage a command for review
 * stagingManager.stageCommand(player, "/op PlayerName");
 * 
 * // Approve a command
 * StagedCommand command = stagingManager.getPendingCommands().get(0);
 * stagingManager.approveCommand(command, reviewer);
 * 
 * // Reject a command
 * stagingManager.rejectCommand(command, reviewer);
 * 
 * // Clean up expired commands
 * stagingManager.pruneExpiredCommands();
 * }</pre>
 * 
 * @see StagedCommand
 * @see StagedCommandRepository
 * @see CommandHistoryManager
 * @see RulesEngine
 * @author DevPlugins
 * @version 1.0
 * @since 1.0
 */
public class StagingManager {

    private final JavaPlugin plugin;
    private final StagedCommandRepository repository;
    private final br.com.devplugins.lang.LanguageManager languageManager;
    private final AuditManager auditManager;
    private final RulesEngine rulesEngine;
    private final RankingManager rankingManager;
    private final MetricsManager metricsManager;
    // Thread-safe list for pending commands, though most operations should happen
    // on the main thread.
    private final List<StagedCommand> pendingCommands;
    private final CommandHistoryManager historyManager;

    public StagingManager(JavaPlugin plugin,
            StagedCommandRepository repository,
            CommandHistoryRepository historyRepository,
            br.com.devplugins.lang.LanguageManager languageManager,
            AuditManager auditManager,
            RulesEngine rulesEngine,
            RankingManager rankingManager,
            MetricsManager metricsManager) {
        this.plugin = plugin;
        this.repository = repository;
        this.languageManager = languageManager;
        this.auditManager = auditManager;
        this.rulesEngine = rulesEngine;
        this.rankingManager = rankingManager;
        this.metricsManager = metricsManager;
        // Use CopyOnWriteArrayList for thread safety as requested
        this.pendingCommands = new java.util.concurrent.CopyOnWriteArrayList<>();
        this.historyManager = new CommandHistoryManager(plugin, historyRepository);
        loadCommands();
        loadHistory();
    }

    private void loadHistory() {
        historyManager.loadHistory();
    }

    /**
     * Gets the command history manager for accessing player command history.
     * 
     * @return the command history manager instance
     */
    public CommandHistoryManager getHistoryManager() {
        return historyManager;
    }

    /**
     * Stages a command for review with auto-approval enabled.
     * 
     * <p>This is a convenience method that calls {@link #stageCommand(CommandSender, String, boolean)}
     * with allowAutoApprove set to true.</p>
     * 
     * @param sender the command sender (player or console)
     * @param commandLine the full command line to stage
     * @see #stageCommand(CommandSender, String, boolean)
     */
    public void stageCommand(CommandSender sender, String commandLine) {
        stageCommand(sender, commandLine, true);
    }

    /**
     * Stages a command for review with optional auto-approval.
     * 
     * <p>This method performs the following operations:</p>
     * <ol>
     *   <li>Validates the command is not empty and has valid syntax</li>
     *   <li>Checks auto-approval rules if enabled</li>
     *   <li>If auto-approved: executes immediately and fires approval event</li>
     *   <li>If not auto-approved: adds to pending queue, persists to repository, and fires staging event</li>
     * </ol>
     * 
     * <p><b>Thread Safety:</b> This method is thread-safe. Repository operations are executed
     * asynchronously on a separate thread.</p>
     * 
     * <p><b>Validation:</b> Commands are validated using {@link CommandValidator} before staging.
     * Invalid commands are rejected with an error message to the sender.</p>
     * 
     * @param sender the command sender (player or console)
     * @param commandLine the full command line to stage (with or without leading slash)
     * @param allowAutoApprove whether to check auto-approval rules
     * @see CommandValidator
     * @see RulesEngine#shouldAutoApprove()
     */
    public void stageCommand(CommandSender sender, String commandLine, boolean allowAutoApprove) {
        // Validate command is not empty
        if (!CommandValidator.isNotEmpty(commandLine)) {
            String msg = languageManager.getMessage(sender, "messages.command-invalid-empty");
            sender.sendMessage(msg);
            return;
        }

        // Validate command has valid syntax
        if (!CommandValidator.hasValidSyntax(commandLine)) {
            String msg = languageManager.getMessage(sender, "messages.command-invalid-syntax");
            sender.sendMessage(msg);
            return;
        }

        UUID senderId;
        String senderName;

        if (sender instanceof Player) {
            senderId = ((Player) sender).getUniqueId();
            senderName = sender.getName();
        } else {
            senderId = UUID.nameUUIDFromBytes("CONSOLE".getBytes());
            senderName = "CONSOLE";
        }

        StagedCommand command = new StagedCommand(senderId, senderName, commandLine);

        // Check auto-approve rule only if allowed
        if (allowAutoApprove && rulesEngine.shouldAutoApprove()) {
            executeCommand(command);
            auditManager.log("approve", "Auto-Approve", "Command auto-approved by rules: " + commandLine);
            // Fire approval event
            Bukkit.getPluginManager().callEvent(new CommandApprovedEvent(command));
            return;
        }

        pendingCommands.add(command);

        // Async save
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            repository.save(command);
        });

        auditManager.log("stage", senderName, "Command staged: " + commandLine);
        
        // Record metrics
        metricsManager.recordStaged();

        String msg = languageManager.getMessage(sender, "messages.command-staged");
        sender.sendMessage(msg.replace("%command%", commandLine));

        // Fire staging event
        Bukkit.getPluginManager().callEvent(new CommandStagedEvent(command));
    }

    /**
     * Approves a staged command and executes it.
     * 
     * <p>This method performs the following atomic operations:</p>
     * <ol>
     *   <li>Updates command status to APPROVED</li>
     *   <li>Records reviewer information</li>
     *   <li>Updates reviewer's ranking statistics</li>
     *   <li>Executes the command</li>
     *   <li>Removes from pending queue</li>
     *   <li>Adds to command history</li>
     *   <li>Deletes from repository (async)</li>
     *   <li>Logs to audit system</li>
     *   <li>Fires approval event for notifications</li>
     * </ol>
     * 
     * <p><b>Execution Context:</b> If the original sender is online, the command executes
     * as that player. If offline, it executes as console.</p>
     * 
     * <p><b>Thread Safety:</b> This method should be called from the main thread. Repository
     * operations are executed asynchronously.</p>
     * 
     * @param command the staged command to approve
     * @param reviewer the player approving the command (null for system approval)
     * @see #executeCommand(StagedCommand)
     * @see RankingManager#addApproval(UUID, String)
     */
    public void approveCommand(StagedCommand command, Player reviewer) {
        if (!pendingCommands.contains(command))
            return;

        command.setStatus(StagedCommand.Status.APPROVED);
        if (reviewer != null) {
            command.setReviewerId(reviewer.getUniqueId());
            command.setReviewerName(reviewer.getName());
            rankingManager.addApproval(reviewer.getUniqueId(), reviewer.getName());
        }

        executeCommand(command);

        pendingCommands.remove(command);
        historyManager.addToHistory(command);

        // Async delete
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            repository.delete(command);
        });

        String reviewerName = reviewer != null ? reviewer.getName() : "System";
        auditManager.log("approve", reviewerName, "Command approved: " + command.getCommandLine());
        
        // Record metrics - calculate review time
        long reviewTimeMs = System.currentTimeMillis() - command.getTimestamp();
        metricsManager.recordApproved(reviewTimeMs);
        
        // Fire approval event
        Bukkit.getPluginManager().callEvent(new CommandApprovedEvent(command));
    }

    /**
     * Rejects a staged command without executing it.
     * 
     * <p>This method performs the following atomic operations:</p>
     * <ol>
     *   <li>Updates command status to REJECTED</li>
     *   <li>Records reviewer information</li>
     *   <li>Updates reviewer's ranking statistics</li>
     *   <li>Removes from pending queue (command is NOT executed)</li>
     *   <li>Adds to command history</li>
     *   <li>Deletes from repository (async)</li>
     *   <li>Logs to audit system</li>
     *   <li>Fires rejection event for notifications</li>
     * </ol>
     * 
     * <p><b>Important:</b> Rejected commands are never executed. They are only recorded
     * in the history for audit purposes.</p>
     * 
     * <p><b>Thread Safety:</b> This method should be called from the main thread. Repository
     * operations are executed asynchronously.</p>
     * 
     * @param command the staged command to reject
     * @param reviewer the player rejecting the command (null for system rejection)
     * @see RankingManager#addRejection(UUID, String)
     */
    public void rejectCommand(StagedCommand command, Player reviewer) {
        if (!pendingCommands.contains(command))
            return;

        command.setStatus(StagedCommand.Status.REJECTED);
        if (reviewer != null) {
            command.setReviewerId(reviewer.getUniqueId());
            command.setReviewerName(reviewer.getName());
            rankingManager.addRejection(reviewer.getUniqueId(), reviewer.getName());
        }

        pendingCommands.remove(command);
        historyManager.addToHistory(command);

        // Async delete
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            repository.delete(command);
        });

        String reviewerName = reviewer != null ? reviewer.getName() : "System";
        auditManager.log("reject", reviewerName, "Command rejected: " + command.getCommandLine());
        
        // Record metrics - calculate review time
        long reviewTimeMs = System.currentTimeMillis() - command.getTimestamp();
        metricsManager.recordRejected(reviewTimeMs);
        
        // Fire rejection event
        Bukkit.getPluginManager().callEvent(new CommandRejectedEvent(command));
    }

    private void executeCommand(StagedCommand command) {
        Player sender = Bukkit.getPlayer(command.getSenderId());
        String cmdToRun = command.getCommandLine();
        if (cmdToRun.startsWith("/")) {
            cmdToRun = cmdToRun.substring(1);
        }

        if (sender != null && sender.isOnline()) {
            Bukkit.dispatchCommand(sender, cmdToRun);
            plugin.getLogger().info(
                    "Executed staged command for online player " + sender.getName() + ": " + command.getCommandLine());
        } else {
            plugin.getLogger().warning("Executing staged command for OFFLINE player " + command.getSenderName() + ": "
                    + command.getCommandLine());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmdToRun);
        }
    }

    /**
     * Gets a copy of all pending commands after pruning expired ones.
     * 
     * <p>This method automatically calls {@link #pruneExpiredCommands()} before returning
     * the list to ensure expired commands are not included.</p>
     * 
     * <p><b>Thread Safety:</b> Returns a new ArrayList copy to prevent concurrent modification
     * issues. The underlying list uses CopyOnWriteArrayList for thread-safe iteration.</p>
     * 
     * @return a new list containing all non-expired pending commands
     * @see #pruneExpiredCommands()
     */
    public List<StagedCommand> getPendingCommands() {
        pruneExpiredCommands();
        return new ArrayList<>(pendingCommands);
    }

    /**
     * Removes expired commands from the pending queue.
     * 
     * <p>This method checks each pending command against the expiration rules configured
     * in rules.yml. Expired commands are:</p>
     * <ul>
     *   <li>Removed from the pending queue</li>
     *   <li>Deleted from the repository (async)</li>
     *   <li>Logged to the audit system</li>
     * </ul>
     * 
     * <p><b>Scheduling:</b> This method should be called periodically (e.g., every 5 minutes)
     * via a Bukkit scheduler task. It is also called automatically by {@link #getPendingCommands()}.</p>
     * 
     * <p><b>Thread Safety:</b> This method is thread-safe due to the use of CopyOnWriteArrayList
     * and removeIf operation.</p>
     * 
     * @see RulesEngine#isExpired(StagedCommand)
     */
    public void pruneExpiredCommands() {
        // Check for expired commands
        pendingCommands.removeIf(cmd -> {
            if (rulesEngine.isExpired(cmd)) {
                // Async delete
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    repository.delete(cmd);
                });
                auditManager.log("expire", "System", "Command expired: " + cmd.getCommandLine());
                return true;
            }
            return false;
        });
    }

    private void loadCommands() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<StagedCommand> loaded = repository.loadAll();
            Bukkit.getScheduler().runTask(plugin, () -> {
                pendingCommands.clear();
                pendingCommands.addAll(loaded);
            });
        });
    }
}
