package ru.rizonchik.refontsocial.service;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.model.FriendEntry;
import ru.rizonchik.refontsocial.storage.sql.SqlStorage;
import ru.rizonchik.refontsocial.util.YamlUtil;

public final class FriendsService {
    private final RefontSocial plugin;
    private final Object lock = new Object();
    private final Map<UUID, FriendRequest> pending = new ConcurrentHashMap<>();
    private File file;
    private YamlConfiguration yaml;

    public FriendsService(RefontSocial plugin) {
        this.plugin = plugin;
    }

    public void init() {
        synchronized (this.lock) {
            this.file = new File(this.plugin.getDataFolder(), "friends.yml");
            this.yaml = YamlUtil.load(this.file);
            if (!this.yaml.contains("friends")) {
                this.yaml.createSection("friends");
            }
            YamlUtil.save(this.file, this.yaml);
        }
        if (this.usesSqlStorage()) {
            this.initSqlSchema();
            this.migrateLegacyYamlIfNeeded();
        }
    }

    public void shutdown() {
        synchronized (this.lock) {
            if (this.file != null && this.yaml != null) {
                YamlUtil.save(this.file, this.yaml);
            }
        }
        this.pending.clear();
    }

    public boolean isEnabled() {
        return this.plugin.getConfig().getBoolean("friends.enabled", true);
    }

    public boolean joinLeaveMessagesEnabled() {
        return this.plugin.getConfig().getBoolean("friends.joinLeaveMessages.enabled", true);
    }

    public double getActionDistance() {
        double value = this.plugin.getConfig().getDouble("friends.actions.requiredDistance", 4.0);
        if (value <= 0.0) {
            value = 4.0;
        }
        return value;
    }

    public RequestResult sendRequest(UUID requester, UUID target) {
        this.cleanupExpired();
        if (requester == null || target == null) {
            return RequestResult.ERROR;
        }
        if (requester.equals(target)) {
            return RequestResult.SELF;
        }
        if (this.areFriends(requester, target)) {
            return RequestResult.ALREADY_FRIENDS;
        }
        FriendRequest existing = this.pending.get(target);
        if (existing != null && !existing.isExpired()) {
            if (existing.requester.equals(requester)) {
                return RequestResult.ALREADY_SENT;
            }
            return RequestResult.TARGET_HAS_PENDING;
        }
        long expireAt = System.currentTimeMillis() + this.requestExpireSeconds() * 1000L;
        this.pending.put(target, new FriendRequest(requester, expireAt));
        return RequestResult.SENT;
    }

    public AcceptResult accept(UUID target, UUID requester) {
        this.cleanupExpired();
        if (target == null || requester == null) {
            return AcceptResult.ERROR;
        }
        FriendRequest request = this.pending.get(target);
        if (request == null || request.isExpired()) {
            this.pending.remove(target);
            return AcceptResult.NO_PENDING;
        }
        if (!request.requester.equals(requester)) {
            return AcceptResult.NO_PENDING_FROM_PLAYER;
        }
        if (this.areFriends(target, requester)) {
            this.pending.remove(target);
            return AcceptResult.ALREADY_FRIENDS;
        }
        this.pending.remove(target);
        this.addFriendPair(target, requester, System.currentTimeMillis());
        return AcceptResult.ACCEPTED;
    }

    public DenyResult deny(UUID target, UUID requester) {
        this.cleanupExpired();
        FriendRequest request = this.pending.get(target);
        if (request == null || request.isExpired()) {
            this.pending.remove(target);
            return DenyResult.NO_PENDING;
        }
        if (!request.requester.equals(requester)) {
            return DenyResult.NO_PENDING_FROM_PLAYER;
        }
        this.pending.remove(target);
        return DenyResult.DENIED;
    }

    public boolean removeFriend(UUID playerId, UUID friendId) {
        if (!this.areFriends(playerId, friendId)) {
            return false;
        }
        this.removeFriendPair(playerId, friendId);
        return true;
    }

    public boolean areFriends(UUID first, UUID second) {
        if (first == null || second == null) {
            return false;
        }
        for (FriendEntry entry : this.getFriends(first)) {
            if (second.equals(entry.getFriend())) {
                return true;
            }
        }
        return false;
    }

