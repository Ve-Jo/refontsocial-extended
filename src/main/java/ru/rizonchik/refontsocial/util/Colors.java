/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.rizonchik.refontsocial.util;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import ru.rizonchik.refontsocial.util.YamlUtil;

public final class Colors {
    private static final Pattern HEX_AMPERSAND = Pattern.compile("(?i)&#([0-9A-F]{6})");
    private static final Pattern HEX_BRACKET = Pattern.compile("(?i)<#([0-9A-F]{6})>");

    private Colors() {
    }

    public static String prefix(JavaPlugin plugin) {
        YamlConfiguration msg = YamlUtil.messages(plugin);
        String p = msg.getString("prefix", "\u00a78[RS] \u00a7r");
        return Colors.color(p);
    }

    public static String msg(JavaPlugin plugin, String key, String ... replace) {
        YamlConfiguration msg = YamlUtil.messages(plugin);
        String s = msg.getString(key, "\u00a7cMissing message: " + key);
        s = s.replace("%prefix%", Colors.prefix(plugin));
        if (replace != null) {
            int i = 0;
            while (i + 1 < replace.length) {
                s = s.replace(replace[i], replace[i + 1]);
                i += 2;
            }
        }
        return Colors.color(s);
    }

    public static void sendList(CommandSender sender, JavaPlugin plugin, String key) {
        YamlConfiguration msg = YamlUtil.messages(plugin);
        List<String> list = msg.getStringList(key);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (String s : list) {
            sender.sendMessage(Colors.color(s.replace("%prefix%", Colors.prefix(plugin))));
        }
    }

    public static String color(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String withHex = Colors.applyHexColors(s);
        return ChatColor.translateAlternateColorCodes((char)'&', (String)withHex);
    }

    private static String applyHexColors(String input) {
        if (input.indexOf(35) < 0) {
            return input;
        }
        String out = Colors.replaceHex(input, HEX_BRACKET);
        return Colors.replaceHex(out, HEX_AMPERSAND);
    }

    private static String replaceHex(String input, Pattern pattern) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("&x");
            for (int i = 0; i < hex.length(); ++i) {
                replacement.append('&').append(hex.charAt(i));
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(replacement.toString()));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}

