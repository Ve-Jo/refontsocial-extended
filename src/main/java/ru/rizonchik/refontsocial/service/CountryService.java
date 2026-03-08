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
import ru.rizonchik.refontsocial.storage.sql.SqlStorage;
import ru.rizonchik.refontsocial.util.YamlUtil;

public final class CountryService {
    private final RefontSocial plugin;
    private final Object lock = new Object();
    private File file;
    private YamlConfiguration yaml;

    public CountryService(RefontSocial plugin) {
        this.plugin = plugin;
    }

    public void init() {
        synchronized (this.lock) {
            this.file = new File(this.plugin.getDataFolder(), "country-profile.yml");
            this.yaml = YamlUtil.load(this.file);
            if (!this.yaml.contains("countries")) {
                this.yaml.createSection("countries");
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

    public String getCountry(UUID playerId) {
        String stored = this.getStoredCountry(playerId);
        if (stored != null && !stored.isEmpty()) {
            return stored;
        }
        return this.getDefaultCountry();
    }

    public String getStoredCountry(UUID playerId) {
        if (this.usesSqlProfileStorage()) {
            return this.getStoredCountrySql(playerId);
        }
        synchronized (this.lock) {
            return this.yaml.getString("countries." + playerId, null);
        }
    }

    public void setCountry(UUID playerId, String country) {
        if (country == null) {
            country = "";
        }
        if (this.usesSqlProfileStorage()) {
            this.setCountrySql(playerId, country);
        }
        synchronized (this.lock) {
            this.yaml.set("countries." + playerId, country);
            YamlUtil.save(this.file, this.yaml);
        }
    }

    public boolean isValidCountry(String country) {
        if (country == null || country.trim().isEmpty()) {
            return false;
        }
        java.util.List<String> allowedCountries = this.loadAllowedCountries();
        if (allowedCountries.isEmpty()) {
            return country.length() <= 64;
        }
        String normalizedInput = this.normalizeCountryInput(country);
        String bestMatch = null;
        int bestScore = 0;
        for (String allowed : allowedCountries) {
            String normalizedAllowed = this.normalizeCountryInput(allowed);
            int score = 0;
            if (normalizedAllowed.equalsIgnoreCase(normalizedInput)) {
                score = 100; // Exact match
            } else if (normalizedAllowed.toLowerCase().startsWith(normalizedInput.toLowerCase())) {
                score = 80; // Starts with
            } else if (normalizedAllowed.toLowerCase().contains(normalizedInput.toLowerCase())) {
                score = 60; // Contains
            } else if (normalizedInput.toLowerCase().contains(normalizedAllowed.toLowerCase())) {
                score = 40; // Input contains allowed
            }
            if (score > bestScore) {
                bestScore = score;
                bestMatch = allowed;
            }
        }
        return bestScore >= 60;
    }

    public String findClosestCountry(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        java.util.List<String> allowedCountries = this.loadAllowedCountries();
        if (allowedCountries.isEmpty()) {
            return input.trim();
        }
        String normalizedInput = this.normalizeCountryInput(input);
        String bestMatch = null;
        int bestScore = 0;
        for (String allowed : allowedCountries) {
            String normalizedAllowed = this.normalizeCountryInput(allowed);
            int score = 0;
            if (normalizedAllowed.equalsIgnoreCase(normalizedInput)) {
                score = 100; // Exact match
            } else if (normalizedAllowed.toLowerCase().startsWith(normalizedInput.toLowerCase())) {
                score = 80; // Starts with
            } else if (normalizedAllowed.toLowerCase().contains(normalizedInput.toLowerCase())) {
                score = 60; // Contains
            } else if (normalizedInput.toLowerCase().contains(normalizedAllowed.toLowerCase())) {
                score = 40; // Input contains allowed
            }
            if (score > bestScore) {
                bestScore = score;
                bestMatch = allowed;
            }
        }
        return bestMatch;
    }

    private String normalizeCountryInput(String input) {
        // Remove flag emojis (Unicode regional indicators are in range U+1F1E6 to U+1F1FF)
        return input.replaceAll("[\\uD83C\\uDDE6-\\uD83C\\uDDFF]{2}", "").trim();
    }

    public java.util.List<String> loadAllowedCountries() {
        File countriesFile = new File(this.plugin.getDataFolder(), "countries.yml");
        if (!countriesFile.exists()) {
            return java.util.Collections.emptyList();
        }
        YamlConfiguration countriesYaml = YamlUtil.load(countriesFile);
        return countriesYaml.getStringList("countries");
    }

    public String getCountryDisplay(String country) {
        if (country == null || country.isEmpty()) {
            return this.plugin.getConfig().getString("country.notSetDisplay", "Not specified");
        }
        return country;
    }

    private boolean usesSqlProfileStorage() {
        return this.plugin.getStorage() instanceof SqlStorage;
    }

    private SqlStorage getSqlStorage() {
        return (SqlStorage)this.plugin.getStorage();
    }

    private void initSqlSchema() {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("CREATE TABLE IF NOT EXISTS rs_country_profile (uuid VARCHAR(36) PRIMARY KEY, country VARCHAR(64) NULL)");) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init country SQL schema", e);
        }
    }

    private void migrateLegacyDataIfNeeded() {
        if (!this.usesSqlProfileStorage()) {
            return;
        }
        if (!this.isSqlCountryTableEmpty()) {
            return;
        }
        Connection c = null;
        try {
            c = this.getSqlStorage().getDataSource().getConnection();
            c.setAutoCommit(false);
            this.migrateFromYamlSection(c, this.yaml.getConfigurationSection("countries"));
            c.commit();
        } catch (SQLException e) {
            this.rollbackQuietly(c);
            throw new RuntimeException("Failed to migrate country data to SQL", e);
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
                String country = section.getString(key, null);
                if (country == null) {
                    continue;
                }
                this.upsertCountry(c, uuid, country);
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isSqlCountryTableEmpty() {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) AS cnt FROM rs_country_profile");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return true;
            }
            return rs.getInt("cnt") <= 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to inspect country SQL state", e);
        }
    }

    private String getStoredCountrySql(UUID playerId) {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT country FROM rs_country_profile WHERE uuid=?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString("country");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read country from SQL", e);
        }
    }

    private void setCountrySql(UUID playerId, String country) {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection()) {
            this.upsertCountry(c, playerId, country);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to store country in SQL", e);
        }
    }

    private void upsertCountry(Connection c, UUID uuid, String country) throws SQLException {
        this.ensureSqlCountryRow(c, uuid);
        try (PreparedStatement ps = c.prepareStatement("UPDATE rs_country_profile SET country=? WHERE uuid=?")) {
            ps.setString(1, country);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        }
    }

    private void ensureSqlCountryRow(Connection c, UUID uuid) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO rs_country_profile(uuid, country) VALUES (?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, null);
            ps.executeUpdate();
        } catch (SQLException ex) {
            try (PreparedStatement check = c.prepareStatement("SELECT uuid FROM rs_country_profile WHERE uuid=?")) {
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

    private String getDefaultCountry() {
        return this.plugin.getConfig().getString("country.default", "");
    }
}
