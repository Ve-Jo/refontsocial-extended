/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.plugin.Plugin
 */
package ru.rizonchik.refontsocial.listener;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.util.SaltStore;
import ru.rizonchik.refontsocial.util.SecurityUtil;

public final class SeenListener
implements Listener {
    private final RefontSocial plugin;

    public SeenListener(RefontSocial plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String ip = null;
        try {
            if (p.getAddress() != null && p.getAddress().getAddress() != null) {
                ip = p.getAddress().getAddress().getHostAddress();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        String ipFinal = ip;
        String name = p.getName();
        UUID uuid = p.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            String salt = SaltStore.getOrCreate(this.plugin);
            String ipHash = ipFinal == null ? null : SecurityUtil.sha256(ipFinal + "|" + salt);
            this.plugin.getStorage().markSeen(uuid, name, ipHash);
        });
    }
}

