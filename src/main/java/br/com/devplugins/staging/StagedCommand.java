package br.com.devplugins.staging;

import java.util.UUID;

public class StagedCommand {
    private final UUID id;
    private final UUID senderId;
    private final String senderName;
    private final String commandLine;
    private final long timestamp;

    public StagedCommand(UUID senderId, String senderName, String commandLine) {
        this.id = UUID.randomUUID();
        this.senderId = senderId;
        this.senderName = senderName;
        this.commandLine = commandLine;
        this.timestamp = System.currentTimeMillis();
    }

    public UUID getId() {
        return id;
    }

    public UUID getSenderId() {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
