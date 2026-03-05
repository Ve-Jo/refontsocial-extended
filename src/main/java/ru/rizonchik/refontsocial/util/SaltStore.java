/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.rizonchik.refontsocial.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.security.SecureRandom;
import java.util.Base64;
import org.bukkit.plugin.java.JavaPlugin;

public final class SaltStore {
    private SaltStore() {
    }

    public static String getOrCreate(JavaPlugin plugin) {
        File f = new File(plugin.getDataFolder(), "ip_salt.txt");
        try {
            byte[] b;
            String s;
            if (f.exists() && !(s = new String(b = Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8).trim()).isEmpty()) {
                return s;
            }
            byte[] salt = new byte[32];
            new SecureRandom().nextBytes(salt);
            s = Base64.getEncoder().encodeToString(salt);
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            Files.write(f.toPath(), s.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
            return s;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to load/create ip salt", e);
        }
    }
}

