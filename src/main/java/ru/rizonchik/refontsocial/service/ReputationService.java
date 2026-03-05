/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.rizonchik.refontsocial.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.rizonchik.refontsocial.listener.InteractionTracker;
import ru.rizonchik.refontsocial.storage.Storage;
import ru.rizonchik.refontsocial.storage.TopCategory;
import ru.rizonchik.refontsocial.storage.model.PlayerRep;
import ru.rizonchik.refontsocial.util.Colors;
import ru.rizonchik.refontsocial.util.NumberUtil;
import ru.rizonchik.refontsocial.util.YamlUtil;

public final class ReputationService {
    private final JavaPlugin plugin;
    private final Storage storage;
    private InteractionTracker interactionTracker;
    private final Map<UUID, CacheEntry> cache = new ConcurrentHashMap<UUID, CacheEntry>();
    private final Map<String, Long> cooldownGlobal = new ConcurrentHashMap<String, Long>();
    private final Map<TopKey, TopCacheEntry> topCache = new ConcurrentHashMap<TopKey, TopCacheEntry>();
    private final Map<UUID, RankCacheEntry> rankCache = new ConcurrentHashMap<UUID, RankCacheEntry>();
    private final Map<UUID, NameCacheEntry> nameCache = new ConcurrentHashMap<UUID, NameCacheEntry>();

    public ReputationService(JavaPlugin plugin, Storage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void setInteractionTracker(InteractionTracker tracker) {
        this.interactionTracker = tracker;
    }

    public void shutdown() {
        this.cache.clear();
        this.cooldownGlobal.clear();
        this.topCache.clear();
        this.rankCache.clear();
        this.nameCache.clear();
    }

    private boolean cacheEnabled() {
        return this.plugin.getConfig().getBoolean("performance.cache.enabled", true);
    }

    private long cacheExpireMs() {
        int expireSeconds = this.plugin.getConfig().getInt("performance.cache.expireSeconds", 30);
        if (expireSeconds < 1) {
            expireSeconds = 1;
        }
        return (long)expireSeconds * 1000L;
    }

    private void invalidateCaches(UUID target) {
        if (target != null) {
            this.cache.remove(target);
            this.nameCache.remove(target);
        }
        this.topCache.clear();
        this.rankCache.clear();
    }

    public PlayerRep getOrCreate(UUID uuid, String name) {
        CacheEntry entry;
        long now = System.currentTimeMillis();
        boolean cacheEnabled = this.cacheEnabled();
        long expireMs = this.cacheExpireMs();
        if (cacheEnabled && (entry = this.cache.get(uuid)) != null && now - entry.time <= expireMs) {
            return entry.rep;
        }
        PlayerRep rep = this.storage.getOrCreate(uuid, name);
        if (cacheEnabled) {
            this.cache.put(uuid, new CacheEntry(rep, now));
        }
        return rep;
    }

    public String getName(UUID uuid) {
        return this.getNameCached(uuid);
    }

    public List<PlayerRep> getTop(int limit, int offset) {
        return this.getTopCached(TopCategory.SCORE, limit, offset);
    }

    public String getNameCached(UUID uuid) {
        if (!this.cacheEnabled()) {
            return this.storage.getLastKnownName(uuid);
        }
        long now = System.currentTimeMillis();
        long expireMs = this.cacheExpireMs();
        NameCacheEntry entry = this.nameCache.get(uuid);
        if (entry != null && now - entry.time <= expireMs) {
            return entry.name;
        }
        String name = this.storage.getLastKnownName(uuid);
        this.nameCache.put(uuid, new NameCacheEntry(name, now));
        return name;
    }

    public int getRankCached(UUID uuid) {
        if (!this.cacheEnabled()) {
            return this.storage.getRank(uuid);
        }
        long now = System.currentTimeMillis();
        long expireMs = this.cacheExpireMs();
        RankCacheEntry entry = this.rankCache.get(uuid);
        if (entry != null && now - entry.time <= expireMs) {
            return entry.rank;
        }
        int rank = this.storage.getRank(uuid);
        this.rankCache.put(uuid, new RankCacheEntry(rank, now));
        return rank;
    }

    public List<String> getReasonTagKeys(boolean like) {
        return new ArrayList<String>(this.loadReasonTags(like).keySet());
    }

    public boolean isReasonTagAllowed(String key, boolean like) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        return this.loadReasonTags(like).containsKey(key);
    }

