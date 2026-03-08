package ru.rizonchik.refontsocial.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.service.FriendsService;
import ru.rizonchik.refontsocial.util.Colors;
import ru.rizonchik.refontsocial.util.YamlUtil;

public final class FriendsCommand implements CommandExecutor, TabCompleter {
    private final RefontSocial plugin;

    public FriendsCommand(RefontSocial plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Colors.msg(this.plugin, "playerOnly"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("refontsocial.friends.use") && !player.hasPermission("refontsocial.use")) {
            player.sendMessage(Colors.msg(this.plugin, "noPermission"));
            return true;
        }
        FriendsService service = this.plugin.getFriendsService();
        if (service == null || !service.isEnabled()) {
            player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
            return true;
        }

        if (args.length == 0) {
            this.plugin.getGuiService().openFriends(player, 1);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("help")) {
            Colors.sendList(player, this.plugin, "friendsHelp");
            return true;
        }
        if (sub.equals("open")) {
            int page = 1;
            if (args.length >= 2) {
                try {
                    page = Math.max(1, Integer.parseInt(args[1]));
                } catch (Exception ignored) {
                    page = 1;
                }
            }
            this.plugin.getGuiService().openFriends(player, page);
            return true;
        }
        if (sub.equals("add") || sub.equals("request")) {
            if (args.length < 2) {
                Colors.sendList(player, this.plugin, "friendsHelp");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                target = Bukkit.getPlayer(args[1]);
            }
            if (target == null) {
                player.sendMessage(Colors.msg(this.plugin, "targetMustBeOnline"));
                return true;
            }
            FriendsService.RequestResult result = service.sendRequest(player.getUniqueId(), target.getUniqueId());
            switch (result) {
                case SENT:
                    player.sendMessage(Colors.msg(this.plugin, "friendsRequestSent", "%target%", target.getName()));
                    sendRequestMessage(target, player);
                    return true;
                case SELF:
                    player.sendMessage(Colors.msg(this.plugin, "friendsSelfDenied"));
                    return true;
                case ALREADY_FRIENDS:
                    player.sendMessage(Colors.msg(this.plugin, "friendsAlreadyFriends", "%target%", target.getName()));
                    return true;
                case ALREADY_SENT:
                    player.sendMessage(Colors.msg(this.plugin, "friendsRequestAlreadySent", "%target%", target.getName()));
                    return true;
                case TARGET_HAS_PENDING:
                    player.sendMessage(Colors.msg(this.plugin, "friendsRequestTargetHasPending", "%target%", target.getName()));
                    return true;
                default:
                    player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
                    return true;
            }
        }
        if (sub.equals("accept")) {
            if (args.length < 2) {
                Colors.sendList(player, this.plugin, "friendsHelp");
                return true;
            }
            Player requester = Bukkit.getPlayerExact(args[1]);
            if (requester == null) {
                requester = Bukkit.getPlayer(args[1]);
            }
            if (requester == null) {
                player.sendMessage(Colors.msg(this.plugin, "targetMustBeOnline"));
                return true;
            }
            FriendsService.AcceptResult result = service.accept(player.getUniqueId(), requester.getUniqueId());
            switch (result) {
                case ACCEPTED:
                    player.sendMessage(Colors.msg(this.plugin, "friendsAcceptedSelf", "%target%", requester.getName()));
                    requester.sendMessage(Colors.msg(this.plugin, "friendsAcceptedOther", "%target%", player.getName()));
                    return true;
                case NO_PENDING:
                    player.sendMessage(Colors.msg(this.plugin, "friendsNoPending"));
                    return true;
                case NO_PENDING_FROM_PLAYER:
                    player.sendMessage(Colors.msg(this.plugin, "friendsNoPendingFrom", "%target%", requester.getName()));
                    return true;
                case ALREADY_FRIENDS:
                    player.sendMessage(Colors.msg(this.plugin, "friendsAlreadyFriends", "%target%", requester.getName()));
                    return true;
                default:
                    player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
                    return true;
            }
        }
        if (sub.equals("deny") || sub.equals("decline")) {
            if (args.length < 2) {
                Colors.sendList(player, this.plugin, "friendsHelp");
                return true;
            }
            Player requester = Bukkit.getPlayerExact(args[1]);
            if (requester == null) {
                requester = Bukkit.getPlayer(args[1]);
            }
            if (requester == null) {
                player.sendMessage(Colors.msg(this.plugin, "targetMustBeOnline"));
                return true;
            }
            FriendsService.DenyResult result = service.deny(player.getUniqueId(), requester.getUniqueId());
            switch (result) {
                case DENIED:
                    player.sendMessage(Colors.msg(this.plugin, "friendsDeniedSelf", "%target%", requester.getName()));
                    requester.sendMessage(Colors.msg(this.plugin, "friendsDeniedOther", "%target%", player.getName()));
                    return true;
                case NO_PENDING:
                    player.sendMessage(Colors.msg(this.plugin, "friendsNoPending"));
                    return true;
                case NO_PENDING_FROM_PLAYER:
                    player.sendMessage(Colors.msg(this.plugin, "friendsNoPendingFrom", "%target%", requester.getName()));
                    return true;
                default:
                    player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
                    return true;
            }
        }
        if (sub.equals("remove") || sub.equals("delete")) {
            if (args.length < 2) {
                Colors.sendList(player, this.plugin, "friendsHelp");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (target == null || target.getUniqueId() == null) {
                player.sendMessage(Colors.msg(this.plugin, "playerNotFound"));
                return true;
            }
            boolean removed = service.removeFriend(player.getUniqueId(), target.getUniqueId());
            if (!removed) {
                player.sendMessage(Colors.msg(this.plugin, "friendsNotFriends", "%target%", target.getName() != null ? target.getName() : args[1]));
                return true;
            }
            player.sendMessage(Colors.msg(this.plugin, "friendsRemovedSelf", "%target%", target.getName() != null ? target.getName() : args[1]));
            if (target.isOnline() && target.getPlayer() != null) {
                target.getPlayer().sendMessage(Colors.msg(this.plugin, "friendsRemovedOther", "%target%", player.getName()));
            }
            return true;
        }
        if (sub.equals("carry")) {
            if (args.length < 2) {
                Colors.sendList(player, this.plugin, "friendsHelp");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                target = Bukkit.getPlayer(args[1]);
            }
            if (target == null) {
                player.sendMessage(Colors.msg(this.plugin, "targetMustBeOnline"));
                return true;
            }
            if (!service.areFriends(player.getUniqueId(), target.getUniqueId())) {
                player.sendMessage(Colors.msg(this.plugin, "friendsNotFriends", "%target%", target.getName()));
                return true;
            }
            double distance = service.getActionDistance();
            if (!service.areFriendsNearby(player, target, distance)) {
                player.sendMessage(Colors.msg(this.plugin, "friendsTooFar", "%distance%", String.valueOf((int)Math.round(distance))));
                return true;
            }
            if (player.isInsideVehicle()) {
                if (player.getVehicle() != null && player.getVehicle().getUniqueId().equals(target.getUniqueId())) {
                    player.leaveVehicle();
                    player.sendMessage(Colors.msg(this.plugin, "friendsCarryStoppedSelf", "%target%", target.getName()));
                    target.sendMessage(Colors.msg(this.plugin, "friendsCarryStoppedOther", "%target%", player.getName()));
                    this.plugin.getVisualIndicatorsHook().spawnSocialIndicator(player, target, "carry_stop");
                    return true;
                }
                player.sendMessage(Colors.msg(this.plugin, "friendsCarryAlreadyRiding"));
                return true;
            }
            if (!target.addPassenger(player)) {
                player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
                return true;
            }
            player.sendMessage(Colors.msg(this.plugin, "friendsCarryStartedSelf", "%target%", target.getName()));
            target.sendMessage(Colors.msg(this.plugin, "friendsCarryStartedOther", "%target%", player.getName()));
            this.plugin.getVisualIndicatorsHook().spawnSocialIndicator(player, target, "carry");
            return true;
        }

        if (isNumeric(sub)) {
            int page = Math.max(1, Integer.parseInt(sub));
            this.plugin.getGuiService().openFriends(player, page);
            return true;
        }

        Colors.sendList(player, this.plugin, "friendsHelp");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("refontsocial.friends.use") && !sender.hasPermission("refontsocial.use")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            List<String> base = new ArrayList<>(Arrays.asList("help", "open", "add", "accept", "deny", "remove", "carry"));
            String p = args[0].toLowerCase(Locale.ROOT);
            return base.stream().filter(s -> s.startsWith(p)).collect(Collectors.toList());
        }
        if (args.length == 2 && Arrays.asList("add", "accept", "deny", "remove", "carry").contains(args[0].toLowerCase(Locale.ROOT))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n != null && n.toLowerCase(Locale.ROOT).startsWith(prefix)).limit(30L).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private void sendRequestMessage(Player target, Player requester) {
        target.sendMessage(Colors.msg(this.plugin, "friendsRequestReceived", "%player%", requester.getName()));
        String acceptText = YamlUtil.messages(this.plugin).getString("friendsRequestAcceptButton", "&a[Accept]");
        String denyText = YamlUtil.messages(this.plugin).getString("friendsRequestDenyButton", "&c[Decline]");
        String hoverText = YamlUtil.messages(this.plugin).getString("friendsRequestHover", "&eClick to accept");
        TextComponent accept = new TextComponent(Colors.color(acceptText));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friends accept " + requester.getName()));
        accept.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Colors.color(hoverText)).create()));
        TextComponent deny = new TextComponent(Colors.color(denyText));
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/friends deny " + requester.getName()));
        deny.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(Colors.color(hoverText)).create()));
        target.spigot().sendMessage(accept, new TextComponent(" "), deny);
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        for (char c : value.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }
}
