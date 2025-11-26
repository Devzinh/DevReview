package br.com.devplugins.staging;

import java.util.UUID;

public class StagedCommand {
    private final UUID id;
    private final UUID senderId;
    private final String senderName;
    private final String commandLine;
    private final long timestamp;
    private String justification;
    private UUID reviewerId;
    private String reviewerName;
    private Status status;

    public enum Status {
        PENDING, APPROVED, REJECTED
    }

    public StagedCommand(UUID senderId, String senderName, String commandLine) {
        this.id = UUID.randomUUID();
        this.senderId = senderId;
        this.senderName = senderName;
        this.commandLine = commandLine;
        this.timestamp = System.currentTimeMillis();
        this.status = Status.PENDING;
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
        this.status = Status.PENDING;
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

    public String getJustification() {
        return justification;
    }

    public void setJustification(String justification) {
        this.justification = justification;
    }

    public UUID getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(UUID reviewerId) {
        this.reviewerId = reviewerId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