    public String getReasonTagDisplay(String key) {
        String legacyDisplay;
        if (key == null || key.trim().isEmpty()) {
            return "";
        }
        String display = this.loadReasonTags(true).get(key);
        if (display != null) {
            return Colors.color(display);
        }
        display = this.loadReasonTags(false).get(key);
        if (display != null) {
            return Colors.color(display);
        }
        ConfigurationSection legacy = this.plugin.getConfig().getConfigurationSection("reasons.tags");
        if (legacy != null && (legacyDisplay = legacy.getString(key)) != null && !legacyDisplay.trim().isEmpty()) {
            return Colors.color(legacyDisplay);
        }
        return Colors.color(key);
    }

    private LinkedHashMap<String, String> loadReasonTags(boolean like) {
        ConfigurationSection legacy;
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        String root = like ? "tags.like" : "tags.dislike";
        ConfigurationSection sec = YamlUtil.tags(this.plugin).getConfigurationSection(root);
        if (sec != null) {
            this.collectReasonTags(sec, out);
        }
        if (out.isEmpty() && (legacy = this.plugin.getConfig().getConfigurationSection("reasons.tags")) != null) {
            this.collectReasonTags(legacy, out);
        }
        return out;
    }

    private void collectReasonTags(ConfigurationSection section, Map<String, String> out) {
        for (String key : section.getKeys(false)) {
            if (section.isConfigurationSection(key)) {
                ConfigurationSection child = section.getConfigurationSection(key);
                if (child == null) continue;
                this.collectReasonTags(child, out);
                continue;
            }
            String display = section.getString(key);
            if (display == null || display.trim().isEmpty() || out.containsKey(key)) continue;
            out.put(key, display);
        }
    }

    public List<PlayerRep> getTopCached(TopCategory category, int limit, int offset) {
        if (limit <= 0) {
            return Collections.emptyList();
        }
        if (!this.cacheEnabled()) {
            return this.storage.getTop(category, limit, offset);
        }
        long now = System.currentTimeMillis();
        long expireMs = this.cacheExpireMs();
        int safeOffset = Math.max(0, offset);
        TopKey key = new TopKey(category, limit, safeOffset);
        TopCacheEntry entry = this.topCache.get(key);
        if (entry != null && now - entry.time <= expireMs) {
            return entry.list;
        }
        List<PlayerRep> list = this.storage.getTop(category, limit, safeOffset);
        List<PlayerRep> cached = Collections.unmodifiableList(new ArrayList<PlayerRep>(list));
        this.topCache.put(key, new TopCacheEntry(cached, now));
        return cached;
    }

