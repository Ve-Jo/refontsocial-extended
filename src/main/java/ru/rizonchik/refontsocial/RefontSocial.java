/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Player
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package ru.rizonchik.refontsocial;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.rizonchik.refontsocial.command.FriendsCommand;
import ru.rizonchik.refontsocial.command.GenderCommand;
import ru.rizonchik.refontsocial.command.MarriageCommand;
import ru.rizonchik.refontsocial.command.ReputationCommand;
import ru.rizonchik.refontsocial.command.SitCommand;
import ru.rizonchik.refontsocial.gui.GuiService;
import ru.rizonchik.refontsocial.listener.FriendsListener;
import ru.rizonchik.refontsocial.listener.InteractionTracker;
import ru.rizonchik.refontsocial.listener.SeenListener;
import ru.rizonchik.refontsocial.placeholder.ReputationExpansion;
import ru.rizonchik.refontsocial.service.FriendsService;
import ru.rizonchik.refontsocial.service.GenderService;
import ru.rizonchik.refontsocial.service.MarriageService;
import ru.rizonchik.refontsocial.service.ReputationService;
import ru.rizonchik.refontsocial.service.SitManager;
import ru.rizonchik.refontsocial.storage.Storage;
import ru.rizonchik.refontsocial.storage.StorageType;
import ru.rizonchik.refontsocial.storage.sql.MysqlStorage;
import ru.rizonchik.refontsocial.storage.sql.SqliteStorage;
import ru.rizonchik.refontsocial.storage.yaml.YamlStorage;
import ru.rizonchik.refontsocial.util.LibraryManager;
import ru.rizonchik.refontsocial.util.SaltStore;
import ru.rizonchik.refontsocial.util.SecurityUtil;
import ru.rizonchik.refontsocial.util.VisualIndicatorsHook;
import ru.rizonchik.refontsocial.util.YamlUtil;

