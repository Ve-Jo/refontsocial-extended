package ru.rizonchik.refontsocial.service;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.model.Gender;
import ru.rizonchik.refontsocial.storage.sql.SqlStorage;
import ru.rizonchik.refontsocial.util.YamlUtil;

public final class GenderService {
    private final RefontSocial plugin;
    private final Object lock = new Object();
    private File file;
    private YamlConfiguration yaml;

    public GenderService(RefontSocial plugin) {
        this.plugin = plugin;
    }

    public void init() {
        synchronized (this.lock) {
            this.file = new File(this.plugin.getDataFolder(), "gender-profile.yml");
            this.yaml = YamlUtil.load(this.file);
            if (!this.yaml.contains("genders")) {
                this.yaml.createSection("genders");
            }
            YamlUtil.save(this.file, this.yaml);
        }
        if (this.usesSqlProfileStorage()) {
            this.initSqlSchema();
            this.migrateLegacyDataIfNeeded();
        }
    }

    public void shutdown() {
        synchronized (this.lock) {
            if (this.file != null && this.yaml != null) {
                YamlUtil.save(this.file, this.yaml);
            }
        }
    }

    public Gender getGender(UUID playerId) {
        Gender stored = this.getStoredGender(playerId);
        if (stored != null) {
            return stored;
        }
        return this.getDefaultGender();
    }

    public Gender getStoredGender(UUID playerId) {
        if (this.usesSqlProfileStorage()) {
            return this.getStoredGenderSql(playerId);
        }
        synchronized (this.lock) {
            String raw = this.yaml.getString("genders." + playerId, null);
            if (raw == null || raw.trim().isEmpty()) {
                return null;
            }
            return Gender.fromInput(raw);
        }
    }

    public void setGender(UUID playerId, Gender gender) {
        if (gender == null) {
            gender = Gender.UNDISCLOSED;
        }
        if (this.usesSqlProfileStorage()) {
            this.setGenderSql(playerId, gender);
        }
        synchronized (this.lock) {
            this.yaml.set("genders." + playerId, gender.getKey());
            YamlUtil.save(this.file, this.yaml);
        }
    }

    public String getGenderLabel(UUID playerId) {
        return this.getGenderLabel(this.getGender(playerId));
    }

    public String getGenderLabel(Gender gender) {
        if (gender == null) {
            gender = Gender.UNDISCLOSED;
        }
        return this.plugin.getConfig().getString("gender.labels." + gender.getKey(), this.toTitle(gender.getKey()));
    }

    public String getGenderEmoji(UUID playerId) {
        return this.getGenderEmoji(playerId, false);
    }

    public String getGenderEmoji(UUID playerId, boolean emptyIfUnset) {
        Gender gender = this.getStoredGender(playerId);
        if (gender == null) {
            if (emptyIfUnset) {
                return "";
            }
            gender = this.getDefaultGender();
        }
        return this.plugin.getConfig().getString("gender.emojis." + gender.getKey(), "");
    }

    private boolean usesSqlProfileStorage() {
        return this.plugin.getStorage() instanceof SqlStorage;
    }

    private SqlStorage getSqlStorage() {
        return (SqlStorage)this.plugin.getStorage();
    }

    private void initSqlSchema() {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("CREATE TABLE IF NOT EXISTS rs_gender_profile (uuid VARCHAR(36) PRIMARY KEY, gender VARCHAR(32) NULL)");) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init gender SQL schema", e);
        }
    }

    private void migrateLegacyDataIfNeeded() {
        if (!this.usesSqlProfileStorage()) {
            return;
        }
        if (!this.isSqlGenderTableEmpty()) {
            return;
        }
        Connection c = null;
        try {
            c = this.getSqlStorage().getDataSource().getConnection();
            c.setAutoCommit(false);
            this.migrateFromYamlSection(c, this.yaml.getConfigurationSection("genders"));
            File legacyProfile = new File(this.plugin.getDataFolder(), "social-profile.yml");
            if (legacyProfile.exists()) {
                YamlConfiguration legacyYaml = YamlUtil.load(legacyProfile);
                this.migrateFromYamlSection(c, legacyYaml.getConfigurationSection("genders"));
            }
            this.migrateFromLegacySqlTable(c);
            c.commit();
        } catch (SQLException e) {
            this.rollbackQuietly(c);
            throw new RuntimeException("Failed to migrate gender data to SQL", e);
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

    private void migrateFromYamlSection(Connection c, ConfigurationSection section) throws SQLException {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                String raw = section.getString(key, null);
                if (raw == null || raw.trim().isEmpty()) {
                    continue;
                }
                Gender gender = Gender.fromInput(raw);
                this.upsertGender(c, uuid, gender != null ? gender.getKey() : raw);
            } catch (Exception ignored) {
            }
        }
    }

    private void migrateFromLegacySqlTable(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT uuid, gender FROM rs_social_profile WHERE gender IS NOT NULL AND TRIM(gender) <> ''");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String raw = rs.getString("gender");
                    if (raw == null || raw.trim().isEmpty()) {
                        continue;
                    }
                    Gender gender = Gender.fromInput(raw);
                    this.upsertGender(c, uuid, gender != null ? gender.getKey() : raw);
                } catch (Exception ignored) {
                }
            }
        } catch (SQLException ignored) {
            // rs_social_profile might not exist on fresh installs
        }
    }

    private boolean isSqlGenderTableEmpty() {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) AS cnt FROM rs_gender_profile");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return true;
            }
            return rs.getInt("cnt") <= 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to inspect gender SQL state", e);
        }
    }

    private Gender getStoredGenderSql(UUID playerId) {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT gender FROM rs_gender_profile WHERE uuid=?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String raw = rs.getString("gender");
                if (raw == null || raw.trim().isEmpty()) {
                    return null;
                }
                return Gender.fromInput(raw);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read gender from SQL", e);
        }
    }

    private void setGenderSql(UUID playerId, Gender gender) {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection()) {
            this.upsertGender(c, playerId, gender.getKey());
        } catch (SQLException e) {
            throw new RuntimeException("Failed to store gender in SQL", e);
        }
    }

    private void upsertGender(Connection c, UUID uuid, String gender) throws SQLException {
        this.ensureSqlGenderRow(c, uuid);
        try (PreparedStatement ps = c.prepareStatement("UPDATE rs_gender_profile SET gender=? WHERE uuid=?")) {
            ps.setString(1, gender);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    private void ensureSqlGenderRow(Connection c, UUID uuid) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO rs_gender_profile(uuid, gender) VALUES (?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, null);
            ps.executeUpdate();
        } catch (SQLException ex) {
            try (PreparedStatement check = c.prepareStatement("SELECT uuid FROM rs_gender_profile WHERE uuid=?")) {
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

    private String toTitle(String key) {
        if (key == null || key.isEmpty()) {
            return "Unknown";
        }
        String normalized = key.replace("_", " ").trim();
        if (normalized.isEmpty()) {
            return "Unknown";
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private Gender getDefaultGender() {
        Gender fallback = Gender.fromInput(this.plugin.getConfig().getString("gender.default", "undisclosed"));
        return fallback != null ? fallback : Gender.UNDISCLOSED;
    }
}