    public void sendShow(Player viewer, UUID target, String targetName) {
        if (viewer == null || target == null) {
            return;
        }
        UUID viewerId = viewer.getUniqueId();
        String targetDisplay = targetName != null ? targetName : "\u0418\u0433\u0440\u043e\u043a";
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            PlayerRep rep = this.getOrCreate(target, targetDisplay);
            String score = NumberUtil.formatScore(this.plugin, rep.getScore());
            String likes = String.valueOf(rep.getLikes());
            String dislikes = String.valueOf(rep.getDislikes());
            String votes = String.valueOf(rep.getVotes());
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (!viewer.isOnline()) {
                    return;
                }
                String key = viewerId.equals(target) ? "showSelf" : "showOther";
                viewer.sendMessage(Colors.msg(this.plugin, key, "%target%", targetDisplay, "%score%", score, "%likes%", likes, "%dislikes%", dislikes, "%votes%", votes));
            });
        });
    }

    public boolean shouldShowVoterName(Player viewer) {
        String mode = this.plugin.getConfig().getString("profile.history.showVoterNameMode", "PERMISSION");
        if (mode == null) {
            mode = "PERMISSION";
        }
        if ((mode = mode.toUpperCase(Locale.ROOT)).equals("ALWAYS")) {
            return true;
        }
        if (mode.equals("ANONYMOUS")) {
            return false;
        }
        String perm = this.plugin.getConfig().getString("profile.history.showVoterNamePermission", "refontsocial.admin");
        if (perm == null || perm.trim().isEmpty()) {
            perm = "refontsocial.admin";
        }
        return viewer != null && viewer.hasPermission(perm);
    }

    private void sendMessageSync(Player voter, String key, String ... placeholders) {
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (!voter.isOnline()) {
                return;
            }
            voter.sendMessage(Colors.msg(this.plugin, key, placeholders));
        });
    }

    private void voteAsync(Player voter, UUID target, String targetName, boolean like, String reasonTagKey, String fallbackName) {
        if (voter == null || target == null) {
            return;
        }
        boolean preventSelf = this.plugin.getConfig().getBoolean("antiAbuse.preventSelfVote", true);
        if (preventSelf && voter.getUniqueId().equals(target)) {
            this.sendMessageSync(voter, "selfVoteDenied", new String[0]);
            return;
        }
        boolean requireHasPlayedBefore = this.plugin.getConfig().getBoolean("antiAbuse.targetEligibility.requireHasPlayedBefore", true);
        boolean requireTargetOnline = this.plugin.getConfig().getBoolean("antiAbuse.targetEligibility.requireTargetOnline", false);
        OfflinePlayer off = Bukkit.getOfflinePlayer((UUID)target);
        if (requireTargetOnline && (off == null || !off.isOnline())) {
            this.sendMessageSync(voter, "targetMustBeOnline", new String[0]);
            return;
        }
        if (requireHasPlayedBefore) {
            boolean played = false;
            try {
                played = off != null && off.hasPlayedBefore();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            if (!(played || off != null && off.isOnline())) {
                this.sendMessageSync(voter, "targetNeverPlayed", new String[0]);
                return;
            }
        }
        boolean bypassCooldown = voter.hasPermission("refontsocial.bypass.cooldown");
        boolean bypassInteraction = voter.hasPermission("refontsocial.bypass.interaction");
        boolean bypassIp = voter.hasPermission("refontsocial.bypass.ip");
        boolean reasonsEnabled = this.plugin.getConfig().getBoolean("reasons.enabled", true);
        boolean requireReason = this.plugin.getConfig().getBoolean("reasons.requireReason", false);
        if (reasonTagKey == null && reasonsEnabled && requireReason) {
            this.sendMessageSync(voter, "reasonRequired", new String[0]);
            return;
        }
        if (reasonTagKey != null && reasonsEnabled && !this.isReasonTagAllowed(reasonTagKey, like)) {
            this.sendMessageSync(voter, "reasonInvalidForVote", new String[0]);
            return;
        }
        UUID voterId = voter.getUniqueId();
        String repName = targetName != null ? targetName : fallbackName;
        boolean requireInteraction = this.plugin.getConfig().getBoolean("antiAbuse.requireInteraction.enabled", true);
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            int globalCd;
            int used;
            int maxPerDay;
            long validSeconds;
            long validMs;
            long left;
            String key;
            Long last;
            int globalCd2;
            long now = System.currentTimeMillis();
            if (!bypassCooldown && (globalCd2 = this.plugin.getConfig().getInt("antiAbuse.cooldowns.voteGlobalSeconds", 20)) > 0 && (last = this.cooldownGlobal.get(key = voterId.toString())) != null && (left = last + (long)globalCd2 * 1000L - now) > 0L) {
                this.sendMessageSync(voter, "cooldownGlobal", "%seconds%", String.valueOf(left / 1000L + 1L));
                return;
            }
            if (!bypassInteraction && requireInteraction && this.interactionTracker != null && !this.interactionTracker.hasRecentInteraction(voterId, target, validMs = Math.max(1L, validSeconds = this.plugin.getConfig().getLong("antiAbuse.requireInteraction.interactionValidSeconds", 600L)) * 1000L)) {
                this.sendMessageSync(voter, "interactionRequired", new String[0]);
                return;
            }
            boolean dailyLimit = this.plugin.getConfig().getBoolean("antiAbuse.dailyLimit.enabled", true);
            if (!bypassCooldown && dailyLimit && (maxPerDay = this.plugin.getConfig().getInt("antiAbuse.dailyLimit.maxVotesPerDay", 20)) > 0 && (used = this.storage.countVotesByVoterSince(voterId, NumberUtil.startOfTodayMillis())) >= maxPerDay) {
                this.sendMessageSync(voter, "dailyLimit", "%limit%", String.valueOf(maxPerDay));
                return;
            }
            Storage.VoteState state = this.storage.getVoteState(voterId, target);
            boolean ipProtection = this.plugin.getConfig().getBoolean("antiAbuse.ipProtection.enabled", false);
            if (ipProtection && !bypassIp) {
                String voterIp = this.storage.getIpHash(voterId);
                String targetIp = this.storage.getIpHash(target);
                if (voterIp != null && targetIp != null && voterIp.equals(targetIp)) {
                    long left2;
                    String mode = this.plugin.getConfig().getString("antiAbuse.ipProtection.mode", "SAME_IP_DENY");
                    if (mode == null) {
                        mode = "SAME_IP_DENY";
                    }
                    if ((mode = mode.toUpperCase(Locale.ROOT)).equals("SAME_IP_DENY")) {
                        this.sendMessageSync(voter, "ipDenied", new String[0]);
                        return;
                    }
                    long cd = this.plugin.getConfig().getLong("antiAbuse.ipProtection.cooldownSeconds", 86400L);
                    if (cd < 1L) {
                        cd = 1L;
                    }
                    long cdMs = cd * 1000L;
                    if (state != null && state.lastTime != null && (left2 = state.lastTime + cdMs - now) > 0L) {
                        this.sendMessageSync(voter, "ipCooldown", "%seconds%", String.valueOf(left2 / 1000L + 1L));
                        return;
                    }
                }
            }
            if (!bypassCooldown && state != null) {
                long left3;
                int changeVoteCd;
                long left4;
                int sameTargetCd = this.plugin.getConfig().getInt("antiAbuse.cooldowns.sameTargetSeconds", 600);
                if (sameTargetCd > 0 && (left4 = state.lastTime + (long)sameTargetCd * 1000L - now) > 0L) {
                    this.sendMessageSync(voter, "cooldownTarget", "%seconds%", String.valueOf(left4 / 1000L + 1L));
                    return;
                }
                if (state.value != null && state.value != (like ? 1 : 0) && (changeVoteCd = this.plugin.getConfig().getInt("antiAbuse.cooldowns.changeVoteSeconds", 1800)) > 0 && (left3 = state.lastTime + (long)changeVoteCd * 1000L - now) > 0L) {
                    this.sendMessageSync(voter, "cooldownChangeVote", "%seconds%", String.valueOf(left3 / 1000L + 1L));
                    return;
                }
            }
            if (!bypassCooldown && (globalCd = this.plugin.getConfig().getInt("antiAbuse.cooldowns.voteGlobalSeconds", 20)) > 0) {
                this.cooldownGlobal.put(voterId.toString(), now);
            }
            double scoreBefore = this.storage.getOrCreate(target, repName).getScore();
            Storage.VoteResult result = this.storage.applyVote(voterId, target, like ? 1 : 0, now, targetName, reasonTagKey);
            this.invalidateCaches(target);
            PlayerRep rep = this.getOrCreate(target, repName);
            double scoreAfter = rep.getScore();
            double deltaRaw = scoreAfter - scoreBefore;
            String score = NumberUtil.formatScore(this.plugin, scoreAfter);
            String delta = NumberUtil.formatScore(this.plugin, Math.abs(deltaRaw));
            String deltaSigned = this.formatDelta(deltaRaw);
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (!voter.isOnline()) {
                    return;
                }
                String safeTargetName = this.safeName(target, targetName);
                String reasonDisplay = reasonTagKey != null ? this.getReasonTagDisplay(reasonTagKey) : "-";
                String voteType = like ? "лайк" : "дизлайк";
                if (result == Storage.VoteResult.CREATED) {
                    voter.sendMessage(Colors.msg(this.plugin, like ? "voteLikeDone" : "voteDislikeDone", "%target%", safeTargetName, "%score%", score, "%delta%", delta, "%deltaSigned%", deltaSigned));
                } else if (result == Storage.VoteResult.CHANGED) {
                    voter.sendMessage(Colors.msg(this.plugin, "voteChanged", "%target%", safeTargetName, "%score%", score, "%delta%", delta, "%deltaSigned%", deltaSigned));
                } else {
                    voter.sendMessage(Colors.msg(this.plugin, "voteRemoved", "%target%", safeTargetName, "%score%", score, "%delta%", delta, "%deltaSigned%", deltaSigned));
                }
                if (reasonTagKey != null) {
                    String display = this.getReasonTagDisplay(reasonTagKey);
                    voter.sendMessage(Colors.msg(this.plugin, "reasonSaved", "%reason%", display));
                }
                Player targetPlayer = Bukkit.getPlayer((UUID)target);
                if (targetPlayer != null && targetPlayer.isOnline() && !targetPlayer.getUniqueId().equals(voterId)) {
                    if (result == Storage.VoteResult.CREATED) {
                        targetPlayer.sendMessage(Colors.msg(this.plugin, like ? "voteReceivedLike" : "voteReceivedDislike", "%player%", voter.getName(), "%score%", score, "%reason%", reasonDisplay, "%type%", voteType, "%delta%", delta, "%deltaSigned%", deltaSigned));
                    } else if (result == Storage.VoteResult.CHANGED) {
                        targetPlayer.sendMessage(Colors.msg(this.plugin, "voteReceivedChanged", "%player%", voter.getName(), "%score%", score, "%reason%", reasonDisplay, "%type%", voteType, "%delta%", delta, "%deltaSigned%", deltaSigned));
                    } else {
                        targetPlayer.sendMessage(Colors.msg(this.plugin, "voteReceivedRemoved", "%player%", voter.getName(), "%score%", score, "%delta%", delta, "%deltaSigned%", deltaSigned));
                    }
                }
            });
        });
    }

    public void voteWithReason(Player voter, UUID target, String targetName, boolean like, String reasonTagKey) {
        this.voteAsync(voter, target, targetName, like, reasonTagKey, "\u0418\u0433\u0440\u043e\u043a");
    }

    public void vote(Player voter, UUID target, String targetName, boolean like) {
        this.voteAsync(voter, target, targetName, like, null, "\u0418\u0433\u0440\u043e\u043a");
    }

    private String safeName(UUID uuid, String name) {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        OfflinePlayer off = Bukkit.getOfflinePlayer((UUID)uuid);
        if (off != null && off.getName() != null) {
            return off.getName();
        }
        return uuid.toString().substring(0, 8);
    }

    private String formatDelta(double value) {
        if (Math.abs(value) < 1.0E-6) {
            return "0";
        }
        String formatted = NumberUtil.formatScore(this.plugin, Math.abs(value));
        return value > 0.0 ? "+" + formatted : "-" + formatted;
    }

    private static final class TopKey {
        private final TopCategory category;
        private final int limit;
        private final int offset;

        private TopKey(TopCategory category, int limit, int offset) {
            this.category = category;
            this.limit = limit;
            this.offset = offset;
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || this.getClass() != o.getClass()) {
                return false;
            }
            TopKey topKey = (TopKey)o;
            if (this.limit != topKey.limit) {
                return false;
            }
            if (this.offset != topKey.offset) {
                return false;
            }
            return this.category == topKey.category;
        }

        public int hashCode() {
            int result = this.category != null ? this.category.hashCode() : 0;
            result = 31 * result + this.limit;
            result = 31 * result + this.offset;
            return result;
        }
    }

    private static final class TopCacheEntry {
        private final List<PlayerRep> list;
        private final long time;

        private TopCacheEntry(List<PlayerRep> list, long time) {
            this.list = list;
            this.time = time;
        }
    }

    private static final class RankCacheEntry {
        private final int rank;
        private final long time;

        private RankCacheEntry(int rank, long time) {
            this.rank = rank;
            this.time = time;
        }
    }

    private static final class NameCacheEntry {
        private final String name;
        private final long time;

        private NameCacheEntry(String name, long time) {
            this.name = name;
            this.time = time;
        }
    }

    private static final class CacheEntry {
        private final PlayerRep rep;
        private final long time;

        private CacheEntry(PlayerRep rep, long time) {
            this.rep = rep;
            this.time = time;
        }
    }
}

