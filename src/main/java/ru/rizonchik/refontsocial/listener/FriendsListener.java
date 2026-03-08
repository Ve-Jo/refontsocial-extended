package ru.rizonchik.refontsocial.listener;

import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.service.FriendsService;
import ru.rizonchik.refontsocial.util.Colors;

public final class FriendsListener implements Listener {
    private final RefontSocial plugin;
    private final FriendsService friendsService;

    public FriendsListener(RefontSocial plugin, FriendsService friendsService) {
        this.plugin = plugin;
        this.friendsService = friendsService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (this.friendsService == null || !this.friendsService.joinLeaveMessagesEnabled()) {
            return;
        }
        Player joining = event.getPlayer();
        UUID joiningId = joining.getUniqueId();
        String joiningName = joining.getName();
        Bukkit.getScheduler().runTaskAsynchronously((Plugin) this.plugin, () -> {
            List<UUID> friends = this.friendsService.getFriendIds(joiningId);
            if (friends.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTask((Plugin) this.plugin, () -> notifyFriends(friends, "friendsJoin", joiningName));
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (this.friendsService == null || !this.friendsService.joinLeaveMessagesEnabled()) {
            return;
        }
        Player quitting = event.getPlayer();
        UUID quittingId = quitting.getUniqueId();
        String quittingName = quitting.getName();
        Bukkit.getScheduler().runTaskAsynchronously((Plugin) this.plugin, () -> {
            List<UUID> friends = this.friendsService.getFriendIds(quittingId);
            if (friends.isEmpty()) {
                return;
            }
            Bukkit.getScheduler().runTask((Plugin) this.plugin, () -> notifyFriends(friends, "friendsLeave", quittingName));
        });
    }

    private void notifyFriends(List<UUID> friends, String messageKey, String targetName) {
        for (UUID friendId : friends) {
            Player friend = Bukkit.getPlayer(friendId);
            if (friend == null || !friend.isOnline()) {
                continue;
            }
            friend.sendMessage(Colors.msg(this.plugin, messageKey, "%player%", targetName));
        }
    }
}