public final class RefontSocial
extends JavaPlugin {
    private Storage storage;
    private ReputationService reputationService;
    private GuiService guiService;
    private InteractionTracker interactionTracker;
    private SeenListener seenListener;
    private FriendsListener friendsListener;
    private MarriageService marriageService;
    private GenderService genderService;
    private FriendsService friendsService;
    private SitManager sitManager;
    private VisualIndicatorsHook visualIndicatorsHook;

    public void onEnable() {
        this.saveDefaultConfig();
        YamlUtil.saveResourceIfNotExists(this, "messages.yml");
        YamlUtil.saveResourceIfNotExists(this, "gui.yml");
        YamlUtil.saveResourceIfNotExists(this, "tags.yml");
        this.visualIndicatorsHook = new VisualIndicatorsHook(this);
        this.reloadPlugin();
        ReputationCommand cmd = new ReputationCommand(this);
        if (this.getCommand("reputation") != null) {
            this.getCommand("reputation").setExecutor((CommandExecutor)cmd);
            this.getCommand("reputation").setTabCompleter((TabCompleter)cmd);
        }
        if (this.getCommand("repreload") != null) {
            this.getCommand("repreload").setExecutor((CommandExecutor)cmd);
        }
        MarriageCommand marriageCommand = new MarriageCommand(this);
        if (this.getCommand("marry") != null) {
            this.getCommand("marry").setExecutor((CommandExecutor)marriageCommand);
            this.getCommand("marry").setTabCompleter((TabCompleter)marriageCommand);
        }
        GenderCommand genderCommand = new GenderCommand(this);
        if (this.getCommand("gender") != null) {
            this.getCommand("gender").setExecutor((CommandExecutor)genderCommand);
            this.getCommand("gender").setTabCompleter((TabCompleter)genderCommand);
        }
        SitCommand sitCommand = new SitCommand(this);
        if (this.getCommand("sit") != null) {
            this.getCommand("sit").setExecutor((CommandExecutor)sitCommand);
        }
        FriendsCommand friendsCommand = new FriendsCommand(this);
        if (this.getCommand("friends") != null) {
            this.getCommand("friends").setExecutor((CommandExecutor)friendsCommand);
            this.getCommand("friends").setTabCompleter((TabCompleter)friendsCommand);
        }
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ReputationExpansion(this).register();
            this.getLogger().info("Hooked into PlaceholderAPI.");
        } else {
            this.getLogger().info("PlaceholderAPI not found (softdepend). Placeholders disabled.");
        }
        this.getLogger().info("Enabled.");
    }

    public void onDisable() {
        if (this.seenListener != null) {
            HandlerList.unregisterAll((Listener)this.seenListener);
            this.seenListener = null;
        }
        if (this.friendsListener != null) {
            HandlerList.unregisterAll((Listener)this.friendsListener);
            this.friendsListener = null;
        }
        if (this.interactionTracker != null) {
            this.interactionTracker.shutdown();
            HandlerList.unregisterAll((Listener)this.interactionTracker);
            this.interactionTracker = null;
        }
        if (this.guiService != null) {
            this.guiService.shutdown();
            HandlerList.unregisterAll((Listener)this.guiService);
            this.guiService = null;
        }
        if (this.reputationService != null) {
            this.reputationService.shutdown();
            this.reputationService = null;
        }
        if (this.marriageService != null) {
            this.marriageService.shutdown();
            this.marriageService = null;
        }
        if (this.genderService != null) {
            this.genderService.shutdown();
            this.genderService = null;
        }
        if (this.friendsService != null) {
            this.friendsService.shutdown();
            this.friendsService = null;
        }
        if (this.sitManager != null) {
            HandlerList.unregisterAll((Listener)this.sitManager);
            this.sitManager.shutdown();
            this.sitManager = null;
        }
        if (this.storage != null) {
            this.storage.close();
            this.storage = null;
        }
        this.getLogger().info("Disabled.");
    }

    public void reloadPlugin() {
        StorageType storageType;
        String path;
        String ver;
        String aid;
        String gid;
        boolean enabled;
        this.reloadConfig();
        String storageTypeStr = this.getConfig().getString("storage.type", "YAML").toUpperCase(Locale.ROOT);
        LibraryManager libs = new LibraryManager(this);
        if (storageTypeStr.equals("SQLITE") && (enabled = this.getConfig().getBoolean("libraries.sqlite.enabled", true))) {
            gid = this.getConfig().getString("libraries.sqlite.groupId", "org.xerial");
            aid = this.getConfig().getString("libraries.sqlite.artifactId", "sqlite-jdbc");
            ver = this.getConfig().getString("libraries.sqlite.version", "3.46.0.0");
            path = gid.replace('.', '/') + "/" + aid + "/" + ver + "/" + aid + "-" + ver + ".jar";
            libs.ensureDriverPresent("org.sqlite.JDBC", path, aid + "-" + ver + ".jar");
        }
        if (storageTypeStr.equals("MYSQL") && (enabled = this.getConfig().getBoolean("libraries.mysql.enabled", true))) {
            gid = this.getConfig().getString("libraries.mysql.groupId", "com.mysql");
            aid = this.getConfig().getString("libraries.mysql.artifactId", "mysql-connector-j");
            ver = this.getConfig().getString("libraries.mysql.version", "8.0.33");
            path = gid.replace('.', '/') + "/" + aid + "/" + ver + "/" + aid + "-" + ver + ".jar";
            libs.ensureDriverPresent("com.mysql.cj.jdbc.Driver", path, aid + "-" + ver + ".jar");
        }
        YamlUtil.reloadMessages(this);
        YamlUtil.reloadGui(this);
        YamlUtil.reloadTags(this);
        if (this.seenListener != null) {
            HandlerList.unregisterAll((Listener)this.seenListener);
            this.seenListener = null;
        }
        if (this.interactionTracker != null) {
            this.interactionTracker.shutdown();
            HandlerList.unregisterAll((Listener)this.interactionTracker);
            this.interactionTracker = null;
        }
        if (this.guiService != null) {
            this.guiService.shutdown();
            HandlerList.unregisterAll((Listener)this.guiService);
            this.guiService = null;
        }
        if (this.reputationService != null) {
            this.reputationService.shutdown();
            this.reputationService = null;
        }
        if (this.marriageService != null) {
            this.marriageService.shutdown();
            this.marriageService = null;
        }
        if (this.genderService != null) {
            this.genderService.shutdown();
            this.genderService = null;
        }
        if (this.sitManager != null) {
            HandlerList.unregisterAll((Listener)this.sitManager);
            this.sitManager.shutdown();
            this.sitManager = null;
        }
        if (this.storage != null) {
            this.storage.close();
            this.storage = null;
        }
        try {
            storageType = StorageType.valueOf(storageTypeStr);
        }
        catch (Exception e) {
            storageType = StorageType.YAML;
        }
        this.storage = storageType == StorageType.MYSQL ? new MysqlStorage(this) : (storageType == StorageType.YAML ? new YamlStorage(this) : new SqliteStorage(this));
        this.storage.init();
        this.marriageService = new MarriageService(this);
        this.marriageService.init();
        this.genderService = new GenderService(this);
        this.genderService.init();
        this.friendsService = new FriendsService(this);
        this.friendsService.init();
        this.sitManager = new SitManager(this);
        this.reputationService = new ReputationService(this, this.storage);
        this.guiService = new GuiService(this, this.reputationService);
        Bukkit.getScheduler().runTask((Plugin)this, () -> {
            List<Player> online = new ArrayList<Player>(Bukkit.getOnlinePlayers());
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this, () -> {
                for (Player p : online) {
                    try {
                        this.storage.markSeen(p.getUniqueId(), p.getName(), null);
                    }
                    catch (Throwable throwable) {}
                }
            });
        });
        this.getServer().getPluginManager().registerEvents((Listener)this.guiService, (Plugin)this);
        this.seenListener = new SeenListener(this);
        this.getServer().getPluginManager().registerEvents((Listener)this.seenListener, (Plugin)this);
        this.friendsListener = new FriendsListener(this, this.friendsService);
        this.getServer().getPluginManager().registerEvents((Listener)this.friendsListener, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.sitManager, (Plugin)this);
        Bukkit.getScheduler().runTask((Plugin)this, () -> {
            List<Player> online = new ArrayList<Player>(Bukkit.getOnlinePlayers());
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this, () -> {
                String salt = SaltStore.getOrCreate(this);
                for (Player p : online) {
                    String ip = null;
                    try {
                        if (p.getAddress() != null && p.getAddress().getAddress() != null) {
                            ip = p.getAddress().getAddress().getHostAddress();
                        }
                    }
                    catch (Throwable throwable) {
                        // empty catch block
                    }
                    String ipHash = ip == null ? null : SecurityUtil.sha256(ip + "|" + salt);
                    try {
                        this.storage.markSeen(p.getUniqueId(), p.getName(), ipHash);
                    }
                    catch (Throwable throwable) {}
                }
            });
        });
        boolean requireInteraction = this.getConfig().getBoolean("antiAbuse.requireInteraction.enabled", true);
        if (requireInteraction) {
            this.interactionTracker = new InteractionTracker(this);
            this.interactionTracker.start();
            this.reputationService.setInteractionTracker(this.interactionTracker);
            this.getServer().getPluginManager().registerEvents((Listener)this.interactionTracker, (Plugin)this);
        } else {
            this.reputationService.setInteractionTracker(null);
        }
    }

    public Storage getStorage() {
        return this.storage;
    }

    public ReputationService getReputationService() {
        return this.reputationService;
    }

    public GuiService getGuiService() {
        return this.guiService;
    }

    public MarriageService getMarriageService() {
        return this.marriageService;
    }

    public GenderService getGenderService() {
        return this.genderService;
    }

    public FriendsService getFriendsService() {
        return this.friendsService;
    }

    public VisualIndicatorsHook getVisualIndicatorsHook() {
        return this.visualIndicatorsHook;
    }

    public SitManager getSitManager() {
        return this.sitManager;
    }
}

