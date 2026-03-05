package ru.rizonchik.refontsocial.model;

import java.util.UUID;

public final class MarriageInfo {
    private final UUID spouse;
    private final long sinceMillis;

    public MarriageInfo(UUID spouse, long sinceMillis) {
        this.spouse = spouse;
        this.sinceMillis = sinceMillis;
    }

    public static MarriageInfo single() {
        return new MarriageInfo(null, 0L);
    }

    public boolean isMarried() {
        return this.spouse != null;
    }

    public UUID getSpouse() {
        return this.spouse;
    }

    public long getSinceMillis() {
        return this.sinceMillis;
    }
}
