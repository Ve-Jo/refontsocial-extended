/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.ItemStack
 */
package ru.rizonchik.refontsocial.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public abstract class AbstractGui {
    protected Inventory inventory;

    public Inventory getInventory() {
        return this.inventory;
    }

    public abstract void open(Player var1);

    public abstract void onClick(Player var1, int var2, ItemStack var3);

    public void onClose(Player player) {
    }
}

