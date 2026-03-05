package ru.rizonchik.refontsocial.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.model.Gender;
import ru.rizonchik.refontsocial.service.GenderService;
import ru.rizonchik.refontsocial.util.Colors;

public final class GenderCommand implements CommandExecutor, TabCompleter {
    private static final List<String> VALUES = Arrays.asList("male", "female", "nonbinary", "other", "undisclosed");

    private final RefontSocial plugin;

    public GenderCommand(RefontSocial plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Colors.msg(this.plugin, "playerOnly"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("refontsocial.gender.use") && !player.hasPermission("refontsocial.use")) {
            player.sendMessage(Colors.msg(this.plugin, "noPermission"));
            return true;
        }

        GenderService service = this.plugin.getGenderService();
        if (service == null) {
            player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("show")) {
            player.sendMessage(Colors.msg(this.plugin, "genderCurrent", "%gender%", service.getGenderLabel(player.getUniqueId())));
            return true;
        }

        if (args[0].equalsIgnoreCase("help")) {
            Colors.sendList(player, this.plugin, "genderHelp");
            return true;
        }

        if (!args[0].equalsIgnoreCase("set") || args.length < 2) {
            Colors.sendList(player, this.plugin, "genderHelp");
            return true;
        }

        if (!this.plugin.getConfig().getBoolean("gender.allowSelfChange", true)) {
            player.sendMessage(Colors.msg(this.plugin, "genderChangeDisabled"));
            return true;
        }

        Gender gender = Gender.fromInput(args[1]);
        if (gender == null) {
            player.sendMessage(Colors.msg(this.plugin, "genderInvalid", "%values%", String.join(", ", VALUES)));
            return true;
        }

        service.setGender(player.getUniqueId(), gender);
        player.sendMessage(Colors.msg(this.plugin, "genderUpdated", "%gender%", service.getGenderLabel(gender)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("refontsocial.gender.use") && !sender.hasPermission("refontsocial.use")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return Arrays.asList("show", "set", "help").stream().filter(s -> s.startsWith(prefix)).collect(Collectors.toList());
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return VALUES.stream().filter(v -> v.startsWith(prefix)).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
