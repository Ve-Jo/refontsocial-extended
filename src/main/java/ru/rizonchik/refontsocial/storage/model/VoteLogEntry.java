/*
 * Decompiled with CFR 0.152.
 */
package ru.rizonchik.refontsocial.storage.model;

public final class VoteLogEntry {
    private final long timeMillis;
    private final int value;
    private final String reason;
    private final String voterName;

    public VoteLogEntry(long timeMillis, int value, String reason, String voterName) {
        this.timeMillis = timeMillis;
        this.value = value;
        this.reason = reason;
        this.voterName = voterName;
    }

    public long getTimeMillis() {
        return this.timeMillis;
    }

    public int getValue() {
        return this.value;
    }

    public String getReason() {
        return this.reason;
    }

    public String getVoterName() {
        return this.voterName;
    }
}

