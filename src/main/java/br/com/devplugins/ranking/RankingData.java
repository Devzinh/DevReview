package br.com.devplugins.ranking;

import java.util.UUID;

public class RankingData {
    private final UUID uuid;
    private String name;
    private int approvals;
    private int rejections;

    public RankingData(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.approvals = 0;
        this.rejections = 0;
    }

    public RankingData(UUID uuid, String name, int approvals, int rejections) {
        this.uuid = uuid;
        this.name = name;
        this.approvals = approvals;
        this.rejections = rejections;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getApprovals() {
        return approvals;
    }

    public void addApproval() {
        this.approvals++;
    }

    public int getRejections() {
        return rejections;
    }

    public void addRejection() {
        this.rejections++;
    }
}
