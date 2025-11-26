package br.com.devplugins.events;

import br.com.devplugins.staging.StagedCommand;

/**
 * Event fired when a command is rejected by a reviewer.
 */
public class CommandRejectedEvent extends StagingEvent {

    public CommandRejectedEvent(StagedCommand command) {
        super(command);
    }
}
