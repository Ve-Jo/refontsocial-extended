/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 */
package ru.rizonchik.refontsocial.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.storage.TopCategory;
import ru.rizonchik.refontsocial.util.Colors;
import ru.rizonchik.refontsocial.util.NumberUtil;

public final class ReputationCommand
implements CommandExecutor,
TabCompleter {
    private final RefontSocial plugin;

    public ReputationCommand(RefontSocial plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("repreload")) {
            return this.handleReload(sender);
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            return this.handleReload(sender);
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(Colors.msg(this.plugin, "playerOnly", new String[0]));
            return true;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("refontsocial.use")) {
            player.sendMessage(Colors.msg(this.plugin, "noPermission", new String[0]));
            return true;
        }
        if (args.length == 0) {
            this.plugin.getReputationService().sendShow(player, player.getUniqueId(), player.getName());
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("help")) {
            Colors.sendList((CommandSender)player, this.plugin, "help");
            return true;
        }
        if (sub.equals("profile")) {
            if (args.length < 2) {
                Colors.sendList((CommandSender)player, this.plugin, "help");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer((String)args[1]);
            if (target == null || target.getUniqueId() == null) {
                player.sendMessage(Colors.msg(this.plugin, "playerNotFound", new String[0]));
                return true;
            }
            this.plugin.getGuiService().openProfile(player, target.getUniqueId(), target.getName());
            return true;
        }
        if (sub.equals("top")) {
            TopCategory category = TopCategory.SCORE;
            int page = 1;
            if (args.length >= 2) {
                String c = args[1].toUpperCase(Locale.ROOT);
                if (c.equals("SCORE")) {
                    category = TopCategory.SCORE;
                } else if (c.equals("LIKES")) {
                    category = TopCategory.LIKES;
                } else if (c.equals("DISLIKES")) {
                    category = TopCategory.DISLIKES;
                } else if (c.equals("VOTES")) {
                    category = TopCategory.VOTES;
                } else {
                    page = NumberUtil.parseInt(args[1], 1);
                }
            }
            if (args.length >= 3) {
                page = NumberUtil.parseInt(args[2], 1);
            }
            if (page < 1) {
                page = 1;
            }
            this.plugin.getGuiService().openCategoryTop(player, category, page);
            return true;
        }
        if (sub.equals("like") || sub.equals("dislike")) {
            if (args.length < 2) {
                Colors.sendList((CommandSender)player, this.plugin, "help");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer((String)args[1]);
            if (target == null || target.getUniqueId() == null) {
                player.sendMessage(Colors.msg(this.plugin, "playerNotFound", new String[0]));
                return true;
            }
            boolean like = sub.equals("like");
            boolean reasonsEnabled = this.plugin.getConfig().getBoolean("reasons.enabled", true);
            if (reasonsEnabled) {
                this.plugin.getGuiService().openReasons(player, target.getUniqueId(), target.getName(), like);
            } else {
                this.plugin.getReputationService().vote(player, target.getUniqueId(), target.getName(), like);
            }
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer((String)args[0]);
        if (target == null || target.getUniqueId() == null) {
            player.sendMessage(Colors.msg(this.plugin, "playerNotFound", new String[0]));
            return true;
        }
        this.plugin.getGuiService().openRate(player, target.getUniqueId(), target.getName());
        return true;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean canUse = sender.hasPermission("refontsocial.use");
        boolean canAdmin = sender.hasPermission("refontsocial.admin");
        if (args.length == 1) {
            ArrayList<String> base = new ArrayList<String>();
            if (canUse) {
                base.addAll(Arrays.asList("help", "top", "like", "dislike", "profile"));
            }
            if (canAdmin) {
                base.add("reload");
            }
            if (base.isEmpty()) {
                return Collections.emptyList();
            }
            String p = args[0].toLowerCase(Locale.ROOT);
            return base.stream().filter(s -> s.startsWith(p)).collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("like") || args[0].equalsIgnoreCase("dislike") || args[0].equalsIgnoreCase("profile"))) {
            if (!canUse) {
                return Collections.emptyList();
            }
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName).filter(n -> n.toLowerCase(Locale.ROOT).startsWith(prefix)).limit(30L).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            if (!canUse) {
                return Collections.emptyList();
            }
            String p = args[1].toLowerCase(Locale.ROOT);
            return Arrays.asList("score", "likes", "dislikes", "votes").stream().filter(s -> s.startsWith(p)).collect(Collectors.toList());
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("top")) {
            if (!canUse) {
                return Collections.emptyList();
            }
            return Collections.singletonList("page");
        }
        return Collections.emptyList();
    }

    private boolean handleReload(CommandSender sender) {
        if (!sender.hasPermission("refontsocial.admin")) {
            sender.sendMessage(Colors.msg(this.plugin, "noPermission", new String[0]));
            return true;
        }
        this.plugin.reloadPlugin();
        sender.sendMessage(Colors.msg(this.plugin, "reloaded", new String[0]));
        return true;
    }
}

