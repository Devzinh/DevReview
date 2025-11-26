package br.com.devplugins.events;

import br.com.devplugins.staging.StagedCommand;

/**
 * Event fired when a command is approved by a reviewer.
 */
public class CommandApprovedEvent extends StagingEvent {

    public CommandApprovedEvent(StagedCommand command) {
        super(command);
    }
}
