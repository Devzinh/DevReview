package br.com.devplugins.listener;

import br.com.devplugins.events.CommandApprovedEvent;
import br.com.devplugins.events.CommandRejectedEvent;
import br.com.devplugins.events.CommandStagedEvent;
import br.com.devplugins.notifications.NotificationManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listener for staging events that triggers notifications.
 * This decouples StagingManager from NotificationManager.
 */
public class StagingEventListener implements Listener {

    private final NotificationManager notificationManager;

    public StagingEventListener(NotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    @EventHandler
    public void onCommandStaged(CommandStagedEvent event) {
        notificationManager.notifyStaging(event.getCommand());
    }

    @EventHandler
    public void onCommandApproved(CommandApprovedEvent event) {
        notificationManager.notifyApproval(event.getCommand());
    }

    @EventHandler
    public void onCommandRejected(CommandRejectedEvent event) {
        notificationManager.notifyRejection(event.getCommand());
    }
}
