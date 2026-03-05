package ru.rizonchik.refontsocial.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.model.MarriageInfo;
import ru.rizonchik.refontsocial.service.GenderService;
import ru.rizonchik.refontsocial.service.MarriageService;
import ru.rizonchik.refontsocial.util.Colors;

public final class MarriageCommand implements CommandExecutor, TabCompleter {
    private final RefontSocial plugin;

    public MarriageCommand(RefontSocial plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Colors.msg(this.plugin, "playerOnly"));
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("refontsocial.marry.use") && !player.hasPermission("refontsocial.use")) {
            player.sendMessage(Colors.msg(this.plugin, "noPermission"));
            return true;
        }
        MarriageService service = this.plugin.getMarriageService();
        if (service == null) {
            player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
            return true;
        }

        if (args.length == 0) {
            MarriageInfo info = service.getMarriage(player.getUniqueId());
            GenderService genderService = this.plugin.getGenderService();
            String genderLabel = genderService != null ? genderService.getGenderLabel(player.getUniqueId()) : this.plugin.getConfig().getString("gender.labels.undisclosed", "Undisclosed");
            if (!info.isMarried()) {
                player.sendMessage(Colors.msg(this.plugin, "marriageStatusSingle", "%gender%", genderLabel));
                return true;
            }
            player.sendMessage(Colors.msg(this.plugin,
                    "marriageStatusMarried",
                    "%spouse%", service.getSpouseName(player.getUniqueId()),
                    "%since%", service.getMarriageSinceFormatted(player.getUniqueId()),
                    "%gender%", genderLabel));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("help")) {
            Colors.sendList(player, this.plugin, "marryHelp");
            return true;
        }

        if (sub.equals("propose") || sub.equals("request")) {
            if (args.length < 2) {
                Colors.sendList(player, this.plugin, "marryHelp");
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

            MarriageService.ProposalResult result = service.sendProposal(player.getUniqueId(), target.getUniqueId());
            switch (result) {
                case SENT:
                    player.sendMessage(Colors.msg(this.plugin, "marriageProposalSent", "%target%", target.getName()));
                    target.sendMessage(Colors.msg(this.plugin, "marriageProposalReceived", "%player%", player.getName()));
                    return true;
                case SELF:
                    player.sendMessage(Colors.msg(this.plugin, "marriageSelfDenied"));
                    return true;
                case PROPOSER_ALREADY_MARRIED:
                    player.sendMessage(Colors.msg(this.plugin, "marriageAlreadyMarriedSelf"));
                    return true;
                case TARGET_ALREADY_MARRIED:
                    player.sendMessage(Colors.msg(this.plugin, "marriageTargetAlreadyMarried", "%target%", target.getName()));
                    return true;
                case ALREADY_SENT:
                    player.sendMessage(Colors.msg(this.plugin, "marriageProposalAlreadySent", "%target%", target.getName()));
                    return true;
                case TARGET_HAS_PENDING:
                    player.sendMessage(Colors.msg(this.plugin, "marriageTargetHasPending", "%target%", target.getName()));
                    return true;
                default:
                    player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
                    return true;
            }
        }

        if (sub.equals("accept")) {
            if (args.length < 2) {
                Colors.sendList(player, this.plugin, "marryHelp");
                return true;
            }
            Player proposer = Bukkit.getPlayerExact(args[1]);
            if (proposer == null) {
                proposer = Bukkit.getPlayer(args[1]);
            }
            if (proposer == null) {
                player.sendMessage(Colors.msg(this.plugin, "targetMustBeOnline"));
                return true;
            }
            MarriageService.AcceptResult result = service.accept(player.getUniqueId(), proposer.getUniqueId());
            switch (result) {
                case ACCEPTED:
                    player.sendMessage(Colors.msg(this.plugin, "marriageAcceptedSelf", "%target%", proposer.getName()));
                    proposer.sendMessage(Colors.msg(this.plugin, "marriageAcceptedOther", "%target%", player.getName()));
                    return true;
                case NO_PENDING:
                    player.sendMessage(Colors.msg(this.plugin, "marriageNoPending"));
                    return true;
                case NO_PENDING_FROM_PLAYER:
                    player.sendMessage(Colors.msg(this.plugin, "marriageNoPendingFrom", "%target%", proposer.getName()));
                    return true;
                case ALREADY_MARRIED:
                    player.sendMessage(Colors.msg(this.plugin, "marriageAlreadyMarriedSelf"));
                    return true;
                default:
                    player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
                    return true;
            }
        }

        if (sub.equals("deny") || sub.equals("decline")) {
            if (args.length < 2) {
                Colors.sendList(player, this.plugin, "marryHelp");
                return true;
            }
            Player proposer = Bukkit.getPlayerExact(args[1]);
            if (proposer == null) {
                proposer = Bukkit.getPlayer(args[1]);
            }
            if (proposer == null) {
                player.sendMessage(Colors.msg(this.plugin, "targetMustBeOnline"));
                return true;
            }
            MarriageService.DenyResult result = service.deny(player.getUniqueId(), proposer.getUniqueId());
            switch (result) {
                case DENIED:
                    player.sendMessage(Colors.msg(this.plugin, "marriageDeniedSelf", "%target%", proposer.getName()));
                    proposer.sendMessage(Colors.msg(this.plugin, "marriageDeniedOther", "%target%", player.getName()));
                    return true;
                case NO_PENDING:
                    player.sendMessage(Colors.msg(this.plugin, "marriageNoPending"));
                    return true;
                case NO_PENDING_FROM_PLAYER:
                    player.sendMessage(Colors.msg(this.plugin, "marriageNoPendingFrom", "%target%", proposer.getName()));
                    return true;
                default:
                    player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
                    return true;
            }
        }

        if (sub.equals("divorce")) {
            String spouseName = service.getSpouseName(player.getUniqueId());
            boolean divorced = service.divorce(player.getUniqueId());
            if (!divorced) {
                player.sendMessage(Colors.msg(this.plugin, "marriageNotMarried"));
                return true;
            }
            player.sendMessage(Colors.msg(this.plugin, "marriageDivorced", "%target%", spouseName));
            return true;
        }

        if (sub.equals("hug") || sub.equals("kiss") || sub.equals("carry")) {
            if (!service.isMarried(player.getUniqueId())) {
                player.sendMessage(Colors.msg(this.plugin, "marriageNotMarried"));
                return true;
            }
            Player spouse = service.getOnlineSpouse(player.getUniqueId());
            if (spouse == null) {
                player.sendMessage(Colors.msg(this.plugin, "marriagePartnerOffline"));
                return true;
            }
            double actionDistance = service.getActionDistance();
            if (!service.isPartnerNearby(player, actionDistance)) {
                player.sendMessage(Colors.msg(this.plugin, "marriagePartnerTooFar", "%distance%", String.valueOf((int)Math.round(actionDistance))));
                return true;
            }

            if (sub.equals("hug")) {
                player.sendMessage(Colors.msg(this.plugin, "marriageHugSelf", "%target%", spouse.getName()));
                spouse.sendMessage(Colors.msg(this.plugin, "marriageHugOther", "%target%", player.getName()));
                player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0.0, 1.0, 0.0), 14, 0.35, 0.4, 0.35, 0.0);
                spouse.getWorld().spawnParticle(Particle.CLOUD, spouse.getLocation().add(0.0, 1.0, 0.0), 14, 0.35, 0.4, 0.35, 0.0);
                return true;
            }

            if (sub.equals("kiss")) {
                player.sendMessage(Colors.msg(this.plugin, "marriageKissSelf", "%target%", spouse.getName()));
                spouse.sendMessage(Colors.msg(this.plugin, "marriageKissOther", "%target%", player.getName()));
                player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0.0, 1.2, 0.0), 4, 0.25, 0.3, 0.25, 0.0);
                spouse.getWorld().spawnParticle(Particle.HEART, spouse.getLocation().add(0.0, 1.2, 0.0), 4, 0.25, 0.3, 0.25, 0.0);
                return true;
            }

            if (player.isInsideVehicle()) {
                if (player.getVehicle() != null && player.getVehicle().getUniqueId().equals(spouse.getUniqueId())) {
                    player.leaveVehicle();
                    player.sendMessage(Colors.msg(this.plugin, "marriageCarryStoppedSelf", "%target%", spouse.getName()));
                    spouse.sendMessage(Colors.msg(this.plugin, "marriageCarryStoppedOther", "%target%", player.getName()));
                    return true;
                }
                player.sendMessage(Colors.msg(this.plugin, "marriageCarryAlreadyRiding"));
                return true;
            }

            if (!spouse.addPassenger(player)) {
                player.sendMessage(Colors.msg(this.plugin, "featureUnavailable"));
                return true;
            }
            player.sendMessage(Colors.msg(this.plugin, "marriageCarryStartedSelf", "%target%", spouse.getName()));
            spouse.sendMessage(Colors.msg(this.plugin, "marriageCarryStartedOther", "%target%", player.getName()));
            return true;
        }

        Colors.sendList(player, this.plugin, "marryHelp");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("refontsocial.marry.use") && !sender.hasPermission("refontsocial.use")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> base = Arrays.asList("help", "propose", "accept", "deny", "divorce", "hug", "kiss", "carry");
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return base.stream().filter(v -> v.startsWith(prefix)).collect(Collectors.toList());
        }

        if (args.length == 2 && Arrays.asList("propose", "accept", "deny", "decline", "request").contains(args[0].toLowerCase(Locale.ROOT))) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .limit(30)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        return Collections.emptyList();
    }
}
