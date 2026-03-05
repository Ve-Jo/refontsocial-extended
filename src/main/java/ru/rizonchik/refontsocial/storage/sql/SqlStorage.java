/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.rizonchik.refontsocial.storage.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.java.JavaPlugin;
import ru.rizonchik.refontsocial.storage.Storage;
import ru.rizonchik.refontsocial.storage.TopCategory;
import ru.rizonchik.refontsocial.storage.model.PlayerRep;
import ru.rizonchik.refontsocial.storage.model.VoteLogEntry;
import ru.rizonchik.refontsocial.util.NumberUtil;

public abstract class SqlStorage
implements Storage {
    protected final JavaPlugin plugin;
    protected HikariDataSource ds;

    protected SqlStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    protected abstract HikariConfig buildConfig();

    protected boolean isMysql() {
        return false;
    }

    @Override
    public void init() {
        HikariConfig cfg = this.buildConfig();
        this.ds = new HikariDataSource(cfg);
        try (Connection c = this.ds.getConnection();
             Statement st = c.createStatement();){
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rs_players (uuid VARCHAR(36) PRIMARY KEY,name VARCHAR(16) NULL,likes INT NOT NULL DEFAULT 0,dislikes INT NOT NULL DEFAULT 0,score DOUBLE NOT NULL DEFAULT 10.0,updated BIGINT NOT NULL DEFAULT 0,seen INT NOT NULL DEFAULT 0,ip_hash VARCHAR(64) NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rs_votes (voter VARCHAR(36) NOT NULL,target VARCHAR(36) NOT NULL,value INT NULL,reason VARCHAR(64) NULL,last_time BIGINT NOT NULL,PRIMARY KEY (voter, target))");
            String voteLogId = this.isMysql() ? "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," : "id INTEGER PRIMARY KEY AUTOINCREMENT,";
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rs_vote_log (" + voteLogId + "target VARCHAR(36) NOT NULL,voter VARCHAR(36) NULL,voter_name VARCHAR(16) NULL,value INT NOT NULL,reason VARCHAR(64) NULL,time BIGINT NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS rs_tags (target VARCHAR(36) NOT NULL,tag VARCHAR(64) NOT NULL,count INT NOT NULL DEFAULT 0,PRIMARY KEY (target, tag))");
            try {
                st.executeUpdate("ALTER TABLE rs_players ADD COLUMN seen INT NOT NULL DEFAULT 0");
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
            try {
                st.executeUpdate("ALTER TABLE rs_players ADD COLUMN ip_hash VARCHAR(64) NULL");
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
            try {
                st.executeUpdate("CREATE INDEX idx_rs_players_score ON rs_players(score)");
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
            try {
                st.executeUpdate("CREATE INDEX idx_rs_players_seen_score ON rs_players(seen, score)");
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
            try {
                st.executeUpdate("CREATE INDEX idx_rs_votes_voter_time ON rs_votes(voter, last_time)");
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
            try {
                st.executeUpdate("CREATE INDEX idx_rs_vote_log_target_time ON rs_vote_log(target, time)");
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
            try {
                st.executeUpdate("CREATE INDEX idx_rs_tags_target_count ON rs_tags(target, count)");
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
            try {
                st.executeUpdate("UPDATE rs_players SET seen=1 WHERE (likes+dislikes) > 0");
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
        }
        catch (SQLException e) {
            throw new RuntimeException("Failed to init SQL schema", e);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public String getIpHash(UUID uuid) {
        try (Connection c = this.ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT ip_hash FROM rs_players WHERE uuid=?");){
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery();){
                if (!rs.next()) return null;
                String string = rs.getString("ip_hash");
                return string;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        if (this.ds != null) {
            this.ds.close();
            this.ds = null;
        }
    }

    public HikariDataSource getDataSource() {
        return this.ds;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public PlayerRep getOrCreate(UUID uuid, String name) {
        this.ensurePlayer(uuid, name);
        try (Connection c = this.ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT uuid, name, likes, dislikes, score FROM rs_players WHERE uuid=?");){
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery();){
                if (!rs.next()) return new PlayerRep(uuid, name, 0, 0, 0, NumberUtil.defaultScore(this.plugin));
                int likes = rs.getInt("likes");
                int dislikes = rs.getInt("dislikes");
                double score = rs.getDouble("score");
                int votes = likes + dislikes;
                PlayerRep playerRep = new PlayerRep(uuid, rs.getString("name"), likes, dislikes, votes, score);
                return playerRep;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public String getLastKnownName(UUID uuid) {
        try (Connection c = this.ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT name FROM rs_players WHERE uuid=?");){
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery();){
                if (!rs.next()) return null;
                String string = rs.getString("name");
                return string;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<PlayerRep> getTop(int limit, int offset) {
        return this.getTop(TopCategory.SCORE, limit, offset);
    }

    @Override
    public List<PlayerRep> getTop(TopCategory category, int limit, int offset) {
        ArrayList<PlayerRep> list = new ArrayList<PlayerRep>();
        if (limit <= 0) {
            return list;
        }
        String order = category == TopCategory.LIKES ? "likes DESC, score DESC" : (category == TopCategory.DISLIKES ? "dislikes DESC, score ASC" : (category == TopCategory.VOTES ? "(likes+dislikes) DESC, score DESC" : "score DESC, (likes+dislikes) DESC"));
        try (Connection c = this.ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT uuid, name, likes, dislikes, score FROM rs_players WHERE seen=1 ORDER BY " + order + " LIMIT ? OFFSET ?");){
            ps.setInt(1, limit);
            ps.setInt(2, Math.max(0, offset));
            try (ResultSet rs = ps.executeQuery();){
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    int likes = rs.getInt("likes");
                    int dislikes = rs.getInt("dislikes");
                    double score = rs.getDouble("score");
                    int votes = likes + dislikes;
                    list.add(new PlayerRep(uuid, rs.getString("name"), likes, dislikes, votes, score));
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public Storage.VoteState getVoteState(UUID voter, UUID target) {
        try (Connection c = this.ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT value, reason, last_time FROM rs_votes WHERE voter=? AND target=?");){
            ps.setString(1, voter.toString());
            ps.setString(2, target.toString());
            try (ResultSet rs = ps.executeQuery();){
                if (!rs.next()) return null;
                Object v = rs.getObject("value");
                Integer value = v == null ? null : Integer.valueOf(rs.getInt("value"));
                String reason = rs.getString("reason");
                long t = rs.getLong("last_time");
                Storage.VoteState voteState = new Storage.VoteState(t, value, reason);
                return voteState;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Storage.VoteResult applyVote(UUID voter, UUID target, int value, long timeMillis, String targetName, String reason) {
        this.ensurePlayer(target, targetName);
        Connection c = null;
        try {
            c = this.ds.getConnection();
            c.setAutoCommit(false);

            Integer existing = null;
            String existingReason = null;
            try (PreparedStatement ps = c.prepareStatement("SELECT value, reason FROM rs_votes WHERE voter=? AND target=?");) {
                ps.setString(1, voter.toString());
                ps.setString(2, target.toString());
                try (ResultSet rs = ps.executeQuery();) {
                    if (rs.next()) {
                        Object existingObj = rs.getObject("value");
                        existing = existingObj == null ? null : Integer.valueOf(rs.getInt("value"));
                        existingReason = rs.getString("reason");
                    }
                }
            }

            Storage.VoteResult result;
            int likeDelta = 0;
            int dislikeDelta = 0;

            if (existing == null) {
                if (value == 1) {
                    likeDelta = 1;
                } else {
                    dislikeDelta = 1;
                }
                this.upsertVote(c, voter, target, value, timeMillis, reason);
                this.addTagCount(c, target, reason, 1);
                this.insertVoteLog(c, target, voter, null, value, reason, timeMillis);
                result = Storage.VoteResult.CREATED;
            } else if (existing.intValue() == value) {
                if (value == 1) {
                    likeDelta = -1;
                } else {
                    dislikeDelta = -1;
                }
                this.clearVote(c, voter, target, timeMillis);
                this.addTagCount(c, target, existingReason, -1);
                this.insertVoteLog(c, target, voter, null, value, "(removed)", timeMillis);
                result = Storage.VoteResult.REMOVED;
            } else {
                if (existing.intValue() == 1 && value == 0) {
                    likeDelta = -1;
                    dislikeDelta = 1;
                } else if (existing.intValue() == 0 && value == 1) {
                    likeDelta = 1;
                    dislikeDelta = -1;
                }
                this.upsertVote(c, voter, target, value, timeMillis, reason);
                this.addTagCount(c, target, existingReason, -1);
                this.addTagCount(c, target, reason, 1);
                this.insertVoteLog(c, target, voter, null, value, reason, timeMillis);
                result = Storage.VoteResult.CHANGED;
            }

            this.updateCounters(c, target, likeDelta, dislikeDelta);
            this.recalcScore(c, target, timeMillis);
            c.commit();
            return result;
        }
        catch (SQLException e) {
            try {
                if (c != null) {
                    c.rollback();
                }
            }
            catch (SQLException ignored) {
            }
            throw new RuntimeException(e);
        }
        finally {
            if (c != null) {
                try {
                    c.setAutoCommit(true);
                    c.close();
                }
                catch (SQLException ignored) {
                }
            }
        }
    }

    private void upsertVote(Connection c, UUID voter, UUID target, int value, long timeMillis, String reason) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO rs_votes(voter, target, value, reason, last_time) VALUES (?,?,?,?,?)");){
            ps.setString(1, voter.toString());
            ps.setString(2, target.toString());
            ps.setInt(3, value);
            ps.setString(4, reason);
            ps.setLong(5, timeMillis);
            ps.executeUpdate();
        }
        catch (SQLException ex) {
            try (PreparedStatement ps2 = c.prepareStatement("UPDATE rs_votes SET value=?, reason=?, last_time=? WHERE voter=? AND target=?");){
                ps2.setInt(1, value);
                ps2.setString(2, reason);
                ps2.setLong(3, timeMillis);
                ps2.setString(4, voter.toString());
                ps2.setString(5, target.toString());
                ps2.executeUpdate();
            }
        }
    }

    private void clearVote(Connection c, UUID voter, UUID target, long timeMillis) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE rs_votes SET value=NULL, reason=NULL, last_time=? WHERE voter=? AND target=?");){
            ps.setLong(1, timeMillis);
            ps.setString(2, voter.toString());
            ps.setString(3, target.toString());
            ps.executeUpdate();
        }
    }

    private void insertVoteLog(Connection c, UUID target, UUID voter, String voterName, int value, String reason, long timeMillis) throws SQLException {
        Throwable throwable;
        PreparedStatement ps;
        String resolvedName = voterName;
        if (resolvedName == null && voter != null) {
            ps = c.prepareStatement("SELECT name FROM rs_players WHERE uuid=?");
            throwable = null;
            try {
                ps.setString(1, voter.toString());
                try (ResultSet rs = ps.executeQuery();){
                    if (rs.next()) {
                        resolvedName = rs.getString("name");
                    }
                }
            }
            catch (Throwable throwable2) {
                throwable = throwable2;
                throw throwable2;
            }
            finally {
                if (ps != null) {
                    if (throwable != null) {
                        try {
                            ps.close();
                        }
                        catch (Throwable throwable3) {
                            throwable.addSuppressed(throwable3);
                        }
                    } else {
                        ps.close();
                    }
                }
            }
        }
        ps = c.prepareStatement("INSERT INTO rs_vote_log(target, voter, voter_name, value, reason, time) VALUES (?,?,?,?,?,?)");
        throwable = null;
        try {
            ps.setString(1, target.toString());
            ps.setString(2, voter != null ? voter.toString() : null);
            ps.setString(3, resolvedName);
            ps.setInt(4, value);
            ps.setString(5, reason);
            ps.setLong(6, timeMillis);
            ps.executeUpdate();
        }
        catch (Throwable throwable4) {
            throwable = throwable4;
            throw throwable4;
        }
        finally {
            if (ps != null) {
                if (throwable != null) {
                    try {
                        ps.close();
                    }
                    catch (Throwable throwable5) {
                        throwable.addSuppressed(throwable5);
                    }
                } else {
                    ps.close();
                }
            }
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    @Override
    public int countVotesByVoterSince(UUID voter, long sinceMillis) {
        try (Connection c = this.ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) AS cnt FROM rs_votes WHERE voter=? AND last_time>=? AND value IS NOT NULL");){
            ps.setString(1, voter.toString());
            ps.setLong(2, sinceMillis);
            try (ResultSet rs = ps.executeQuery();){
                if (!rs.next()) return 0;
                int n = rs.getInt("cnt");
                return n;
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void markSeen(UUID uuid, String name, String ipHash) {
        this.ensurePlayer(uuid, name);
        try (Connection c = this.ds.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE rs_players SET seen=1, name=COALESCE(?, name), ip_hash=COALESCE(?, ip_hash), updated=? WHERE uuid=?");){
            ps.setString(1, name);
            ps.setString(2, ipHash);
            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, uuid.toString());
            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getRank(UUID uuid) {
        try (Connection c = this.ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT uuid FROM rs_players WHERE seen=1 ORDER BY score DESC, (likes+dislikes) DESC");
             ResultSet rs = ps.executeQuery();) {
            int rank = 1;
            while (rs.next()) {
                String current = rs.getString("uuid");
                if (uuid.toString().equals(current)) {
                    return rank;
                }
                rank++;
            }
            return -1;
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, Integer> getTopTags(UUID target, int limit) {
        LinkedHashMap<String, Integer> out = new LinkedHashMap<String, Integer>();
        if (limit <= 0) {
            return out;
        }
        try (Connection c = this.ds.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT tag, count FROM rs_tags WHERE target=? AND count>0 ORDER BY count DESC LIMIT ?");){
            ps.setString(1, target.toString());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery();){
                while (rs.next()) {
                    out.put(rs.getString("tag"), rs.getInt("count"));
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    @Override
    public List<VoteLogEntry> getRecentVotes(UUID target, int limit, boolean includeVoterName) {
        ArrayList<VoteLogEntry> list = new ArrayList<VoteLogEntry>();
        if (limit <= 0) {
            return list;
        }
        try (Connection c = this.ds.getConnection();){
            String sql = includeVoterName ? "SELECT value, reason, time, voter_name FROM rs_vote_log WHERE target=? ORDER BY time DESC LIMIT ?" : "SELECT value, reason, time, NULL AS voter_name FROM rs_vote_log WHERE target=? ORDER BY time DESC LIMIT ?";
            try (PreparedStatement ps = c.prepareStatement(sql);){
                ps.setString(1, target.toString());
                ps.setInt(2, limit);
                try (ResultSet rs = ps.executeQuery();){
                    while (rs.next()) {
                        int v = rs.getInt("value");
                        String reason = rs.getString("reason");
                        long t = rs.getLong("time");
                        String voterName = rs.getString("voter_name");
                        list.add(new VoteLogEntry(t, v, reason, voterName));
                    }
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    protected void ensurePlayer(UUID uuid, String name) {
        try (Connection c = this.ds.getConnection();){
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO rs_players(uuid, name, likes, dislikes, score, updated, seen, ip_hash) VALUES (?,?,?,?,?,?,?,?)");){
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.setInt(3, 0);
                ps.setInt(4, 0);
                ps.setDouble(5, NumberUtil.defaultScore(this.plugin));
                ps.setLong(6, System.currentTimeMillis());
                ps.setInt(7, 0);
                ps.setString(8, null);
                ps.executeUpdate();
            }
            catch (SQLException ex) {
                try (PreparedStatement ps2 = c.prepareStatement("UPDATE rs_players SET name=COALESCE(?, name) WHERE uuid=?");){
                    ps2.setString(1, name);
                    ps2.setString(2, uuid.toString());
                    ps2.executeUpdate();
                }
            }
        }
        catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected void updateCounters(Connection c, UUID target, int likeDelta, int dislikeDelta) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("UPDATE rs_players SET likes=likes+?, dislikes=dislikes+? WHERE uuid=?");){
            ps.setInt(1, likeDelta);
            ps.setInt(2, dislikeDelta);
            ps.setString(3, target.toString());
            ps.executeUpdate();
        }
    }

    protected void recalcScore(Connection c, UUID target, long now) throws SQLException {
        int dislikes;
        int likes;
        Throwable throwable;
        try (PreparedStatement ps = c.prepareStatement("SELECT likes, dislikes FROM rs_players WHERE uuid=?");){
            ps.setString(1, target.toString());
            throwable = null;
            try (ResultSet rs = ps.executeQuery();){
                if (!rs.next()) {
                    return;
                }
                likes = rs.getInt("likes");
                dislikes = rs.getInt("dislikes");
            }
            catch (Throwable throwable2) {
                throwable = throwable2;
                throw throwable2;
            }
        }
        double score = NumberUtil.computeScore(this.plugin, likes, dislikes);
        throwable = null;
        try (PreparedStatement ps = c.prepareStatement("UPDATE rs_players SET score=?, updated=? WHERE uuid=?");){
            ps.setDouble(1, score);
            ps.setLong(2, now);
            ps.setString(3, target.toString());
            ps.executeUpdate();
        }
        catch (Throwable throwable3) {
            throwable = throwable3;
            throw throwable3;
        }
    }

    protected void addTagCount(Connection c, UUID target, String tag, int delta) throws SQLException {
        if (tag == null || tag.trim().isEmpty()) {
            return;
        }
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO rs_tags(target, tag, count) VALUES (?,?,?)");){
            ps.setString(1, target.toString());
            ps.setString(2, tag);
            ps.setInt(3, Math.max(0, delta));
            ps.executeUpdate();
        }
        catch (SQLException ex) {
            String clamp = this.isMysql() ? "GREATEST(count+?, 0)" : "MAX(count+?, 0)";
            try (PreparedStatement ps2 = c.prepareStatement("UPDATE rs_tags SET count=" + clamp + " WHERE target=? AND tag=?");){
                ps2.setInt(1, delta);
                ps2.setString(2, target.toString());
                ps2.setString(3, tag);
                ps2.executeUpdate();
            }
            catch (SQLException ex2) {
                try (PreparedStatement ps3 = c.prepareStatement("SELECT count FROM rs_tags WHERE target=? AND tag=?");){
                    ps3.setString(1, target.toString());
                    ps3.setString(2, tag);
                    try (ResultSet rs = ps3.executeQuery();){
                        int cur = 0;
                        if (rs.next()) {
                            cur = rs.getInt("count");
                        }
                        int next = Math.max(0, cur + delta);
                        try (PreparedStatement ups = c.prepareStatement("UPDATE rs_tags SET count=? WHERE target=? AND tag=?");){
                            ups.setInt(1, next);
                            ups.setString(2, target.toString());
                            ups.setString(3, tag);
                            ups.executeUpdate();
                        }
                    }
                }
            }
        }
    }
}

