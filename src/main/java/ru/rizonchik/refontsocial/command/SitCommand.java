package ru.rizonchik.refontsocial.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.service.SitManager;
import ru.rizonchik.refontsocial.util.Colors;

public final class SitCommand implements CommandExecutor {
    private final RefontSocial plugin;

    public SitCommand(RefontSocial plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Colors.msg(this.plugin, "playerOnly"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("refontsocial.sit.use") && !player.hasPermission("refontsocial.use")) {
            player.sendMessage(Colors.msg(this.plugin, "noPermission"));
            return true;
        }
        SitManager sitManager = this.plugin.getSitManager();
        if (sitManager == null) {
            player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
            return true;
        }
        sitManager.toggleSit(player);
        return true;
    }
}
