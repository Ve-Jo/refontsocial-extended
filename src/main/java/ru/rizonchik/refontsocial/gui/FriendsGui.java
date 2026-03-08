package ru.rizonchik.refontsocial.gui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.model.FriendEntry;
import ru.rizonchik.refontsocial.service.FriendsService;
import ru.rizonchik.refontsocial.util.ItemUtil;

public final class FriendsGui extends AbstractGui {
    private final RefontSocial plugin;
    private final FriendsService friendsService;
    private final int page;
    private final Map<Integer, UUID> slotTargets = new HashMap<>();
    private final Map<Integer, String> slotNames = new HashMap<>();

    public FriendsGui(RefontSocial plugin, FriendsService friendsService, int page) {
        this.plugin = plugin;
        this.friendsService = friendsService;
        this.page = page;
    }

    @Override
    public void open(Player viewer) {
        String title = this.plugin.getConfig().getString("gui.friends.title", "Друзья");
        int size = this.plugin.getConfig().getInt("gui.friends.size", 54);
        if (size < 9 || size % 9 != 0) {
            size = 54;
        }
        this.inventory = Bukkit.createInventory(null, size, title);
        fillFrame();
        this.inventory.setItem(this.inventory.getSize() - 9, ItemUtil.fromGui(this.plugin, "back"));
        this.inventory.setItem(this.inventory.getSize() - 1, ItemUtil.fromGui(this.plugin, "next"));
        viewer.openInventory(this.inventory);

        ItemStack loading = new ItemStack(Material.PAPER);
        ItemMeta meta = loading.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§fЗагрузка...");
            loading.setItemMeta(meta);
        }
        this.inventory.setItem(22, loading);

