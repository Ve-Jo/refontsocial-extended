/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.rizonchik.refontsocial.storage.yaml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.rizonchik.refontsocial.storage.Storage;
import ru.rizonchik.refontsocial.storage.TopCategory;
import ru.rizonchik.refontsocial.storage.model.PlayerRep;
import ru.rizonchik.refontsocial.storage.model.VoteLogEntry;
import ru.rizonchik.refontsocial.util.NumberUtil;
import ru.rizonchik.refontsocial.util.YamlUtil;

public final class YamlStorage
implements Storage {
    private final JavaPlugin plugin;
    private final Object lock = new Object();
    private File file;
    private YamlConfiguration yaml;

    public YamlStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void init() {
        Object object = this.lock;
        synchronized (object) {
            this.file = new File(this.plugin.getDataFolder(), "data.yml");
            this.yaml = YamlUtil.load(this.file);
            if (!this.yaml.contains("players")) {
                this.yaml.createSection("players");
            }
            if (!this.yaml.contains("votes")) {
                this.yaml.createSection("votes");
            }
            if (!this.yaml.contains("tags")) {
                this.yaml.createSection("tags");
            }
            if (!this.yaml.contains("vote_log")) {
                this.yaml.createSection("vote_log");
            }
            YamlUtil.save(this.file, this.yaml);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void close() {
        Object object = this.lock;
        synchronized (object) {
            YamlUtil.save(this.file, this.yaml);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public PlayerRep getOrCreate(UUID uuid, String name) {
        Object object = this.lock;
        synchronized (object) {
            String path = "players." + uuid.toString();
            if (!this.yaml.contains(path)) {
                this.yaml.set(path + ".name", (Object)name);
                this.yaml.set(path + ".likes", (Object)0);
                this.yaml.set(path + ".dislikes", (Object)0);
                this.yaml.set(path + ".score", (Object)NumberUtil.defaultScore(this.plugin));
                this.yaml.set(path + ".seen", (Object)false);
                this.yaml.set(path + ".ipHash", null);
                YamlUtil.save(this.file, this.yaml);
            } else if (name != null && !name.isEmpty()) {
                this.yaml.set(path + ".name", (Object)name);
            }
            int likes = this.yaml.getInt(path + ".likes", 0);
            int dislikes = this.yaml.getInt(path + ".dislikes", 0);
            double score = this.yaml.getDouble(path + ".score", NumberUtil.defaultScore(this.plugin));
            int votes = likes + dislikes;
            return new PlayerRep(uuid, this.yaml.getString(path + ".name", name), likes, dislikes, votes, score);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public String getLastKnownName(UUID uuid) {
        Object object = this.lock;
        synchronized (object) {
            return this.yaml.getString("players." + uuid + ".name", null);
        }
    }

    @Override
    public List<PlayerRep> getTop(int limit, int offset) {
        return this.getTop(TopCategory.SCORE, limit, offset);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public List<PlayerRep> getTop(TopCategory category, int limit, int offset) {
        Object object = this.lock;
        synchronized (object) {
            int to;
            if (!this.yaml.contains("players")) {
                return Collections.emptyList();
            }
            Map<String, Object> map = this.yaml.getConfigurationSection("players").getValues(false);
            ArrayList<PlayerRep> list = new ArrayList<PlayerRep>();
            for (String key : map.keySet()) {
                try {
                    UUID uuid = UUID.fromString(key);
                    String base = "players." + key;
                    boolean seen = this.yaml.getBoolean(base + ".seen", false);
                    if (!seen) continue;
                    String name = this.yaml.getString(base + ".name", null);
                    int likes = this.yaml.getInt(base + ".likes", 0);
                    int dislikes = this.yaml.getInt(base + ".dislikes", 0);
                    double score = this.yaml.getDouble(base + ".score", NumberUtil.defaultScore(this.plugin));
                    list.add(new PlayerRep(uuid, name, likes, dislikes, likes + dislikes, score));
                }
                catch (Exception uuid) {}
            }
            Comparator<PlayerRep> cmp = category == TopCategory.LIKES ? Comparator.comparingInt(PlayerRep::getLikes).reversed().thenComparingDouble(PlayerRep::getScore).reversed().thenComparingInt(PlayerRep::getVotes).reversed() : (category == TopCategory.DISLIKES ? Comparator.comparingInt(PlayerRep::getDislikes).reversed().thenComparingDouble(PlayerRep::getScore).thenComparingInt(PlayerRep::getVotes).reversed() : (category == TopCategory.VOTES ? Comparator.comparingInt(PlayerRep::getVotes).reversed().thenComparingDouble(PlayerRep::getScore).reversed() : Comparator.comparingDouble(PlayerRep::getScore).reversed().thenComparingInt(PlayerRep::getVotes).reversed()));
            List<PlayerRep> sorted = list.stream().sorted(cmp).collect(Collectors.toList());
            int from = Math.max(0, offset);
            if (from >= (to = Math.min(sorted.size(), from + Math.max(0, limit)))) {
                return Collections.emptyList();
            }
            return sorted.subList(from, to);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Storage.VoteState getVoteState(UUID voter, UUID target) {
        Object object = this.lock;
        synchronized (object) {
            String path = "votes." + voter + "." + target;
            if (!this.yaml.contains(path)) {
                return null;
            }
            Long lastTime = this.yaml.getLong(path + ".lastTime", 0L);
            Object v = this.yaml.get(path + ".value");
            Integer value = v == null ? null : Integer.valueOf(this.yaml.getInt(path + ".value"));
            String reason = this.yaml.getString(path + ".reason", null);
            return new Storage.VoteState(lastTime, value, reason);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Storage.VoteResult applyVote(UUID voter, UUID target, int value, long timeMillis, String targetName, String reason) {
        Object object = this.lock;
        synchronized (object) {
            Storage.VoteResult result;
            PlayerRep rep = this.getOrCreate(target, targetName);
            String votePath = "votes." + voter + "." + target;
            Object existingObj = this.yaml.get(votePath + ".value");
            Integer existing = existingObj == null ? null : Integer.valueOf(this.yaml.getInt(votePath + ".value"));
            String existingReason = this.yaml.getString(votePath + ".reason", null);
            int likes = rep.getLikes();
            int dislikes = rep.getDislikes();
            if (existing == null) {
                if (value == 1) {
                    ++likes;
                } else {
                    ++dislikes;
                }
                this.yaml.set(votePath + ".value", (Object)value);
                this.yaml.set(votePath + ".reason", (Object)reason);
                this.yaml.set(votePath + ".lastTime", (Object)timeMillis);
                this.addTagCount(target, reason, 1);
                this.addVoteLog(target, voter, null, value, reason, timeMillis);
                result = Storage.VoteResult.CREATED;
            } else if (existing == value) {
                if (value == 1) {
                    --likes;
                } else {
                    --dislikes;
                }
                this.yaml.set(votePath + ".value", null);
                this.yaml.set(votePath + ".reason", null);
                this.yaml.set(votePath + ".lastTime", (Object)timeMillis);
                this.addTagCount(target, existingReason, -1);
                this.addVoteLog(target, voter, null, value, "(removed)", timeMillis);
                result = Storage.VoteResult.REMOVED;
            } else {
                if (existing == 1 && value == 0) {
                    --likes;
                    ++dislikes;
                }
                if (existing == 0 && value == 1) {
                    --dislikes;
                    ++likes;
                }
                this.yaml.set(votePath + ".value", (Object)value);
                this.yaml.set(votePath + ".reason", (Object)reason);
                this.yaml.set(votePath + ".lastTime", (Object)timeMillis);
                this.addTagCount(target, existingReason, -1);
                this.addTagCount(target, reason, 1);
                this.addVoteLog(target, voter, null, value, reason, timeMillis);
                result = Storage.VoteResult.CHANGED;
            }
            likes = Math.max(0, likes);
            dislikes = Math.max(0, dislikes);
            String p = "players." + target;
            this.yaml.set(p + ".name", (Object)targetName);
            this.yaml.set(p + ".likes", (Object)likes);
            this.yaml.set(p + ".dislikes", (Object)dislikes);
            this.yaml.set(p + ".score", (Object)NumberUtil.computeScore(this.plugin, likes, dislikes));
            YamlUtil.save(this.file, this.yaml);
            return result;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int countVotesByVoterSince(UUID voter, long sinceMillis) {
        Object object = this.lock;
        synchronized (object) {
            if (!this.yaml.contains("votes." + voter)) {
                return 0;
            }
            int cnt = 0;
            ConfigurationSection sec = this.yaml.getConfigurationSection("votes." + voter);
            if (sec == null) {
                return 0;
            }
            for (String target : sec.getKeys(false)) {
                String base = "votes." + voter + "." + target;
                long t = this.yaml.getLong(base + ".lastTime", 0L);
                Object v = this.yaml.get(base + ".value");
                if (v == null || t < sinceMillis) continue;
                ++cnt;
            }
            return cnt;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void markSeen(UUID uuid, String name, String ipHash) {
        Object object = this.lock;
        synchronized (object) {
            String path = "players." + uuid.toString();
            if (!this.yaml.contains(path)) {
                this.getOrCreate(uuid, name);
            }
            this.yaml.set(path + ".seen", (Object)true);
            if (name != null && !name.isEmpty()) {
                this.yaml.set(path + ".name", (Object)name);
            }
            if (ipHash != null) {
                this.yaml.set(path + ".ipHash", (Object)ipHash);
            }
            YamlUtil.save(this.file, this.yaml);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public int getRank(UUID uuid) {
        Object object = this.lock;
        synchronized (object) {
            List<PlayerRep> all = this.getTop(TopCategory.SCORE, Integer.MAX_VALUE, 0);
            for (int i = 0; i < all.size(); ++i) {
                if (!all.get(i).getUuid().equals(uuid)) continue;
                return i + 1;
            }
            return -1;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public Map<String, Integer> getTopTags(UUID target, int limit) {
        Object object = this.lock;
        synchronized (object) {
            LinkedHashMap<String, Integer> out = new LinkedHashMap<String, Integer>();
            if (limit <= 0) {
                return out;
            }
            String base = "tags." + target.toString();
            ConfigurationSection sec = this.yaml.getConfigurationSection(base);
            if (sec == null) {
                return out;
            }
            List<Map.Entry<String, Object>> entries = new ArrayList<Map.Entry<String, Object>>(sec.getValues(false).entrySet());
            entries.sort((a, b) -> Integer.compare(this.parseInt(b.getValue(), 0), this.parseInt(a.getValue(), 0)));
            for (int i = 0; i < entries.size() && out.size() < limit; ++i) {
                String tag = entries.get(i).getKey();
                int cnt = this.parseInt(entries.get(i).getValue(), 0);
                if (cnt <= 0) continue;
                out.put(tag, cnt);
            }
            return out;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public List<VoteLogEntry> getRecentVotes(UUID target, int limit, boolean includeVoterName) {
        Object object = this.lock;
        synchronized (object) {
            ArrayList<VoteLogEntry> list = new ArrayList<VoteLogEntry>();
            if (limit <= 0) {
                return list;
            }
            String base = "vote_log." + target.toString();
            ConfigurationSection sec = this.yaml.getConfigurationSection(base);
            if (sec == null) {
                return list;
            }
            List<String> keys = new ArrayList<String>(sec.getKeys(false));
            keys.sort((a, b) -> Long.compare(this.yaml.getLong(base + "." + b + ".time", 0L), this.yaml.getLong(base + "." + a + ".time", 0L)));
            for (String k : keys) {
                String voterUuid;
                if (list.size() >= limit) break;
                String p = base + "." + k;
                int value = this.yaml.getInt(p + ".value", 1);
                String reason = this.yaml.getString(p + ".reason", null);
                long time = this.yaml.getLong(p + ".time", 0L);
                String voterName = null;
                if (includeVoterName && (voterName = this.yaml.getString(p + ".voterName", null)) == null && (voterUuid = this.yaml.getString(p + ".voter", null)) != null) {
                    voterName = this.yaml.getString("players." + voterUuid + ".name", null);
                }
                list.add(new VoteLogEntry(time, value, reason, voterName));
            }
            return list;
        }
    }

    private void addTagCount(UUID target, String tag, int delta) {
        if (tag == null || tag.trim().isEmpty()) {
            return;
        }
        String base = "tags." + target.toString() + "." + tag;
        int cur = this.yaml.getInt(base, 0);
        int next = Math.max(0, cur + delta);
        this.yaml.set(base, (Object)next);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public String getIpHash(UUID uuid) {
        Object object = this.lock;
        synchronized (object) {
            return this.yaml.getString("players." + uuid.toString() + ".ipHash", null);
        }
    }

    private void addVoteLog(UUID target, UUID voter, String voterName, int value, String reason, long timeMillis) {
        String base = "vote_log." + target.toString();
        String id = String.valueOf(System.currentTimeMillis()) + "-" + String.valueOf(new Random().nextInt(9999));
        this.yaml.set(base + "." + id + ".time", (Object)timeMillis);
        this.yaml.set(base + "." + id + ".value", (Object)value);
        this.yaml.set(base + "." + id + ".reason", (Object)reason);
        this.yaml.set(base + "." + id + ".voter", (Object)(voter != null ? voter.toString() : null));
        this.yaml.set(base + "." + id + ".voterName", (Object)voterName);
        int keep = this.plugin.getConfig().getInt("profile.history.limit", 10);
        if (keep < 1) {
            keep = 1;
        }
        this.trimVoteLog(target, keep * 3);
    }

    private void trimVoteLog(UUID target, int keep) {
        String base = "vote_log." + target.toString();
        ConfigurationSection sec = this.yaml.getConfigurationSection(base);
        if (sec == null) {
            return;
        }
        ArrayList keys = new ArrayList(sec.getKeys(false));
        keys.sort((a, b) -> Long.compare(this.yaml.getLong(base + "." + b + ".time", 0L), this.yaml.getLong(base + "." + a + ".time", 0L)));
        for (int i = keep; i < keys.size(); ++i) {
            this.yaml.set(base + "." + (String)keys.get(i), null);
        }
    }

    private int parseInt(Object o, int def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Number) {
            return ((Number)o).intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(o));
        }
        catch (Exception e) {
            return def;
        }
    }
}

