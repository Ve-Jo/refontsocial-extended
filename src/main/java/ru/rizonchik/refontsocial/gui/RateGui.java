/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package ru.rizonchik.refontsocial.gui;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.gui.AbstractGui;
import ru.rizonchik.refontsocial.service.ReputationService;
import ru.rizonchik.refontsocial.storage.model.PlayerRep;
import ru.rizonchik.refontsocial.util.ItemUtil;
import ru.rizonchik.refontsocial.util.NumberUtil;

public final class RateGui
extends AbstractGui {
    private final RefontSocial plugin;
    private final ReputationService service;
    private final UUID target;
    private final String targetName;

    public RateGui(RefontSocial plugin, ReputationService service, UUID target, String targetName) {
        this.plugin = plugin;
        this.service = service;
        this.target = target;
        this.targetName = targetName != null ? targetName : "\u0418\u0433\u0440\u043e\u043a";
    }

    @Override
    public void open(Player player) {
        String title = this.plugin.getConfig().getString("gui.rate.title", "\u041e\u0446\u0435\u043d\u043a\u0430");
        int size = this.plugin.getConfig().getInt("gui.rate.size", 27);
        if (size < 9) {
            size = 27;
        }
        if (size % 9 != 0) {
            size = 27;
        }
        this.inventory = Bukkit.createInventory(null, (int)size, (String)title);
        ItemStack loading = new ItemStack(Material.PAPER);
        ItemMeta meta = loading.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("\u00a7f\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430...");
            loading.setItemMeta(meta);
        }
        this.inventory.setItem(13, loading);
        player.openInventory(this.inventory);
        Player viewer = player;
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            PlayerRep rep = this.service.getOrCreate(this.target, this.targetName);
            String score = NumberUtil.formatScore(this.plugin, rep.getScore());
            String likes = String.valueOf(rep.getLikes());
            String dislikes = String.valueOf(rep.getDislikes());
            String votes = String.valueOf(rep.getVotes());
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (!viewer.isOnline()) {
                    return;
                }
                if (viewer.getOpenInventory() == null) {
                    return;
                }
                if (viewer.getOpenInventory().getTopInventory() == null) {
                    return;
                }
                if (!viewer.getOpenInventory().getTopInventory().equals(this.inventory)) {
                    return;
                }
                ItemStack like = ItemUtil.fromGui(this.plugin, "like", "%score%", score);
                ItemStack dislike = ItemUtil.fromGui(this.plugin, "dislike", "%score%", score);
                ItemStack info = ItemUtil.fromGui(this.plugin, "info", "%target%", this.targetName, "%score%", score, "%likes%", likes, "%dislikes%", dislikes, "%votes%", votes);
                this.inventory.setItem(11, like);
                this.inventory.setItem(15, dislike);
                this.inventory.setItem(13, info);
            });
        });
    }

    @Override
    public void onClick(Player player, int rawSlot, ItemStack clicked) {
        if (clicked == null) {
            return;
        }
        Material type = clicked.getType();
        if (rawSlot == 11 && type != Material.AIR) {
            player.closeInventory();
            this.service.vote(player, this.target, this.targetName, true);
            return;
        }
        if (rawSlot == 15 && type != Material.AIR) {
            player.closeInventory();
            this.service.vote(player, this.target, this.targetName, false);
        }
    }
}

