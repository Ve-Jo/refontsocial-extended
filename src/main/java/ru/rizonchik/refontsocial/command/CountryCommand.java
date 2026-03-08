package ru.rizonchik.refontsocial.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.service.CountryService;
import ru.rizonchik.refontsocial.util.Colors;

public final class CountryCommand implements CommandExecutor {
    private final RefontSocial plugin;

    public CountryCommand(RefontSocial plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Colors.msg(this.plugin, "playerOnly"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("refontsocial.country.use") && !player.hasPermission("refontsocial.use")) {
            player.sendMessage(Colors.msg(this.plugin, "noPermission"));
            return true;
        }
        CountryService countryService = this.plugin.getCountryService();
        if (countryService == null) {
            player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
            return true;
        }
        if (args.length == 0) {
            String country = countryService.getCountry(player.getUniqueId());
            player.sendMessage(Colors.msg(this.plugin, "countryCurrent", "%country%", countryService.getCountryDisplay(country)));
            return true;
        }
        if (args[0].equalsIgnoreCase("clear") || args[0].equalsIgnoreCase("reset")) {
            countryService.setCountry(player.getUniqueId(), "");
            player.sendMessage(Colors.msg(this.plugin, "countryCleared"));
            return true;
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (args.length < 2) {
                player.sendMessage(Colors.msg(this.plugin, "countryUsage"));
                return true;
            }
            String countryInput = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            if (!countryService.isValidCountry(countryInput)) {
                String closest = countryService.findClosestCountry(countryInput);
                if (closest != null) {
                    player.sendMessage(Colors.msg(this.plugin, "countryInvalidSuggest", "%input%", countryInput, "%suggestion%", closest));
                } else {
                    player.sendMessage(Colors.msg(this.plugin, "countryInvalid"));
                }
                return true;
            }
            // Get the exact country name from the list (with flag emoji)
            String exactCountry = countryService.findClosestCountry(countryInput);
            countryService.setCountry(player.getUniqueId(), exactCountry);
            player.sendMessage(Colors.msg(this.plugin, "countrySet", "%country%", exactCountry));
            return true;
        }
        if (args[0].equalsIgnoreCase("list")) {
            player.sendMessage(Colors.msg(this.plugin, "countryListHeader"));
            java.util.List<String> countries = countryService.loadAllowedCountries();
            int page = 1;
            if (args.length > 1) {
                try {
                    page = Integer.parseInt(args[1]);
                } catch (NumberFormatException ignored) {
                }
            }
            int perPage = 20;
            int totalPages = (int) Math.ceil((double) countries.size() / perPage);
            if (page < 1) page = 1;
            if (page > totalPages) page = totalPages;
            
            int start = (page - 1) * perPage;
            int end = Math.min(start + perPage, countries.size());
            
            for (int i = start; i < end; i++) {
                player.sendMessage("§7• " + countries.get(i));
            }
            player.sendMessage(Colors.msg(this.plugin, "countryListFooter", "%page%", String.valueOf(page), "%total%", String.valueOf(totalPages)));
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) {
            player.sendMessage(Colors.msg(this.plugin, "countryHelp"));
            return true;
        }
        // Try to set country - supports partial matching
        String countryInput = String.join(" ", args);
        if (!countryService.isValidCountry(countryInput)) {
            String closest = countryService.findClosestCountry(countryInput);
            if (closest != null) {
                player.sendMessage(Colors.msg(this.plugin, "countryInvalidSuggest", "%input%", countryInput, "%suggestion%", closest));
            } else {
                player.sendMessage(Colors.msg(this.plugin, "countryInvalid"));
            }
            return true;
        }
        
        // Get the exact country name from the list (with flag emoji)
        String exactCountry = countryService.findClosestCountry(countryInput);
        countryService.setCountry(player.getUniqueId(), exactCountry);
        player.sendMessage(Colors.msg(this.plugin, "countrySet", "%country%", exactCountry));
        return true;
    }
}
