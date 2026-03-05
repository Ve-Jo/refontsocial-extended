/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 */
package ru.rizonchik.refontsocial.gui;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.gui.AbstractGui;
import ru.rizonchik.refontsocial.gui.CategoryTopGui;
import ru.rizonchik.refontsocial.gui.ProfileGui;
import ru.rizonchik.refontsocial.gui.RateGui;
import ru.rizonchik.refontsocial.gui.ReasonsGui;
import ru.rizonchik.refontsocial.service.ReputationService;
import ru.rizonchik.refontsocial.storage.TopCategory;

public final class GuiService
implements Listener {
    private final RefontSocial plugin;
    private final ReputationService service;
    private final Map<UUID, AbstractGui> open = new ConcurrentHashMap<UUID, AbstractGui>();

    public GuiService(RefontSocial plugin, ReputationService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public void openTop(Player player, int page) {
        this.openCategoryTop(player, TopCategory.SCORE, page);
    }

    public void openCategoryTop(Player player, TopCategory category, int page) {
        CategoryTopGui gui = new CategoryTopGui(this.plugin, this.service, category, page);
        this.open.put(player.getUniqueId(), gui);
        ((AbstractGui)gui).open(player);
    }

    public void openRate(Player player, UUID target, String targetName) {
        RateGui gui = new RateGui(this.plugin, this.service, target, targetName);
        this.open.put(player.getUniqueId(), gui);
        ((AbstractGui)gui).open(player);
    }

    public void openProfile(Player player, UUID target, String targetName) {
        ProfileGui gui = new ProfileGui(this.plugin, this.service, target, targetName);
        this.open.put(player.getUniqueId(), gui);
        ((AbstractGui)gui).open(player);
    }

    public void openReasons(Player player, UUID target, String targetName, boolean like) {
        ReasonsGui gui = new ReasonsGui(this.plugin, this.service, target, targetName, like);
        this.open.put(player.getUniqueId(), gui);
        ((AbstractGui)gui).open(player);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player)e.getWhoClicked();
        AbstractGui gui = this.open.get(player.getUniqueId());
        if (gui == null) {
            return;
        }
        if (e.getView() == null || e.getView().getTopInventory() == null) {
            return;
        }
        if (!e.getView().getTopInventory().equals(gui.getInventory())) {
            return;
        }
        e.setCancelled(true);
        gui.onClick(player, e.getRawSlot(), e.getCurrentItem());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) {
            return;
        }
        Player player = (Player)e.getPlayer();
        AbstractGui gui = this.open.get(player.getUniqueId());
        if (gui == null) {
            return;
        }
        if (e.getInventory() != null && e.getInventory().equals(gui.getInventory())) {
            this.open.remove(player.getUniqueId());
            gui.onClose(player);
        }
    }

    public void shutdown() {
        for (UUID uuid : this.open.keySet()) {
            Player p = Bukkit.getPlayer((UUID)uuid);
            if (p == null || !p.isOnline()) continue;
            p.closeInventory();
        }
        this.open.clear();
    }
}