    public boolean areFriendsNearby(Player player, Player friend, double maxDistance) {
        if (player == null || friend == null || maxDistance <= 0.0) {
            return false;
        }
        if (!player.getWorld().equals(friend.getWorld())) {
            return false;
        }
        return player.getLocation().distanceSquared(friend.getLocation()) <= maxDistance * maxDistance;
    }

    public List<FriendEntry> getFriends(UUID playerId) {
        if (playerId == null) {
            return Collections.emptyList();
        }
        if (this.usesSqlStorage()) {
            return this.getFriendsSql(playerId);
        }
        return this.getFriendsYaml(playerId);
    }

    public List<UUID> getFriendIds(UUID playerId) {
        List<FriendEntry> friends = this.getFriends(playerId);
        if (friends.isEmpty()) {
            return Collections.emptyList();
        }
        List<UUID> out = new ArrayList<>(friends.size());
        for (FriendEntry entry : friends) {
            out.add(entry.getFriend());
        }
        return out;
    }

    public int getFriendsCount(UUID playerId) {
        return this.getFriends(playerId).size();
    }

    public int getOnlineFriendsCount(UUID playerId) {
        int count = 0;
        for (UUID friendId : this.getFriendIds(playerId)) {
            Player friend = Bukkit.getPlayer(friendId);
            if (friend != null && friend.isOnline()) {
                count++;
            }
        }
        return count;
    }

    private List<FriendEntry> getFriendsYaml(UUID playerId) {
        synchronized (this.lock) {
            ConfigurationSection section = this.yaml.getConfigurationSection("friends." + playerId);
            if (section == null) {
                return Collections.emptyList();
            }
            List<FriendEntry> entries = new ArrayList<>();
            for (String key : section.getKeys(false)) {
                try {
                    UUID friendId = UUID.fromString(key);
                    long since = section.getLong(key + ".since", 0L);
                    entries.add(new FriendEntry(friendId, since));
                } catch (Exception ignored) {
                }
            }
            return entries;
        }
    }

