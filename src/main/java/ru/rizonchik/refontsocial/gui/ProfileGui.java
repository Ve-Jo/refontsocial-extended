/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.SkullMeta
 *  org.bukkit.plugin.Plugin
 */
package ru.rizonchik.refontsocial.gui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.gui.AbstractGui;
import ru.rizonchik.refontsocial.model.MarriageInfo;
import ru.rizonchik.refontsocial.service.AgeService;
import ru.rizonchik.refontsocial.service.CountryService;
import ru.rizonchik.refontsocial.service.GenderService;
import ru.rizonchik.refontsocial.service.MarriageService;
import ru.rizonchik.refontsocial.service.ReputationService;
import ru.rizonchik.refontsocial.storage.model.PlayerRep;
import ru.rizonchik.refontsocial.storage.model.VoteLogEntry;
import ru.rizonchik.refontsocial.util.ItemUtil;
import ru.rizonchik.refontsocial.util.NumberUtil;

public final class ProfileGui
extends AbstractGui {
    private final RefontSocial plugin;
    private final ReputationService service;
    private final UUID target;
    private final String targetName;

    public ProfileGui(RefontSocial plugin, ReputationService service, UUID target, String targetName) {
        this.plugin = plugin;
        this.service = service;
        this.target = target;
        this.targetName = targetName != null ? targetName : "\u0418\u0433\u0440\u043e\u043a";
    }

    @Override
    public void open(Player viewer) {
        String title = this.plugin.getConfig().getString("gui.profile.title", "\u041f\u0440\u043e\u0444\u0438\u043b\u044c");
        int size = this.plugin.getConfig().getInt("gui.profile.size", 54);
        if (size < 9) {
            size = 54;
        }
        if (size % 9 != 0) {
            size = 54;
        }
        this.inventory = Bukkit.createInventory(null, (int)size, (String)title);
        ItemStack filler = ItemUtil.fromGui(this.plugin, "filler", new String[0]);
        for (int i = 0; i < this.inventory.getSize(); ++i) {
            this.inventory.setItem(i, filler);
        }
        ItemStack loading = new ItemStack(Material.PAPER);
        ItemMeta lm = loading.getItemMeta();
        if (lm != null) {
            lm.setDisplayName("\u00a7f\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 \u043f\u0440\u043e\u0444\u0438\u043b\u044f...");
            loading.setItemMeta(lm);
        }
        this.inventory.setItem(22, loading);
        this.inventory.setItem(this.inventory.getSize() - 9, ItemUtil.fromGui(this.plugin, "back", new String[0]));
        viewer.openInventory(this.inventory);
        Player viewerFinal = viewer;
        boolean includeVoter = this.service.shouldShowVoterName(viewerFinal);
        Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
            PlayerRep rep = this.service.getOrCreate(this.target, this.targetName);
            int rank = this.service.getRankCached(this.target);
            String rankStr = rank > 0 ? String.valueOf(rank) : this.plugin.getConfig().getString("placeholders.notFound", "\u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d");
            int tagLimit = this.plugin.getConfig().getInt("profile.topTags.limit", 3);
            if (tagLimit < 1) {
                tagLimit = 3;
            }
            Map<String, Integer> topTags = this.plugin.getStorage().getTopTags(this.target, tagLimit);
            boolean historyEnabled = this.plugin.getConfig().getBoolean("profile.history.enabled", true);
            int limit = this.plugin.getConfig().getInt("profile.history.limit", 10);
            if (limit < 1) {
                limit = 10;
            }
            List<VoteLogEntry> history = historyEnabled ? this.plugin.getStorage().getRecentVotes(this.target, limit, includeVoter) : Collections.emptyList();
            MarriageService marriageService = this.plugin.getMarriageService();
            GenderService genderService = this.plugin.getGenderService();
            CountryService countryService = this.plugin.getCountryService();
            AgeService ageService = this.plugin.getAgeService();
            MarriageInfo marriageInfo = marriageService != null ? marriageService.getMarriage(this.target) : MarriageInfo.single();
            String genderLabel = genderService != null ? genderService.getGenderLabel(this.target) : this.plugin.getConfig().getString("gender.labels.undisclosed", "Undisclosed");
            String genderEmoji = genderService != null ? genderService.getGenderEmoji(this.target, true) : "";
            String country = countryService != null ? countryService.getCountry(this.target) : "";
            String countryDisplay = countryService != null ? countryService.getCountryDisplay(country) : "Not specified";
            String birthday = ageService != null ? ageService.getBirthday(this.target) : "";
            String birthdayDisplay = ageService != null ? ageService.getBirthdayDisplay(this.target) : "Not specified";
            String ageDisplay = ageService != null ? ageService.getAgeDisplay(this.target) : "Not specified";
            String marriedLabel = marriageInfo.isMarried() ? this.plugin.getConfig().getString("marriage.marriedText", "Married") : this.plugin.getConfig().getString("marriage.singleText", "Single");
            String spouseName = marriageInfo.isMarried() && marriageService != null ? marriageService.getSpouseName(this.target) : this.plugin.getConfig().getString("marriage.notMarriedSpouseText", "-");
            String marriageSince = marriageInfo.isMarried() && marriageService != null ? marriageService.getMarriageSinceFormatted(this.target) : this.plugin.getConfig().getString("marriage.notMarriedSinceText", "-");
            int limitFinal = limit;
            boolean includeVoterFinal = includeVoter;
            boolean historyEnabledFinal = historyEnabled;
            PlayerRep repFinal = rep;
            String rankStrFinal = rankStr;
            Map<String, Integer> topTagsFinal = topTags;
            List<VoteLogEntry> historyFinal = history;
            String genderLabelFinal = genderLabel;
            String genderEmojiFinal = genderEmoji;
            String countryFinal = country;
            String countryDisplayFinal = countryDisplay;
            String birthdayFinal = birthday;
            String birthdayDisplayFinal = birthdayDisplay;
            String ageDisplayFinal = ageDisplay;
            String marriedLabelFinal = marriedLabel;
            String spouseNameFinal = spouseName;
            String marriageSinceFinal = marriageSince;
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (!viewerFinal.isOnline()) {
                    return;
                }
                if (viewerFinal.getOpenInventory() == null) {
                    return;
                }
                if (viewerFinal.getOpenInventory().getTopInventory() == null) {
                    return;
                }
                if (!viewerFinal.getOpenInventory().getTopInventory().equals(this.inventory)) {
                    return;
                }
                for (int i = 0; i < 45; ++i) {
                    this.inventory.setItem(i, null);
                }
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta sm = (SkullMeta)head.getItemMeta();
                if (sm != null) {
                    sm.setDisplayName("\u00a7f" + this.targetName);
                    ArrayList<String> lore = new ArrayList<String>();
                    lore.add("\u00a77\u0420\u0435\u0439\u0442\u0438\u043d\u0433: \u00a7f" + NumberUtil.formatScore(this.plugin, repFinal.getScore()) + " \u00a77(\u043c\u0435\u0441\u0442\u043e: \u00a7f" + rankStrFinal + "\u00a77)");
                    lore.add("\u00a77\u041b\u0430\u0439\u043a\u0438: \u00a7a" + repFinal.getLikes() + " \u00a77/ \u0414\u0438\u0437\u043b\u0430\u0439\u043a\u0438: \u00a7c" + repFinal.getDislikes());
                    lore.add("\u00a77\u0413\u043e\u043b\u043e\u0441\u043e\u0432: \u00a7f" + repFinal.getVotes());
                    lore.add("");
                    lore.add("\u00a77\u0422\u0435\u0433\u0438:");
                    if (topTagsFinal.isEmpty()) {
                        lore.add("\u00a78\u2022 \u00a77\u043d\u0435\u0442");
                    } else {
                        for (Map.Entry e : topTagsFinal.entrySet()) {
                            String key = (String)e.getKey();
                            int cnt = (Integer)e.getValue();
                            String display = this.service.getReasonTagDisplay(key);
                            lore.add("\u00a78\u2022 \u00a7f" + display + " \u00a78x\u00a77" + cnt);
                        }
                    }
                    sm.setLore(lore);
                    try {
                        OfflinePlayer off = Bukkit.getOfflinePlayer((UUID)this.target);
                        sm.setOwningPlayer(off);
                    }
                    catch (Throwable off) {
                        // empty catch block
                    }
                    head.setItemMeta((ItemMeta)sm);
                }
                this.inventory.setItem(13, head);
                int countrySlot = this.plugin.getConfig().getInt("profile.country.slot", 20);
                if (countrySlot >= 0 && countrySlot < this.inventory.getSize() - 9 && countryService != null) {
                    this.inventory.setItem(countrySlot, ItemUtil.fromGui(this.plugin, "profile_country", "%country%", countryDisplayFinal));
                }
                int birthdaySlot = this.plugin.getConfig().getInt("profile.birthday.slot", 21);
                if (birthdaySlot >= 0 && birthdaySlot < this.inventory.getSize() - 9 && ageService != null) {
                    this.inventory.setItem(birthdaySlot, ItemUtil.fromGui(this.plugin, "profile_birthday", "%birthday%", birthdayDisplayFinal, "%age%", ageDisplayFinal));
                }
                int genderSlot = this.plugin.getConfig().getInt("profile.gender.slot", 23);
                if (genderSlot >= 0 && genderSlot < this.inventory.getSize() - 9 && genderService != null) {
                    this.inventory.setItem(genderSlot, ItemUtil.fromGui(this.plugin, "profile_gender", "%gender%", genderLabelFinal, "%gender_emoji%", genderEmojiFinal));
                }
                int marriageSlot = this.plugin.getConfig().getInt("profile.marriage.slot", 24);
                if (marriageSlot >= 0 && marriageSlot < this.inventory.getSize() - 9 && marriageService != null) {
                    this.inventory.setItem(marriageSlot, ItemUtil.fromGui(this.plugin, "profile_marriage", "%married%", marriedLabelFinal, "%spouse%", spouseNameFinal, "%since%", marriageSinceFinal));
                }
                if (historyEnabledFinal) {
                    ItemStack book = new ItemStack(Material.BOOK);
                    ItemMeta bm = book.getItemMeta();
                    if (bm != null) {
                        bm.setDisplayName("\u00a7f\u0418\u0441\u0442\u043e\u0440\u0438\u044f \u043e\u0446\u0435\u043d\u043e\u043a");
                        ArrayList<String> lore = new ArrayList<String>();
                        lore.add("\u00a77\u041f\u043e\u0441\u043b\u0435\u0434\u043d\u0438\u0435 " + limitFinal + " \u0441\u043e\u0431\u044b\u0442\u0438\u0439:");
                        lore.add("");
                        SimpleDateFormat df = new SimpleDateFormat("dd.MM HH:mm");
                        if (historyFinal.isEmpty()) {
                            lore.add("\u00a78\u2022 \u00a77\u043f\u0443\u0441\u0442\u043e");
                        } else {
                            for (VoteLogEntry e : historyFinal) {
                                String when = df.format(new Date(e.getTimeMillis()));
                                String sign = e.getValue() == 1 ? "\u00a7a+\u00a77" : "\u00a7c-\u00a77";
                                String reason = e.getReason();
                                reason = reason != null && !reason.trim().isEmpty() ? this.service.getReasonTagDisplay(reason) : "\u0431\u0435\u0437 \u043f\u0440\u0438\u0447\u0438\u043d\u044b";
                                if (includeVoterFinal && e.getVoterName() != null && !e.getVoterName().trim().isEmpty()) {
                                    lore.add("\u00a78\u2022 \u00a77" + when + " " + sign + " \u00a7f" + e.getVoterName() + " \u00a78\u2014 \u00a7f" + reason);
                                    continue;
                                }
                                lore.add("\u00a78\u2022 \u00a77" + when + " " + sign + " \u00a78\u2014 \u00a7f" + reason);
                            }
                        }
                        bm.setLore(lore);
                        book.setItemMeta(bm);
                    }
                    this.inventory.setItem(31, book);
                }
            });
        });
    }

    @Override
    public void onClick(Player player, int rawSlot, ItemStack clicked) {
        if (rawSlot == this.inventory.getSize() - 9) {
            player.closeInventory();
        }
    }
}

