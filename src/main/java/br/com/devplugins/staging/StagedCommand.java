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

    // Constructor for loading from persistence
    public StagedCommand(UUID id, UUID senderId, String senderName, String commandLine, long timestamp,
            String justification) {
        this.id = id;
        this.senderId = senderId;
        this.senderName = senderName;
        this.commandLine = commandLine;
        this.timestamp = timestamp;
        this.justification = justification;
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

    private String justification;

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }
}
