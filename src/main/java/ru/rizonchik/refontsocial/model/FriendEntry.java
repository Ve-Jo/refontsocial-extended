package ru.rizonchik.refontsocial.model;

import java.util.UUID;

public final class FriendEntry {
    private final UUID friend;
    private final long sinceMillis;

    public FriendEntry(UUID friend, long sinceMillis) {
        this.friend = friend;
        this.sinceMillis = sinceMillis;
    }

    public UUID getFriend() {
        return this.friend;
    }

    public long getSinceMillis() {
        return this.sinceMillis;
    }
}
