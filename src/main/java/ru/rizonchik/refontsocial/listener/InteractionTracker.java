/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.rizonchik.refontsocial.listener;

import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class InteractionTracker
implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, Map<UUID, Long>> lastInteraction = new ConcurrentHashMap<UUID, Map<UUID, Long>>();
    private int taskId = -1;

    public InteractionTracker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        double radius = this.plugin.getConfig().getDouble("antiAbuse.requireInteraction.radiusBlocks", 100.0);
        long period = this.plugin.getConfig().getLong("antiAbuse.requireInteraction.taskPeriodTicks", 40L);
        if (period < 20L) {
            period = 20L;
        }
        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask((Plugin)this.plugin, () -> this.tick(radius), period, period);
    }

    public void shutdown() {
        if (this.taskId != -1) {
            Bukkit.getScheduler().cancelTask(this.taskId);
            this.taskId = -1;
        }
        this.lastInteraction.clear();
    }

    private void tick(double radius) {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            ArrayList<Player> nearby = new ArrayList<Player>();
            for (Entity e : p.getNearbyEntities(radius, radius, radius)) {
                if (!(e instanceof Player)) continue;
                nearby.add((Player)e);
            }
            for (Player other : nearby) {
                if (other == null || !other.isOnline() || other.getUniqueId().equals(p.getUniqueId())) continue;
                UUID a = p.getUniqueId();
                UUID b = other.getUniqueId();
                this.lastInteraction.computeIfAbsent(a, k -> new ConcurrentHashMap()).put(b, now);
            }
        }
    }

    public boolean hasRecentInteraction(UUID voter, UUID target, long validMs) {
        Map<UUID, Long> map = this.lastInteraction.get(voter);
        if (map == null) {
            return false;
        }
        Long t = map.get(target);
        if (t == null) {
            return false;
        }
        return System.currentTimeMillis() - t <= validMs;
    }
}

