/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Material
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.rizonchik.refontsocial.util;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import ru.rizonchik.refontsocial.util.Colors;
import ru.rizonchik.refontsocial.util.YamlUtil;

public final class ItemUtil {
    private ItemUtil() {
    }

    public static ItemStack fromGui(JavaPlugin plugin, String key, String ... replace) {
        ItemStack item;
        ItemMeta meta;
        String base;
        YamlConfiguration gui = YamlUtil.gui(plugin);
        String matName = gui.getString((base = "items." + key) + ".material", "PAPER");
        Material mat = Material.matchMaterial((String)matName);
        if (mat == null) {
            mat = Material.PAPER;
        }
        if ((meta = (item = new ItemStack(mat)).getItemMeta()) == null) {
            return item;
        }
        String name = gui.getString(base + ".name", " ");
        name = ItemUtil.apply(name, replace);
        meta.setDisplayName(Colors.color(name));
        List<String> loreRaw = gui.getStringList(base + ".lore");
        if (loreRaw != null && !loreRaw.isEmpty()) {
            ArrayList<String> lore = new ArrayList<String>();
            for (String s : loreRaw) {
                lore.add(Colors.color(ItemUtil.apply(s, replace)));
            }
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private static String apply(String s, String ... replace) {
        if (s == null) {
            return "";
        }
        if (replace == null) {
            return s;
        }
        int i = 0;
        while (i + 1 < replace.length) {
            s = s.replace(replace[i], replace[i + 1]);
            i += 2;
        }
        return s;
    }
}

