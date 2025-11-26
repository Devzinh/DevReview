package br.com.devplugins.events;

import br.com.devplugins.staging.StagedCommand;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Base event for staging-related actions.
 * This event is fired when commands are staged, approved, or rejected.
 */
public abstract class StagingEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final StagedCommand command;

    public StagingEvent(StagedCommand command) {
        this.command = command;
    }

    public StagedCommand getCommand() {
        return command;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
