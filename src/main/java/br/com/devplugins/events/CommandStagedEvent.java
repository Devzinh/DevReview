package br.com.devplugins.events;

import br.com.devplugins.staging.StagedCommand;

/**
 * Event fired when a command is staged for review.
 */
public class CommandStagedEvent extends StagingEvent {

    public CommandStagedEvent(StagedCommand command) {
        super(command);
    }
}
