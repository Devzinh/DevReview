package br.com.devplugins.staging;

import br.com.devplugins.audit.AuditManager;
import br.com.devplugins.notifications.NotificationManager;
import br.com.devplugins.rules.RulesEngine;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StagingManager {

    private final JavaPlugin plugin;
    private final StagedCommandRepository repository;
    private final br.com.devplugins.lang.LanguageManager languageManager;
    private final AuditManager auditManager;
    private final NotificationManager notificationManager;
    private final RulesEngine rulesEngine;
    private final List<StagedCommand> pendingCommands;

    public StagingManager(JavaPlugin plugin,
            StagedCommandRepository repository,
            br.com.devplugins.lang.LanguageManager languageManager,
            AuditManager auditManager,
            NotificationManager notificationManager,
            RulesEngine rulesEngine) {
        this.plugin = plugin;
        this.repository = repository;
        this.languageManager = languageManager;
        this.auditManager = auditManager;
        this.notificationManager = notificationManager;
        this.rulesEngine = rulesEngine;
        this.pendingCommands = new ArrayList<>();
        loadCommands();
    }

    public void stageCommand(CommandSender sender, String commandLine) {
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

        // Check auto-approve rule
        if (rulesEngine.shouldAutoApprove()) {
            executeCommand(command);
            auditManager.log("approve", "Auto-Approve", "Command auto-approved by rules: " + commandLine);
            notificationManager.notifyApproval(command);
            return;
        }

        pendingCommands.add(command);

        // Async save
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            repository.save(command);
        });

        auditManager.log("stage", senderName, "Command staged: " + commandLine);

        String msg = languageManager.getMessage(sender, "messages.command-staged");
        sender.sendMessage(msg.replace("%command%", commandLine));

        notificationManager.notifyStaging(command);
    }

    public void approveCommand(StagedCommand command) {
        if (!pendingCommands.contains(command))
            return;

        executeCommand(command);

        pendingCommands.remove(command);

        // Async delete
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            repository.delete(command);
        });

        auditManager.log("approve", "Reviewer", "Command approved: " + command.getCommandLine());
        notificationManager.notifyApproval(command);
    }

    public void rejectCommand(StagedCommand command) {
        if (!pendingCommands.contains(command))
            return;

        pendingCommands.remove(command);

        // Async delete
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            repository.delete(command);
        });

        auditManager.log("reject", "Reviewer", "Command rejected: " + command.getCommandLine());
        notificationManager.notifyRejection(command);
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

    public List<StagedCommand> getPendingCommands() {
        pruneExpiredCommands();
        return new ArrayList<>(pendingCommands);
    }

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
