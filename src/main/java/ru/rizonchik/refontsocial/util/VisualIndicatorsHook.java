package ru.rizonchik.refontsocial.util;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.rizonchik.refontsocial.RefontSocial;

public final class VisualIndicatorsHook {
    private final RefontSocial plugin;
    private Method serviceMethod;
    private Method spawnMethod;
    private Object serviceInstance;

    public VisualIndicatorsHook(RefontSocial plugin) {
        this.plugin = plugin;
    }

    public void spawnSocialIndicator(Player actor, Player target, String actionKey) {
        if (actor == null || target == null) {
            return;
        }
        if (!this.ensureHooked()) {
            return;
        }
        try {
            this.spawnMethod.invoke(this.serviceInstance, actor, target, actionKey);
        } catch (Throwable ignored) {
        }
    }

    private boolean ensureHooked() {
        Plugin visualIndicators = Bukkit.getPluginManager().getPlugin("VisualIndicators");
        if (visualIndicators == null || !visualIndicators.isEnabled()) {
            return false;
        }
        if (this.spawnMethod != null && this.serviceInstance != null) {
            return true;
        }
        try {
            this.serviceMethod = visualIndicators.getClass().getMethod("indicatorService");
            Object service = this.serviceMethod.invoke(visualIndicators);
            if (service == null) {
                return false;
            }
            this.spawnMethod = service.getClass().getMethod("spawnSocialIndicator", Player.class, Player.class, String.class);
            this.serviceInstance = service;
            return true;
        } catch (Throwable throwable) {
            this.serviceMethod = null;
            this.spawnMethod = null;
            this.serviceInstance = null;
            return false;
        }
    }
}