    private List<FriendEntry> getFriendsSql(UUID playerId) {
        List<FriendEntry> entries = new ArrayList<>();
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT friend, since FROM rs_friends WHERE uuid=?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        UUID friend = UUID.fromString(rs.getString("friend"));
                        long since = rs.getLong("since");
                        entries.add(new FriendEntry(friend, since));
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read friends from SQL", e);
        }
        return entries;
    }

    private void addFriendPair(UUID first, UUID second, long sinceMillis) {
        if (this.usesSqlStorage()) {
            this.addFriendPairSql(first, second, sinceMillis);
        }
        synchronized (this.lock) {
            setFriendYaml(first, second, sinceMillis);
            setFriendYaml(second, first, sinceMillis);
            YamlUtil.save(this.file, this.yaml);
        }
    }

    private void removeFriendPair(UUID first, UUID second) {
        if (this.usesSqlStorage()) {
            this.removeFriendPairSql(first, second);
        }
        synchronized (this.lock) {
            this.yaml.set("friends." + first + "." + second, null);
            this.yaml.set("friends." + second + "." + first, null);
            YamlUtil.save(this.file, this.yaml);
        }
    }

    private void setFriendYaml(UUID owner, UUID friend, long sinceMillis) {
        String base = "friends." + owner + "." + friend;
        this.yaml.set(base + ".since", sinceMillis);
    }

    private void initSqlSchema() {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps1 = c.prepareStatement("CREATE TABLE IF NOT EXISTS rs_friends (uuid VARCHAR(36) NOT NULL, friend VARCHAR(36) NOT NULL, since BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (uuid, friend))");
             PreparedStatement ps2 = c.prepareStatement("CREATE INDEX idx_rs_friends_friend ON rs_friends(friend)");) {
            ps1.executeUpdate();
            try {
                ps2.executeUpdate();
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init friends SQL schema", e);
        }
    }

    private void migrateLegacyYamlIfNeeded() {
        if (!this.usesSqlStorage()) {
            return;
        }
        if (!this.isSqlTableEmpty()) {
            return;
        }
        Connection c = null;
        try {
            c = this.getSqlStorage().getDataSource().getConnection();
            c.setAutoCommit(false);
            synchronized (this.lock) {
                ConfigurationSection section = this.yaml.getConfigurationSection("friends");
                if (section != null) {
                    for (String key : section.getKeys(false)) {
                        try {
                            UUID owner = UUID.fromString(key);
                            ConfigurationSection friends = section.getConfigurationSection(key);
                            if (friends == null) {
                                continue;
                            }
                            for (String friendKey : friends.getKeys(false)) {
                                try {
                                    UUID friend = UUID.fromString(friendKey);
                                    long since = friends.getLong(friendKey + ".since", 0L);
                                    this.upsertFriend(c, owner, friend, since);
                                } catch (Exception ignored) {
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            c.commit();
        } catch (SQLException e) {
            this.rollbackQuietly(c);
            throw new RuntimeException("Failed to migrate friends data to SQL", e);
        } finally {
            if (c != null) {
                try {
                    c.setAutoCommit(true);
                    c.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private boolean isSqlTableEmpty() {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) AS cnt FROM rs_friends");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return true;
            }
            return rs.getInt("cnt") <= 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to inspect friends SQL state", e);
        }
    }

    private void addFriendPairSql(UUID first, UUID second, long sinceMillis) {
        Connection c = null;
        try {
            c = this.getSqlStorage().getDataSource().getConnection();
            c.setAutoCommit(false);
            this.upsertFriend(c, first, second, sinceMillis);
            this.upsertFriend(c, second, first, sinceMillis);
            c.commit();
        } catch (SQLException e) {
            this.rollbackQuietly(c);
            throw new RuntimeException("Failed to store friends in SQL", e);
        } finally {
            if (c != null) {
                try {
                    c.setAutoCommit(true);
                    c.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private void removeFriendPairSql(UUID first, UUID second) {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection()) {
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM rs_friends WHERE uuid=? AND friend=?")) {
                ps.setString(1, first.toString());
                ps.setString(2, second.toString());
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM rs_friends WHERE uuid=? AND friend=?")) {
                ps.setString(1, second.toString());
                ps.setString(2, first.toString());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove friends in SQL", e);
        }
    }

    private void upsertFriend(Connection c, UUID owner, UUID friend, long sinceMillis) throws SQLException {
        try (PreparedStatement update = c.prepareStatement("UPDATE rs_friends SET since=? WHERE uuid=? AND friend=?")) {
            update.setLong(1, sinceMillis);
            update.setString(2, owner.toString());
            update.setString(3, friend.toString());
            int updated = update.executeUpdate();
            if (updated > 0) {
                return;
            }
        }
        try (PreparedStatement insert = c.prepareStatement("INSERT INTO rs_friends(uuid, friend, since) VALUES (?,?,?)")) {
            insert.setString(1, owner.toString());
            insert.setString(2, friend.toString());
            insert.setLong(3, sinceMillis);
            insert.executeUpdate();
        }
    }

    private void rollbackQuietly(Connection c) {
        if (c == null) {
            return;
        }
        try {
            c.rollback();
        } catch (SQLException ignored) {
        }
    }

    private boolean usesSqlStorage() {
        return this.plugin.getStorage() instanceof SqlStorage;
    }

    private SqlStorage getSqlStorage() {
        return (SqlStorage) this.plugin.getStorage();
    }

    private int requestExpireSeconds() {
        int value = this.plugin.getConfig().getInt("friends.requestExpireSeconds", 120);
        if (value < 10) {
            value = 10;
        }
        return value;
    }

    private void cleanupExpired() {
        this.pending.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public enum RequestResult {
        SENT,
        SELF,
        ALREADY_SENT,
        TARGET_HAS_PENDING,
        ALREADY_FRIENDS,
        ERROR
    }

    public enum AcceptResult {
        ACCEPTED,
        NO_PENDING,
        NO_PENDING_FROM_PLAYER,
        ALREADY_FRIENDS,
        ERROR
    }

    public enum DenyResult {
        DENIED,
        NO_PENDING,
        NO_PENDING_FROM_PLAYER
    }

    private static final class FriendRequest {
        private final UUID requester;
        private final long expireAt;

        private FriendRequest(UUID requester, long expireAt) {
            this.requester = requester;
            this.expireAt = expireAt;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() >= this.expireAt;
        }
    }
}
