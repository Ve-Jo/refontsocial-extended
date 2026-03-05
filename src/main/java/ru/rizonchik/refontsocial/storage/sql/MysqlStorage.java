/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.rizonchik.refontsocial.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import org.bukkit.plugin.java.JavaPlugin;
import ru.rizonchik.refontsocial.storage.sql.SqlStorage;

public final class MysqlStorage
extends SqlStorage {
    public MysqlStorage(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    protected boolean isMysql() {
        return true;
    }

    @Override
    protected HikariConfig buildConfig() {
        String host = this.plugin.getConfig().getString("storage.mysql.host", "127.0.0.1");
        int port = this.plugin.getConfig().getInt("storage.mysql.port", 3306);
        String database = this.plugin.getConfig().getString("storage.mysql.database", "refontsocial");
        String user = this.plugin.getConfig().getString("storage.mysql.username", "root");
        String pass = this.plugin.getConfig().getString("storage.mysql.password", "password");
        boolean useSSL = this.plugin.getConfig().getBoolean("storage.mysql.useSSL", false);
        String tz = this.plugin.getConfig().getString("storage.mysql.serverTimezone", "UTC");
        String params = this.plugin.getConfig().getString("storage.mysql.params", "");
        String jdbc = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL + "&serverTimezone=" + tz + (params.isEmpty() ? "" : "&" + params);
        int maxPool = this.plugin.getConfig().getInt("storage.mysql.pool.maximumPoolSize", 10);
        int minIdle = this.plugin.getConfig().getInt("storage.mysql.pool.minimumIdle", 2);
        long connTimeout = this.plugin.getConfig().getLong("storage.mysql.pool.connectionTimeoutMs", 10000L);
        long idleTimeout = this.plugin.getConfig().getLong("storage.mysql.pool.idleTimeoutMs", 600000L);
        long maxLifetime = this.plugin.getConfig().getLong("storage.mysql.pool.maxLifetimeMs", 1800000L);
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(jdbc);
        cfg.setUsername(user);
        cfg.setPassword(pass);
        cfg.setMaximumPoolSize(Math.max(2, maxPool));
        cfg.setMinimumIdle(Math.max(0, minIdle));
        cfg.setConnectionTimeout(Math.max(1000L, connTimeout));
        cfg.setIdleTimeout(Math.max(10000L, idleTimeout));
        cfg.setMaxLifetime(Math.max(30000L, maxLifetime));
        cfg.setPoolName("RefontSocial-MySQL");
        cfg.addDataSourceProperty("cachePrepStmts", "true");
        cfg.addDataSourceProperty("prepStmtCacheSize", "250");
        cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        return cfg;
    }
}

