package ru.rizonchik.refontsocial.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.service.AgeService;
import ru.rizonchik.refontsocial.util.Colors;

public final class AgeCommand implements CommandExecutor {
    private final RefontSocial plugin;

    public AgeCommand(RefontSocial plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Colors.msg(this.plugin, "playerOnly"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("refontsocial.age.use") && !player.hasPermission("refontsocial.use")) {
            player.sendMessage(Colors.msg(this.plugin, "noPermission"));
            return true;
        }
        AgeService ageService = this.plugin.getAgeService();
        if (ageService == null) {
            player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
            return true;
        }
        if (args.length == 0) {
            String birthday = ageService.getBirthday(player.getUniqueId());
            String age = ageService.getAgeDisplay(player.getUniqueId());
            if (birthday.isEmpty()) {
                player.sendMessage(Colors.msg(this.plugin, "birthdayCurrent", "%birthday%", age, "%age%", age));
            } else {
                player.sendMessage(Colors.msg(this.plugin, "birthdayCurrent", "%birthday%", birthday, "%age%", age));
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("reset")) {
            ageService.clearBirthday(player.getUniqueId());
            player.sendMessage(Colors.msg(this.plugin, "birthdayCleared"));
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage(Colors.msg(this.plugin, "birthdayHelp"));
            return true;
        }
        
        // Check if birthday can be changed (once per month)
        if (!ageService.canSetBirthday(player.getUniqueId())) {
            player.sendMessage(Colors.msg(this.plugin, "birthdayChangeCooldown"));
            return true;
        }
        
        // Validate date format (dd.MM.yyyy)
        String birthday = args[0];
        if (!ageService.isValidBirthday(birthday)) {
            int min = this.plugin.getConfig().getInt("birthday.min", 13);
            int max = this.plugin.getConfig().getInt("birthday.max", 100);
            player.sendMessage(Colors.msg(this.plugin, "birthdayInvalid", "%min%", String.valueOf(min), "%max%", String.valueOf(max)));
            return true;
        }
        
        boolean success = ageService.setBirthday(player.getUniqueId(), birthday);
        if (success) {
            String age = ageService.getAgeDisplay(player.getUniqueId());
            player.sendMessage(Colors.msg(this.plugin, "birthdaySet", "%birthday%", birthday, "%age%", age));
        } else {
            player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
        }
        return true;
    }
}
