package ru.rizonchik.refontsocial.service;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.model.MarriageInfo;
import ru.rizonchik.refontsocial.storage.sql.SqlStorage;
import ru.rizonchik.refontsocial.util.Colors;
import ru.rizonchik.refontsocial.util.YamlUtil;

public final class MarriageService {
    private final RefontSocial plugin;
    private final Object lock = new Object();
    private final Map<UUID, Proposal> pending = new ConcurrentHashMap<UUID, Proposal>();
    private final Map<UUID, Boolean> partnerNearby = new ConcurrentHashMap<UUID, Boolean>();
    private int proximityTaskId = -1;
    private File file;
    private YamlConfiguration yaml;

    public MarriageService(RefontSocial plugin) {
        this.plugin = plugin;
    }

    public void init() {
        synchronized (this.lock) {
            this.file = new File(this.plugin.getDataFolder(), "social-profile.yml");
            this.yaml = YamlUtil.load(this.file);
            if (!this.yaml.contains("marriages")) {
                this.yaml.createSection("marriages");
            }
            YamlUtil.save(this.file, this.yaml);
        }
        if (this.usesSqlProfileStorage()) {
            this.initSqlProfileSchema();
            this.migrateLegacyYamlProfileIfNeeded();
        }
        this.startProximityTask();
    }

    public void shutdown() {
        this.cancelProximityTask();
        synchronized (this.lock) {
            if (this.file != null && this.yaml != null) {
                YamlUtil.save(this.file, this.yaml);
            }
        }
        this.pending.clear();
        this.partnerNearby.clear();
    }

    public MarriageInfo getMarriage(UUID playerId) {
        if (this.usesSqlProfileStorage()) {
            return this.getMarriageSql(playerId);
        }
        return this.getMarriageYaml(playerId);
    }

    private MarriageInfo getMarriageYaml(UUID playerId) {
        synchronized (this.lock) {
            String base = "marriages." + playerId;
            String spouseRaw = this.yaml.getString(base + ".spouse", null);
            if (spouseRaw == null || spouseRaw.trim().isEmpty()) {
                return MarriageInfo.single();
            }
            try {
                UUID spouse = UUID.fromString(spouseRaw);
                long since = this.yaml.getLong(base + ".since", 0L);
                return new MarriageInfo(spouse, since);
            } catch (Exception ignored) {
                return MarriageInfo.single();
            }
        }
    }

    public boolean isMarried(UUID playerId) {
        return this.getMarriage(playerId).isMarried();
    }

    public Player getOnlineSpouse(UUID playerId) {
        MarriageInfo info = this.getMarriage(playerId);
        if (!info.isMarried()) {
            return null;
        }
        Player spouse = Bukkit.getPlayer((UUID)info.getSpouse());
        if (spouse == null || !spouse.isOnline()) {
            return null;
        }
        return spouse;
    }

    public boolean isPartnerNearby(Player player, double maxDistance) {
        if (player == null || maxDistance <= 0.0) {
            return false;
        }
        Player spouse = this.getOnlineSpouse(player.getUniqueId());
        if (spouse == null) {
            return false;
        }
        return this.areClose(player, spouse, maxDistance * maxDistance);
    }

    public double getActionDistance() {
        double value = this.plugin.getConfig().getDouble("marriage.actions.requiredDistance", 4.0);
        if (value <= 0.0) {
            value = 4.0;
        }
        return value;
    }

    public String getSpouseName(UUID playerId) {
        MarriageInfo info = this.getMarriage(playerId);
        if (!info.isMarried()) {
            return this.plugin.getConfig().getString("marriage.singleText", "Single");
        }
        UUID spouseId = info.getSpouse();
        String name = this.plugin.getStorage().getLastKnownName(spouseId);
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(spouseId);
        name = offline.getName();
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return this.plugin.getConfig().getString("placeholders.notFound", "Not found");
    }

