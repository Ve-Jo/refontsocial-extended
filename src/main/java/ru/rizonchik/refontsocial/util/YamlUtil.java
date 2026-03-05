/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.Configuration
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.rizonchik.refontsocial.util;

import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class YamlUtil {
    private static YamlConfiguration MESSAGES;
    private static YamlConfiguration GUI;
    private static YamlConfiguration TAGS;

    private YamlUtil() {
    }

    public static void saveResourceIfNotExists(JavaPlugin plugin, String name) {
        File f = new File(plugin.getDataFolder(), name);
        if (!f.exists()) {
            plugin.saveResource(name, false);
        }
    }

    public static void reloadMessages(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "messages.yml");
        MESSAGES = YamlUtil.load(f);
    }

    public static void reloadGui(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "gui.yml");
        GUI = YamlUtil.load(f);
    }

    public static void reloadTags(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "tags.yml");
        TAGS = YamlUtil.load(f);
    }

    public static YamlConfiguration messages(JavaPlugin plugin) {
        if (MESSAGES == null) {
            YamlUtil.reloadMessages(plugin);
        }
        return MESSAGES;
    }

    public static YamlConfiguration gui(JavaPlugin plugin) {
        if (GUI == null) {
            YamlUtil.reloadGui(plugin);
        }
        return GUI;
    }

    public static YamlConfiguration tags(JavaPlugin plugin) {
        if (TAGS == null) {
            YamlUtil.reloadTags(plugin);
        }
        return TAGS;
    }

    public static YamlConfiguration load(File file) {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)file);
        try {
            YamlConfiguration def;
            InputStreamReader reader;
            if (file.getName().equalsIgnoreCase("messages.yml") && (reader = new InputStreamReader(YamlUtil.class.getClassLoader().getResourceAsStream("messages.yml"), StandardCharsets.UTF_8)) != null) {
                def = YamlConfiguration.loadConfiguration((Reader)reader);
                cfg.setDefaults((Configuration)def);
            }
            if (file.getName().equalsIgnoreCase("gui.yml") && (reader = new InputStreamReader(YamlUtil.class.getClassLoader().getResourceAsStream("gui.yml"), StandardCharsets.UTF_8)) != null) {
                def = YamlConfiguration.loadConfiguration((Reader)reader);
                cfg.setDefaults((Configuration)def);
            }
            if (file.getName().equalsIgnoreCase("tags.yml") && (reader = new InputStreamReader(YamlUtil.class.getClassLoader().getResourceAsStream("tags.yml"), StandardCharsets.UTF_8)) != null) {
                def = YamlConfiguration.loadConfiguration((Reader)reader);
                cfg.setDefaults((Configuration)def);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return cfg;
    }

    public static void save(File file, YamlConfiguration cfg) {
        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            cfg.save(file);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

