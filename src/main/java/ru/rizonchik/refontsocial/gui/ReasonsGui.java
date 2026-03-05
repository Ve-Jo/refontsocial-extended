/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package ru.rizonchik.refontsocial.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.gui.AbstractGui;
import ru.rizonchik.refontsocial.service.ReputationService;
import ru.rizonchik.refontsocial.util.Colors;
import ru.rizonchik.refontsocial.util.ItemUtil;

public final class ReasonsGui
extends AbstractGui {
    private final RefontSocial plugin;
    private final ReputationService service;
    private final UUID target;
    private final String targetName;
    private final boolean like;
    private final List<String> tagKeys = new ArrayList<String>();

    public ReasonsGui(RefontSocial plugin, ReputationService service, UUID target, String targetName, boolean like) {
        this.plugin = plugin;
        this.service = service;
        this.target = target;
        this.targetName = targetName != null ? targetName : "\u0418\u0433\u0440\u043e\u043a";
        this.like = like;
    }

    @Override
    public void open(Player player) {
        String title = this.plugin.getConfig().getString("gui.reasons.title", "\u041f\u0440\u0438\u0447\u0438\u043d\u0430");
        int size = this.plugin.getConfig().getInt("gui.reasons.size", 54);
        if (size < 9) {
            size = 54;
        }
        if (size % 9 != 0) {
            size = 54;
        }
        this.inventory = Bukkit.createInventory(null, (int)size, (String)title);
        this.tagKeys.clear();
        this.tagKeys.addAll(this.service.getReasonTagKeys(this.like));
        if (this.tagKeys.isEmpty()) {
            this.service.vote(player, this.target, this.targetName, this.like);
            return;
        }
        int slot = 0;
        for (String key : this.tagKeys) {
            ItemMeta meta;
            if (slot >= 45) break;
            String display = this.service.getReasonTagDisplay(key);
            ItemStack it = ItemUtil.fromGui(this.plugin, "reason_tag", "%reason%", display);
            if (it == null || it.getType() == Material.AIR) {
                it = new ItemStack(Material.NAME_TAG);
            }
            if ((meta = it.getItemMeta()) != null) {
                List<String> lore;
                String name = meta.getDisplayName();
                if (name == null || name.trim().isEmpty() || name.equals(" ")) {
                    meta.setDisplayName("\u00a7f" + display);
                }
                if ((lore = meta.getLore()) == null) {
                    lore = new ArrayList<String>();
                }
                lore.add("\u00a77\u041d\u0430\u0436\u043c\u0438, \u0447\u0442\u043e\u0431\u044b \u0432\u044b\u0431\u0440\u0430\u0442\u044c");
                lore.add("\u00a77\u0418\u0433\u0440\u043e\u043a: \u00a7f" + this.targetName);
                lore.add(this.like ? "\u00a77\u041e\u0446\u0435\u043d\u043a\u0430: \u00a7a\u043b\u0430\u0439\u043a" : "\u00a77\u041e\u0446\u0435\u043d\u043a\u0430: \u00a7c\u0434\u0438\u0437\u043b\u0430\u0439\u043a");
                meta.setLore(lore);
                it.setItemMeta(meta);
            }
            this.inventory.setItem(slot++, it);
        }
        ItemStack filler = ItemUtil.fromGui(this.plugin, "filler", new String[0]);
        for (int i = 45; i < this.inventory.getSize(); ++i) {
            this.inventory.setItem(i, filler);
        }
        this.inventory.setItem(this.inventory.getSize() - 9, ItemUtil.fromGui(this.plugin, "back", new String[0]));
        player.sendMessage(Colors.msg(this.plugin, "reasonChooseTitle", "%target%", this.targetName));
        player.openInventory(this.inventory);
    }

    @Override
    public void onClick(Player player, int rawSlot, ItemStack clicked) {
        if (rawSlot == this.inventory.getSize() - 9) {
            player.closeInventory();
            return;
        }
        if (rawSlot < 0 || rawSlot >= 45) {
            return;
        }
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }
        if (rawSlot >= this.tagKeys.size()) {
            return;
        }
        String key = this.tagKeys.get(rawSlot);
        player.closeInventory();
        this.service.voteWithReason(player, this.target, this.targetName, this.like, key);
    }
}