        Player viewerFinal = viewer;
        Bukkit.getScheduler().runTaskAsynchronously((Plugin) this.plugin, () -> {
            List<FriendEntry> entries = this.friendsService.getFriends(viewerFinal.getUniqueId());
            entries.sort(friendComparator());
            List<FriendView> views = new ArrayList<>(entries.size());
            for (FriendEntry entry : entries) {
                views.add(buildFriendView(entry));
            }
            Bukkit.getScheduler().runTask((Plugin) this.plugin, () -> render(viewerFinal, views));
        });
    }

    @Override
    public void onClick(Player player, int rawSlot, ItemStack clicked) {
        if (rawSlot < 0) {
            return;
        }
        int size = this.inventory.getSize();
        int pageSize = getPageSize();
        if (rawSlot < pageSize) {
            if (!this.slotTargets.containsKey(rawSlot)) {
                return;
            }
            UUID target = this.slotTargets.get(rawSlot);
            String name = this.slotNames.getOrDefault(rawSlot, "Игрок");
            player.closeInventory();
            Bukkit.getScheduler().runTask((Plugin) this.plugin, () -> this.plugin.getGuiService().openProfile(player, target, name));
            return;
        }
        if (rawSlot == size - 9) {
            int newPage = Math.max(1, this.page - 1);
            this.plugin.getGuiService().openFriends(player, newPage);
            return;
        }
        if (rawSlot == size - 1) {
            this.plugin.getGuiService().openFriends(player, this.page + 1);
        }
    }

    private void render(Player viewer, List<FriendView> friends) {
        if (!viewer.isOnline() || viewer.getOpenInventory() == null || viewer.getOpenInventory().getTopInventory() == null) {
            return;
        }
        if (!viewer.getOpenInventory().getTopInventory().equals(this.inventory)) {
            return;
        }
        int pageSize = getPageSize();
        for (int i = 0; i < pageSize; ++i) {
            this.inventory.setItem(i, null);
        }
        this.slotTargets.clear();
        this.slotNames.clear();
        int offset = (this.page - 1) * pageSize;
        if (friends.isEmpty()) {
            ItemStack emptyItem = ItemUtil.fromGui(this.plugin, "friends_empty");
            ensureMeta(emptyItem, "§fДрузья", List.of("§7Список друзей пуст.", "§7Используй §f/friends add <ник>§7."));
            this.inventory.setItem(22, emptyItem);
            return;
        }
        for (int i = 0; i < pageSize; ++i) {
            int index = offset + i;
            if (index >= friends.size()) {
                break;
            }
            FriendView view = friends.get(index);
            ItemStack head = ItemUtil.fromGui(this.plugin, "friends_entry",
                    "%player%", view.name,
                    "%status%", view.status,
                    "%playtime%", view.playtime,
                    "%last_join%", view.lastJoin,
                    "%since%", view.friendSince);
            if (head.getType() != Material.PLAYER_HEAD) {
                head.setType(Material.PLAYER_HEAD);
            }
            List<String> fallbackLore = new ArrayList<>();
            fallbackLore.add("§7Статус: " + view.status);
            fallbackLore.add("§7Наиграно: §f" + view.playtime);
            fallbackLore.add("§7Последний вход: §f" + view.lastJoin);
            fallbackLore.add("§7Дружите с: §f" + view.friendSince);
            fallbackLore.add("");
            fallbackLore.add("§eНажми, чтобы открыть профиль");
            ensureMeta(head, "§f" + view.name, fallbackLore);
            ItemMeta meta = head.getItemMeta();
            if (meta instanceof SkullMeta && view.offlinePlayer != null) {
                SkullMeta skullMeta = (SkullMeta) meta;
                skullMeta.setOwningPlayer(view.offlinePlayer);
                head.setItemMeta(skullMeta);
            }
            this.inventory.setItem(i, head);
            this.slotTargets.put(i, view.uuid);
            this.slotNames.put(i, view.name);
        }
    }

    private int getPageSize() {
        int pageSize = this.plugin.getConfig().getInt("gui.friends.pageSize", 45);
        if (pageSize < 1) {
            pageSize = 45;
        }
        return pageSize;
    }

    private void fillFrame() {
        ItemStack filler = ItemUtil.fromGui(this.plugin, "filler");
        for (int i = getPageSize(); i < this.inventory.getSize(); ++i) {
            this.inventory.setItem(i, filler);
        }
    }

    private Comparator<FriendEntry> friendComparator() {
        return (a, b) -> {
            boolean onlineA = isOnline(a.getFriend());
            boolean onlineB = isOnline(b.getFriend());
            if (onlineA != onlineB) {
                return onlineA ? -1 : 1;
            }
            String nameA = resolveName(a.getFriend());
            String nameB = resolveName(b.getFriend());
            return nameA.compareToIgnoreCase(nameB);
        };
    }

    private FriendView buildFriendView(FriendEntry entry) {
        UUID friendId = entry.getFriend();
        OfflinePlayer offline = Bukkit.getOfflinePlayer(friendId);
        String name = resolveName(friendId);
        boolean online = isOnline(friendId);
        String status = online ? this.plugin.getConfig().getString("friends.statusOnline", "§aОнлайн")
                : this.plugin.getConfig().getString("friends.statusOffline", "§7Оффлайн");
        String playtime = formatPlaytime(offline);
        String lastJoin = formatLastJoin(offline);
        String friendSince = formatFriendSince(entry.getSinceMillis());
        return new FriendView(friendId, name, offline, status, playtime, lastJoin, friendSince);
    }

    private String resolveName(UUID friendId) {
        String name = this.plugin.getStorage().getLastKnownName(friendId);
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        OfflinePlayer offline = Bukkit.getOfflinePlayer(friendId);
        if (offline.getName() != null && !offline.getName().trim().isEmpty()) {
            return offline.getName();
        }
        return friendId.toString().substring(0, 8);
    }

    private boolean isOnline(UUID friendId) {
        Player friend = Bukkit.getPlayer(friendId);
        return friend != null && friend.isOnline();
    }

    private String formatPlaytime(OfflinePlayer offline) {
        long ticks = 0L;
        try {
            ticks = offline.getStatistic(Statistic.PLAY_ONE_MINUTE);
        } catch (Throwable ignored) {
        }
        long totalSeconds = ticks / 20L;
        long totalMinutes = totalSeconds / 60L;
        long days = totalMinutes / 1440L;
        long hours = (totalMinutes % 1440L) / 60L;
        long minutes = totalMinutes % 60L;
        if (days > 0) {
            return days + "д " + hours + "ч";
        }
        if (hours > 0) {
            return hours + "ч " + minutes + "м";
        }
        return Math.max(1, minutes) + "м";
    }

    private String formatLastJoin(OfflinePlayer offline) {
        long last = 0L;
        try {
            last = offline.getLastLogin();
        } catch (Throwable ignored) {
            last = offline.getLastPlayed();
        }
        if (last <= 0L) {
            return this.plugin.getConfig().getString("friends.notPlayedText", "-");
        }
        String pattern = this.plugin.getConfig().getString("friends.dateFormat", "dd.MM.yyyy HH:mm");
        try {
            return new SimpleDateFormat(pattern, Locale.ROOT).format(new Date(last));
        } catch (Exception ignored) {
            return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(last));
        }
    }

    private String formatFriendSince(long sinceMillis) {
        if (sinceMillis <= 0L) {
            return this.plugin.getConfig().getString("friends.notPlayedText", "-");
        }
        String pattern = this.plugin.getConfig().getString("friends.dateFormat", "dd.MM.yyyy HH:mm");
        try {
            return new SimpleDateFormat(pattern, Locale.ROOT).format(new Date(sinceMillis));
        } catch (Exception ignored) {
            return new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ROOT).format(new Date(sinceMillis));
        }
    }

    private static final class FriendView {
        private final UUID uuid;
        private final String name;
        private final OfflinePlayer offlinePlayer;
        private final String status;
        private final String playtime;
        private final String lastJoin;
        private final String friendSince;

        private FriendView(UUID uuid, String name, OfflinePlayer offlinePlayer, String status, String playtime, String lastJoin, String friendSince) {
            this.uuid = uuid;
            this.name = name;
            this.offlinePlayer = offlinePlayer;
            this.status = status;
            this.playtime = playtime;
            this.lastJoin = lastJoin;
            this.friendSince = friendSince;
        }
    }

    private void ensureMeta(ItemStack item, String name, List<String> lore) {
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        boolean missingName = meta == null || meta.getDisplayName() == null || meta.getDisplayName().trim().isEmpty();
        if (!missingName) {
            return;
        }
        if (meta == null) {
            meta = Bukkit.getItemFactory().getItemMeta(item.getType());
            if (meta == null) {
                return;
            }
        }
        meta.setDisplayName(name);
        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
