package ru.rizonchik.refontsocial.service;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.storage.sql.SqlStorage;
import ru.rizonchik.refontsocial.util.YamlUtil;

public final class AgeService {
    private final RefontSocial plugin;
    private final Object lock = new Object();
    private File file;
    private YamlConfiguration yaml;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private final SimpleDateFormat monthFormatter = new SimpleDateFormat("yyyy-MM");

    public AgeService(RefontSocial plugin) {
        this.plugin = plugin;
    }

    public void init() {
        synchronized (this.lock) {
            this.file = new File(this.plugin.getDataFolder(), "birthday-profile.yml");
            this.yaml = YamlUtil.load(this.file);
            if (!this.yaml.contains("birthdays")) {
                this.yaml.createSection("birthdays");
            }
            if (!this.yaml.contains("lastChanged")) {
                this.yaml.createSection("lastChanged");
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

    public String getBirthday(UUID playerId) {
        String stored = this.getStoredBirthday(playerId);
        return stored != null ? stored : "";
    }

    public Integer getAge(UUID playerId) {
        String birthday = this.getStoredBirthday(playerId);
        if (birthday == null || birthday.isEmpty()) {
            return 0;
        }
        return calculateAge(birthday);
    }

    public String getAgeDisplay(UUID playerId) {
        int age = this.getAge(playerId);
        if (age <= 0) {
            return this.plugin.getConfig().getString("birthday.notSetDisplay", "§7Не указан");
        }
        return String.valueOf(age);
    }

    public String getStoredBirthday(UUID playerId) {
        if (this.usesSqlProfileStorage()) {
            return this.getStoredBirthdaySql(playerId);
        }
        synchronized (this.lock) {
            return this.yaml.getString("birthdays." + playerId, null);
        }
    }

    public boolean canSetBirthday(UUID playerId) {
        String lastChanged = this.getLastChanged(playerId);
        if (lastChanged == null || lastChanged.isEmpty()) {
            return true;
        }
        String currentMonth = monthFormatter.format(new Date());
        return !currentMonth.equals(lastChanged);
    }

    public String getLastChanged(UUID playerId) {
        if (this.usesSqlProfileStorage()) {
            return this.getLastChangedSql(playerId);
        }
        synchronized (this.lock) {
            return this.yaml.getString("lastChanged." + playerId, null);
        }
    }

    public boolean setBirthday(UUID playerId, String birthday) {
        if (!isValidBirthday(birthday)) {
            return false;
        }
        if (!canSetBirthday(playerId)) {
            return false;
        }
        String currentMonth = monthFormatter.format(new Date());
        if (this.usesSqlProfileStorage()) {
            this.setBirthdaySql(playerId, birthday, currentMonth);
        }
        synchronized (this.lock) {
            this.yaml.set("birthdays." + playerId, birthday);
            this.yaml.set("lastChanged." + playerId, currentMonth);
            YamlUtil.save(this.file, this.yaml);
        }
        return true;
    }

    public void clearBirthday(UUID playerId) {
        if (this.usesSqlProfileStorage()) {
            this.clearBirthdaySql(playerId);
        }
        synchronized (this.lock) {
            this.yaml.set("birthdays." + playerId, null);
            this.yaml.set("lastChanged." + playerId, null);
            YamlUtil.save(this.file, this.yaml);
        }
    }

    public boolean isValidBirthday(String birthday) {
        if (birthday == null || birthday.trim().isEmpty()) {
            return false;
        }
        try {
            LocalDate date = LocalDate.parse(birthday, dateFormatter);
            LocalDate min = LocalDate.now().minusYears(this.plugin.getConfig().getInt("birthday.max", 100));
            LocalDate max = LocalDate.now().minusYears(this.plugin.getConfig().getInt("birthday.min", 13));
            return !date.isAfter(max) && !date.isBefore(min);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public boolean isBirthdayToday(UUID playerId) {
        String birthday = this.getStoredBirthday(playerId);
        if (birthday == null || birthday.isEmpty()) {
            return false;
        }
        try {
            LocalDate date = LocalDate.parse(birthday, dateFormatter);
            LocalDate today = LocalDate.now();
            return date.getMonthValue() == today.getMonthValue() && date.getDayOfMonth() == today.getDayOfMonth();
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    public String getBirthdayDisplay(UUID playerId) {
        String birthday = this.getStoredBirthday(playerId);
        if (birthday == null || birthday.isEmpty()) {
            return this.plugin.getConfig().getString("birthday.notSetDisplay", "§7Не указан");
        }
        return birthday;
    }

    public String getBirthdayEmoji() {
        return this.plugin.getConfig().getString("birthday.tabEmoji", "§6❤");
    }

    public String getFormattedBirthdayForTab(UUID playerId) {
        if (!isBirthdayToday(playerId)) {
            return "";
        }
        String emoji = getBirthdayEmoji();
        return emoji + " ";
    }

    private int calculateAge(String birthday) {
        try {
            LocalDate birthDate = LocalDate.parse(birthday, dateFormatter);
            return Period.between(birthDate, LocalDate.now()).getYears();
        } catch (DateTimeParseException e) {
            return 0;
        }
    }

    private boolean usesSqlProfileStorage() {
        return this.plugin.getStorage() instanceof SqlStorage;
    }

    private SqlStorage getSqlStorage() {
        return (SqlStorage) this.plugin.getStorage();
    }

    private void initSqlSchema() {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "CREATE TABLE IF NOT EXISTS rs_birthday_profile (uuid VARCHAR(36) PRIMARY KEY, birthday VARCHAR(10) NULL, last_changed VARCHAR(7) NULL)")) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init birthday SQL schema", e);
        }
    }

    private void migrateLegacyDataIfNeeded() {
        if (!this.usesSqlProfileStorage()) {
            return;
        }
        if (!this.isSqlBirthdayTableEmpty()) {
            return;
        }
        Connection c = null;
        try {
            c = this.getSqlStorage().getDataSource().getConnection();
            c.setAutoCommit(false);
            this.migrateFromYamlSection(c, this.yaml.getConfigurationSection("birthdays"));
            c.commit();
        } catch (SQLException e) {
            this.rollbackQuietly(c);
            throw new RuntimeException("Failed to migrate birthday data to SQL", e);
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
                String birthday = section.getString(key, null);
                if (birthday == null) {
                    continue;
                }
                String lastChanged = this.yaml.getString("lastChanged." + key, null);
                this.upsertBirthday(c, uuid, birthday, lastChanged);
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isSqlBirthdayTableEmpty() {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) AS cnt FROM rs_birthday_profile");
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return true;
            }
            return rs.getInt("cnt") <= 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to inspect birthday SQL state", e);
        }
    }

    private String getStoredBirthdaySql(UUID playerId) {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT birthday FROM rs_birthday_profile WHERE uuid=?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString("birthday");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read birthday from SQL", e);
        }
    }

    private String getLastChangedSql(UUID playerId) {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT last_changed FROM rs_birthday_profile WHERE uuid=?")) {
            ps.setString(1, playerId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return rs.getString("last_changed");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read last_changed from SQL", e);
        }
    }

    private void setBirthdaySql(UUID playerId, String birthday, String lastChanged) {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection()) {
            this.upsertBirthday(c, playerId, birthday, lastChanged);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to store birthday in SQL", e);
        }
    }

    private void clearBirthdaySql(UUID playerId) {
        try (Connection c = this.getSqlStorage().getDataSource().getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM rs_birthday_profile WHERE uuid=?")) {
            ps.setString(1, playerId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear birthday from SQL", e);
        }
    }

    private void upsertBirthday(Connection c, UUID uuid, String birthday, String lastChanged) throws SQLException {
        this.ensureSqlBirthdayRow(c, uuid);
        try (PreparedStatement ps = c.prepareStatement(
            "UPDATE rs_birthday_profile SET birthday=?, last_changed=? WHERE uuid=?")) {
            ps.setString(1, birthday);
            ps.setString(2, lastChanged);
            ps.setString(3, uuid.toString());
            ps.executeUpdate();
        }
    }

    private void ensureSqlBirthdayRow(Connection c, UUID uuid) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
            "INSERT INTO rs_birthday_profile(uuid, birthday, last_changed) VALUES (?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, null);
            ps.setString(3, null);
            ps.executeUpdate();
        } catch (SQLException ex) {
            try (PreparedStatement check = c.prepareStatement("SELECT uuid FROM rs_birthday_profile WHERE uuid=?")) {
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
}
