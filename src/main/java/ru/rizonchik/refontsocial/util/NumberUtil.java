/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.rizonchik.refontsocial.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.bukkit.plugin.java.JavaPlugin;

public final class NumberUtil {
    private NumberUtil() {
    }

    public static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            return def;
        }
    }

    public static double defaultScore(JavaPlugin plugin) {
        double d = plugin.getConfig().getDouble("rating.defaultScore", 10.0);
        return d;
    }

    public static double computeScore(JavaPlugin plugin, int likes, int dislikes) {
        int d;
        double min = plugin.getConfig().getDouble("rating.scale.min", 0.0);
        double max = plugin.getConfig().getDouble("rating.scale.max", 10.0);
        double def = NumberUtil.defaultScore(plugin);
        int l = Math.max(0, likes);
        int votes = l + (d = Math.max(0, dislikes));
        if (votes <= 0) {
            return NumberUtil.clamp(def, min, max);
        }
        String algo = plugin.getConfig().getString("rating.algorithm", "BAYESIAN");
        if (algo == null) {
            algo = "BAYESIAN";
        }
        if ((algo = algo.toUpperCase(Locale.ROOT)).equals("SIMPLE_RATIO")) {
            double ratio = (double)l / (double)votes;
            double score = min + (max - min) * ratio;
            return NumberUtil.clamp(score, min, max);
        }
        int priorVotes = plugin.getConfig().getInt("rating.bayesian.priorVotes", 12);
        if (priorVotes < 0) {
            priorVotes = 0;
        }
        double defRatio = max - min <= 0.0 ? 0.5 : (NumberUtil.clamp(def, min, max) - min) / (max - min);
        double priorLikes = (double)priorVotes * defRatio;
        double ratio = ((double)l + priorLikes) / ((double)votes + (double)priorVotes);
        double score = min + (max - min) * ratio;
        return NumberUtil.clamp(score, min, max);
    }

    public static String formatScore(JavaPlugin plugin, double score) {
        String pattern = plugin.getConfig().getString("rating.format", "#0.0");
        DecimalFormat df = new DecimalFormat(pattern, new DecimalFormatSymbols(Locale.US));
        return df.format(score);
    }

    public static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public static long startOfTodayMillis() {
        long now = System.currentTimeMillis();
        long day = 86400000L;
        return now - now % day;
    }
}

