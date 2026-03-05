/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.plugin.Plugin
 */
package ru.rizonchik.refontsocial.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.gui.AbstractGui;
import ru.rizonchik.refontsocial.service.ReputationService;
import ru.rizonchik.refontsocial.storage.TopCategory;
import ru.rizonchik.refontsocial.storage.model.PlayerRep;
import ru.rizonchik.refontsocial.util.ItemUtil;
import ru.rizonchik.refontsocial.util.NumberUtil;

public final class TopGui
extends AbstractGui {
    private final RefontSocial plugin;
    private final ReputationService service;
    private final int page;
    private final Map<Integer, UUID> slotTargets = new HashMap<Integer, UUID>();
    private final Map<Integer, String> slotNames = new HashMap<Integer, String>();

    public TopGui(RefontSocial plugin, ReputationService service, int page) {
        this.plugin = plugin;
        this.service = service;
        this.page = page;
    }

    @Override
    public void open(Player player) {
        String title = this.plugin.getConfig().getString("gui.top.title", "\u0420\u0435\u043f\u0443\u0442\u0430\u0446\u0438\u044f \u2022 \u0422\u043e\u043f");
        int size = this.plugin.getConfig().getInt("gui.top.size", 54);
        if (size < 9) {
            size = 54;
        }
        if (size % 9 != 0) {
            size = 54;
        }
        this.inventory = Bukkit.createInventory(null, (int)size, (String)title);
        this.fillFrame();
        this.inventory.setItem(this.inventory.getSize() - 9, ItemUtil.fromGui(this.plugin, "back", new String[0]));
        this.inventory.setItem(this.inventory.getSize() - 1, ItemUtil.fromGui(this.plugin, "next", new String[0]));
        player.openInventory(this.inventory);
        ItemStack loading = new ItemStack(Material.PAPER);
        ItemMeta loadingMeta = loading.getItemMeta();
        if (loadingMeta != null) {
            loadingMeta.setDisplayName("\u00a7f\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430...");
            loading.setItemMeta(loadingMeta);
        }
        this.inventory.setItem(22, loading);
        Player viewer = player;
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            int pageSize = this.plugin.getConfig().getInt("gui.top.pageSize", 45);
            if (pageSize < 1) {
                pageSize = 45;
            }
            int pageSizeFinal = pageSize;
            int offset = (this.page - 1) * pageSizeFinal;
            List<PlayerRep> top = this.service.getTopCached(TopCategory.SCORE, pageSizeFinal, offset);
            ArrayList<String> names = new ArrayList<String>(top.size());
            for (PlayerRep rep : top) {
                String name = rep.getName();
                if (name == null || name.trim().isEmpty()) {
                    name = this.service.getNameCached(rep.getUuid());
                }
                if (name == null || name.trim().isEmpty()) {
                    name = rep.getUuid().toString().substring(0, 8);
                }
                names.add(name);
            }
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                int i;
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
                for (i = 0; i < pageSizeFinal; ++i) {
                    this.inventory.setItem(i, null);
                }
                this.slotTargets.clear();
                this.slotNames.clear();
                for (i = 0; i < top.size() && i < pageSizeFinal; ++i) {
                    PlayerRep rep = (PlayerRep)top.get(i);
                    String name = names.size() > i ? (String)names.get(i) : rep.getUuid().toString().substring(0, 8);
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta)head.getItemMeta();
                    meta.setDisplayName("\u00a7f#" + (offset + i + 1) + " \u00a77\u2014 \u00a7f" + name);
                    ArrayList<String> lore = new ArrayList<String>();
                    lore.add("\u00a77\u0420\u0435\u0439\u0442\u0438\u043d\u0433: \u00a7f" + NumberUtil.formatScore(this.plugin, rep.getScore()));
                    lore.add("\u00a77\u041b\u0430\u0439\u043a\u0438: \u00a7a" + rep.getLikes() + " \u00a77/ \u0414\u0438\u0437\u043b\u0430\u0439\u043a\u0438: \u00a7c" + rep.getDislikes());
                    lore.add("\u00a77\u0413\u043e\u043b\u043e\u0441\u043e\u0432: \u00a7f" + rep.getVotes());
                    lore.add("");
                    lore.add("\u00a7e\u041d\u0430\u0436\u043c\u0438, \u0447\u0442\u043e\u0431\u044b \u043e\u0446\u0435\u043d\u0438\u0442\u044c");
                    meta.setLore(lore);
                    try {
                        OfflinePlayer off = Bukkit.getOfflinePlayer((UUID)rep.getUuid());
                        meta.setOwningPlayer(off);
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                    head.setItemMeta((ItemMeta)meta);
                    this.inventory.setItem(i, head);
                    this.slotTargets.put(i, rep.getUuid());
                    this.slotNames.put(i, name);
                }
            });
        });
    }

    private void fillFrame() {
        ItemStack filler = ItemUtil.fromGui(this.plugin, "filler", new String[0]);
        for (int i = 45; i < this.inventory.getSize(); ++i) {
            this.inventory.setItem(i, filler);
        }
    }

    @Override
    public void onClick(Player player, int rawSlot, ItemStack clicked) {
        if (rawSlot < 0) {
            return;
        }
        int size = this.inventory.getSize();
        int pageSize = this.plugin.getConfig().getInt("gui.top.pageSize", 45);
        if (pageSize < 1) {
            pageSize = 45;
        }
        if (rawSlot < pageSize) {
            ItemStack item = this.inventory.getItem(rawSlot);
            if (item == null || item.getType() != Material.PLAYER_HEAD) {
                return;
            }
            if (!this.slotTargets.containsKey(rawSlot)) {
                return;
            }
            UUID target = this.slotTargets.get(rawSlot);
            String name = this.slotNames.get(rawSlot);
            player.closeInventory();
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.plugin.getGuiService().openRate(player, target, name));
            return;
        }
        if (rawSlot == size - 9) {
            int newPage = this.page - 1;
            if (newPage < 1) {
                newPage = 1;
            }
            this.plugin.getGuiService().openTop(player, newPage);
            return;
        }
        if (rawSlot == size - 1) {
            this.plugin.getGuiService().openTop(player, this.page + 1);
        }
    }
}

