/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.rizonchik.refontsocial.storage.sql;

import java.io.File;
import com.zaxxer.hikari.HikariConfig;
import org.bukkit.plugin.java.JavaPlugin;
import ru.rizonchik.refontsocial.storage.sql.SqlStorage;

public final class SqliteStorage
extends SqlStorage {
    public SqliteStorage(JavaPlugin plugin) {
        super(plugin);
    }

    @Override
    protected boolean isMysql() {
        return false;
    }

    @Override
    protected HikariConfig buildConfig() {
        String fileName = this.plugin.getConfig().getString("storage.sqlite.file", "data.db");
        File file = new File(this.plugin.getDataFolder(), fileName);
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl("jdbc:sqlite:" + file.getAbsolutePath());
        cfg.setMaximumPoolSize(1);
        cfg.setPoolName("RefontSocial-SQLite");
        cfg.setConnectionTestQuery("SELECT 1");
        return cfg;
    }
}

