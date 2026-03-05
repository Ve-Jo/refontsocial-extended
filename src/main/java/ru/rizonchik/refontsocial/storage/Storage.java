/*
 * Decompiled with CFR 0.152.
 */
package ru.rizonchik.refontsocial.storage;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import ru.rizonchik.refontsocial.storage.TopCategory;
import ru.rizonchik.refontsocial.storage.model.PlayerRep;
import ru.rizonchik.refontsocial.storage.model.VoteLogEntry;

public interface Storage {
    public void init();

    public void close();

    public PlayerRep getOrCreate(UUID var1, String var2);

    public String getLastKnownName(UUID var1);

    public List<PlayerRep> getTop(int var1, int var2);

    public List<PlayerRep> getTop(TopCategory var1, int var2, int var3);

    public VoteState getVoteState(UUID var1, UUID var2);

    public VoteResult applyVote(UUID var1, UUID var2, int var3, long var4, String var6, String var7);

    public int countVotesByVoterSince(UUID var1, long var2);

    public void markSeen(UUID var1, String var2, String var3);

    public int getRank(UUID var1);

    public Map<String, Integer> getTopTags(UUID var1, int var2);

    public List<VoteLogEntry> getRecentVotes(UUID var1, int var2, boolean var3);

    public String getIpHash(UUID var1);

    public static enum VoteResult {
        CREATED,
        CHANGED,
        REMOVED;

    }

    public static final class VoteState {
        public final Long lastTime;
        public final Integer value;
        public final String reason;

        public VoteState(Long lastTime, Integer value, String reason) {
            this.lastTime = lastTime;
            this.value = value;
            this.reason = reason;
        }
    }
}

