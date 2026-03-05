/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.rizonchik.refontsocial.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import org.bukkit.plugin.java.JavaPlugin;

public final class LibraryManager {
    private final JavaPlugin plugin;

    public LibraryManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void ensureDriverPresent(String driverClass, String mavenPath, String fileName) {
        if (!this.plugin.getConfig().getBoolean("libraries.enabled", true)) {
            return;
        }
        if (this.isClassPresent(driverClass)) {
            this.plugin.getLogger().info("Driver found: " + driverClass);
            return;
        }
        File libsDir = new File(this.plugin.getDataFolder(), this.plugin.getConfig().getString("libraries.folder", "libs"));
        if (!libsDir.exists() && !libsDir.mkdirs()) {
            throw new RuntimeException("Cannot create libs folder: " + libsDir.getAbsolutePath());
        }
        File jar = new File(libsDir, fileName);
        if (!jar.exists()) {
            this.downloadFromRepos(mavenPath, jar);
        }
        this.addJarToClasspath(jar);
        if (!this.isClassPresent(driverClass)) {
            throw new RuntimeException("Driver still not found after loading jar: " + driverClass);
        }
        this.plugin.getLogger().info("Loaded driver: " + driverClass);
    }

    private void downloadFromRepos(String mavenPath, File target) {
        List<String> repos = this.plugin.getConfig().getStringList("libraries.repositories");
        if (repos == null || repos.isEmpty()) {
            throw new RuntimeException("No repositories configured in libraries.repositories");
        }
        IOException last = null;
        for (String repo : repos) {
            if (repo == null || repo.trim().isEmpty()) continue;
            if (!repo.endsWith("/")) {
                repo = repo + "/";
            }
            String url = repo + mavenPath;
            this.plugin.getLogger().info("Downloading: " + url);
            try {
                this.download(url, target);
                this.plugin.getLogger().info("Downloaded to: " + target.getAbsolutePath() + " (" + target.length() + " bytes)");
                return;
            }
            catch (IOException e) {
                last = e;
                this.plugin.getLogger().warning("Failed: " + url + " -> " + e.getMessage());
            }
        }
        throw new RuntimeException("Failed to download dependency: " + mavenPath, last);
    }

    private void download(String url, File target) throws IOException {
        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
        if (tmp.exists()) {
            tmp.delete();
        }
        URL u = new URL(url);
        try (InputStream in = u.openStream();
             FileOutputStream out = new FileOutputStream(tmp);){
            int r;
            byte[] buf = new byte[8192];
            while ((r = in.read(buf)) != -1) {
                out.write(buf, 0, r);
            }
        }
        if (target.exists() && !target.delete()) {
            throw new IOException("Cannot replace existing jar: " + target.getAbsolutePath());
        }
        if (!tmp.renameTo(target)) {
            throw new IOException("Cannot move temp file to target: " + target.getAbsolutePath());
        }
    }

    private boolean isClassPresent(String name) {
        try {
            Class.forName(name, false, this.plugin.getClass().getClassLoader());
            return true;
        }
        catch (Throwable ignored) {
            return false;
        }
    }

    private void addJarToClasspath(File jar) {
        try {
            ClassLoader cl = this.plugin.getClass().getClassLoader();
            if (cl instanceof URLClassLoader) {
                URLClassLoader urlCl = (URLClassLoader)cl;
                Method addUrl = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addUrl.setAccessible(true);
                addUrl.invoke((Object)urlCl, jar.toURI().toURL());
                return;
            }
            Method addUrl = cl.getClass().getDeclaredMethod("addURL", URL.class);
            addUrl.setAccessible(true);
            addUrl.invoke((Object)cl, jar.toURI().toURL());
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to add jar to classpath: " + jar.getAbsolutePath(), e);
        }
    }
}