    public String getMarriageSinceFormatted(UUID playerId) {
        MarriageInfo info = this.getMarriage(playerId);
        if (!info.isMarried()) {
            return this.plugin.getConfig().getString("marriage.notMarriedSinceText", "-");
        }
        String pattern = this.plugin.getConfig().getString("marriage.dateFormat", "dd.MM.yyyy HH:mm");
        try {
            return new SimpleDateFormat(pattern, Locale.ROOT).format(new Date(info.getSinceMillis()));
        } catch (Exception ignored) {
            return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(info.getSinceMillis()));
        }
    }

    public ProposalResult sendProposal(UUID proposer, UUID target) {
        this.cleanupExpired();
        if (proposer.equals(target)) {
            return ProposalResult.SELF;
        }
        if (this.isMarried(proposer)) {
            return ProposalResult.PROPOSER_ALREADY_MARRIED;
        }
        if (this.isMarried(target)) {
            return ProposalResult.TARGET_ALREADY_MARRIED;
        }
        Proposal existing = this.pending.get(target);
        if (existing != null && !existing.isExpired()) {
            if (existing.proposer.equals(proposer)) {
                return ProposalResult.ALREADY_SENT;
            }
            return ProposalResult.TARGET_HAS_PENDING;
        }
        long expireAt = System.currentTimeMillis() + this.proposalExpireSeconds() * 1000L;
        this.pending.put(target, new Proposal(proposer, expireAt));
        return ProposalResult.SENT;
    }

    public AcceptResult accept(UUID target, UUID proposer) {
        this.cleanupExpired();
        Proposal proposal = this.pending.get(target);
        if (proposal == null || proposal.isExpired()) {
            this.pending.remove(target);
            return AcceptResult.NO_PENDING;
        }
        if (!proposal.proposer.equals(proposer)) {
            return AcceptResult.NO_PENDING_FROM_PLAYER;
        }
        if (this.isMarried(proposer) || this.isMarried(target)) {
            this.pending.remove(target);
            return AcceptResult.ALREADY_MARRIED;
        }
        this.pending.remove(target);
        this.setMarriagePair(proposer, target, System.currentTimeMillis());
        return AcceptResult.ACCEPTED;
    }

    public DenyResult deny(UUID target, UUID proposer) {
        this.cleanupExpired();
        Proposal proposal = this.pending.get(target);
        if (proposal == null || proposal.isExpired()) {
            this.pending.remove(target);
            return DenyResult.NO_PENDING;
        }
        if (!proposal.proposer.equals(proposer)) {
            return DenyResult.NO_PENDING_FROM_PLAYER;
        }
        this.pending.remove(target);
        return DenyResult.DENIED;
    }

    public boolean divorce(UUID playerId) {
        MarriageInfo info = this.getMarriage(playerId);
        if (!info.isMarried()) {
            return false;
        }
        UUID spouse = info.getSpouse();
        if (this.usesSqlProfileStorage()) {
            this.clearMarriageSql(playerId, spouse);
        }
        synchronized (this.lock) {
            this.yaml.set("marriages." + playerId, null);
            this.yaml.set("marriages." + spouse, null);
            YamlUtil.save(this.file, this.yaml);
        }
        this.partnerNearby.remove(playerId);
        this.partnerNearby.remove(spouse);
        return true;
    }

    private void setMarriagePair(UUID first, UUID second, long sinceMillis) {
        if (this.usesSqlProfileStorage()) {
            this.setMarriagePairSql(first, second, sinceMillis);
        }
        synchronized (this.lock) {
            this.yaml.set("marriages." + first + ".spouse", second.toString());
            this.yaml.set("marriages." + first + ".since", sinceMillis);
            this.yaml.set("marriages." + second + ".spouse", first.toString());
            this.yaml.set("marriages." + second + ".since", sinceMillis);
            YamlUtil.save(this.file, this.yaml);
        }
    }

    private boolean usesSqlProfileStorage() {
        return this.plugin.getStorage() instanceof SqlStorage;
    }

    private SqlStorage getSqlStorage() {
        return (SqlStorage)this.plugin.getStorage();
    }

    private void initSqlProfileSchema() {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps1 = c.prepareStatement("CREATE TABLE IF NOT EXISTS rs_social_profile (uuid VARCHAR(36) PRIMARY KEY, spouse VARCHAR(36) NULL, since BIGINT NOT NULL DEFAULT 0)");
             PreparedStatement ps2 = c.prepareStatement("CREATE INDEX idx_rs_social_spouse ON rs_social_profile(spouse)");) {
            ps1.executeUpdate();
            try {
                ps2.executeUpdate();
            } catch (SQLException ignored) {
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init social profile SQL schema", e);
        }
    }

    private void migrateLegacyYamlProfileIfNeeded() {
        if (!this.usesSqlProfileStorage()) {
            return;
        }
        if (!this.isSqlProfileTableEmpty()) {
            return;
        }
        Connection c = null;
        try {
            c = this.getSqlStorage().getDataSource().getConnection();
            c.setAutoCommit(false);
            synchronized (this.lock) {
                ConfigurationSection marriages = this.yaml.getConfigurationSection("marriages");
                if (marriages != null) {
                    for (String key : marriages.getKeys(false)) {
                        try {
                            UUID playerId = UUID.fromString(key);
                            String spouseRaw = this.yaml.getString("marriages." + key + ".spouse", null);
                            if (spouseRaw == null || spouseRaw.trim().isEmpty()) {
                                continue;
                            }
                            UUID spouseId = UUID.fromString(spouseRaw);
                            long since = this.yaml.getLong("marriages." + key + ".since", 0L);
                            this.upsertMarriage(c, playerId, spouseId, since);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
            c.commit();
        } catch (SQLException e) {
            this.rollbackQuietly(c);
            throw new RuntimeException("Failed to migrate social profile data to SQL", e);
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

    private boolean isSqlProfileTableEmpty() {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) AS cnt FROM rs_social_profile");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return true;
            }
            return rs.getInt("cnt") <= 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to inspect social profile SQL state", e);
        }
    }

    private MarriageInfo getMarriageSql(UUID playerId) {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT spouse, since FROM rs_social_profile WHERE uuid=?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return MarriageInfo.single();
                }
                String spouseRaw = rs.getString("spouse");
                if (spouseRaw == null || spouseRaw.trim().isEmpty()) {
                    return MarriageInfo.single();
                }
                try {
                    UUID spouse = UUID.fromString(spouseRaw);
                    long since = rs.getLong("since");
                    return new MarriageInfo(spouse, since);
                } catch (Exception ignored) {
                    return MarriageInfo.single();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read marriage from SQL", e);
        }
    }

    private void setMarriagePairSql(UUID first, UUID second, long sinceMillis) {
        Connection c = null;
        try {
            c = this.getSqlStorage().getDataSource().getConnection();
            c.setAutoCommit(false);
            this.upsertMarriage(c, first, second, sinceMillis);
            this.upsertMarriage(c, second, first, sinceMillis);
            c.commit();
        } catch (SQLException e) {
            this.rollbackQuietly(c);
            throw new RuntimeException("Failed to store marriage in SQL", e);
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

    private void clearMarriageSql(UUID first, UUID second) {
        Connection c = null;
        try {
            c = this.getSqlStorage().getDataSource().getConnection();
            c.setAutoCommit(false);
            this.clearMarriageFor(c, first);
            this.clearMarriageFor(c, second);
            c.commit();
        } catch (SQLException e) {
            this.rollbackQuietly(c);
            throw new RuntimeException("Failed to clear marriage in SQL", e);
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

    private void upsertMarriage(Connection c, UUID playerId, UUID spouseId, long sinceMillis) throws SQLException {
        this.ensureSqlProfileRow(c, playerId);
        try (PreparedStatement ps = c.prepareStatement("UPDATE rs_social_profile SET spouse=?, since=? WHERE uuid=?")) {
            ps.setString(1, spouseId.toString());
            ps.setLong(2, sinceMillis);
            ps.setString(3, playerId.toString());
            ps.executeUpdate();
        }
    }

    private void clearMarriageFor(Connection c, UUID playerId) throws SQLException {
        this.ensureSqlProfileRow(c, playerId);
        try (PreparedStatement ps = c.prepareStatement("UPDATE rs_social_profile SET spouse=NULL, since=0 WHERE uuid=?")) {
            ps.setString(1, playerId.toString());
            ps.executeUpdate();
        }
    }

    private void ensureSqlProfileRow(Connection c, UUID uuid) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO rs_social_profile(uuid, spouse, since) VALUES (?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, null);
            ps.setLong(3, 0L);
            ps.executeUpdate();
        } catch (SQLException ex) {
            try (PreparedStatement check = c.prepareStatement("SELECT uuid FROM rs_social_profile WHERE uuid=?")) {
                check.setString(1, uuid.toString());
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) {
                        return;
                    }
                }
            }
            throw ex;
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

    private long proposalExpireSeconds() {
        long value = this.plugin.getConfig().getLong("marriage.proposalExpireSeconds", 120L);
        return Math.max(10L, value);
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        this.pending.entrySet().removeIf(entry -> entry.getValue().expireAt < now);
    }

    private void startProximityTask() {
        this.cancelProximityTask();
        if (!this.plugin.getConfig().getBoolean("marriage.proximity.enabled", true)) {
            return;
        }
        long period = this.plugin.getConfig().getLong("marriage.proximity.checkPeriodTicks", 40L);
        if (period < 20L) {
            period = 20L;
        }
        this.proximityTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this.plugin, this::tickProximity, period, period);
    }

    private void cancelProximityTask() {
        if (this.proximityTaskId != -1) {
            Bukkit.getScheduler().cancelTask(this.proximityTaskId);
            this.proximityTaskId = -1;
        }
    }

    private void tickProximity() {
        double radius = this.plugin.getConfig().getDouble("marriage.proximity.radius", 50.0);
        if (radius <= 0.0) {
            radius = 50.0;
        }
        double radiusSq = radius * radius;
        Set<UUID> online = new HashSet<UUID>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID playerId = player.getUniqueId();
            online.add(playerId);
            MarriageInfo info = this.getMarriage(playerId);
            if (!info.isMarried()) {
                this.partnerNearby.remove(playerId);
                continue;
            }
            Player spouse = this.getOnlineSpouse(playerId);
            boolean near = spouse != null && this.areClose(player, spouse, radiusSq);
            Boolean previous = this.partnerNearby.put(playerId, near);
            if (previous == null || previous.booleanValue() == near) {
                continue;
            }
            String spouseName = spouse != null ? spouse.getName() : this.getSpouseName(playerId);
            if (near) {
                player.sendMessage(Colors.msg(this.plugin, "marriagePartnerNear", "%target%", spouseName));
            } else {
                player.sendMessage(Colors.msg(this.plugin, "marriagePartnerFar", "%target%", spouseName));
            }
        }
        this.partnerNearby.keySet().retainAll(online);
    }

    private boolean areClose(Player first, Player second, double maxDistanceSq) {
        if (first == null || second == null) {
            return false;
        }
        if (first.getWorld() == null || second.getWorld() == null || !first.getWorld().equals(second.getWorld())) {
            return false;
        }
        return first.getLocation().distanceSquared(second.getLocation()) <= maxDistanceSq;
    }

    public static enum ProposalResult {
        SENT,
        SELF,
        PROPOSER_ALREADY_MARRIED,
        TARGET_ALREADY_MARRIED,
        TARGET_HAS_PENDING,
        ALREADY_SENT;
    }

    public static enum AcceptResult {
        ACCEPTED,
        NO_PENDING,
        NO_PENDING_FROM_PLAYER,
        ALREADY_MARRIED;
    }

    public static enum DenyResult {
        DENIED,
        NO_PENDING,
        NO_PENDING_FROM_PLAYER;
    }

    private static final class Proposal {
        private final UUID proposer;
        private final long expireAt;

        private Proposal(UUID proposer, long expireAt) {
            this.proposer = proposer;
            this.expireAt = expireAt;
        }

        private boolean isExpired() {
            return this.expireAt < System.currentTimeMillis();
        }
    }
}
