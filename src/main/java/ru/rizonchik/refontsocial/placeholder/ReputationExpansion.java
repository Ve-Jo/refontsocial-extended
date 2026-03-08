/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  me.clip.placeholderapi.expansion.PlaceholderExpansion
 *  org.bukkit.OfflinePlayer
 */
package ru.rizonchik.refontsocial.placeholder;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import ru.rizonchik.refontsocial.RefontSocial;
import ru.rizonchik.refontsocial.model.Gender;
import ru.rizonchik.refontsocial.model.MarriageInfo;
import ru.rizonchik.refontsocial.service.FriendsService;
import ru.rizonchik.refontsocial.service.GenderService;
import ru.rizonchik.refontsocial.storage.TopCategory;
import ru.rizonchik.refontsocial.storage.model.PlayerRep;
import ru.rizonchik.refontsocial.util.NumberUtil;

public final class ReputationExpansion
extends PlaceholderExpansion {
    private final RefontSocial plugin;

    public ReputationExpansion(RefontSocial plugin) {
        this.plugin = plugin;
    }

    public String getIdentifier() {
        return "refontsocial";
    }

    public String getAuthor() {
        return "rizonchik";
    }

    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    public boolean persist() {
        return true;
    }

    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) {
            return null;
        }
        String p = params.toLowerCase(Locale.ROOT).trim();
        String notFound = this.plugin.getConfig().getString("placeholders.notFound", "\u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d");
        if (player == null || player.getUniqueId() == null) {
            return notFound;
        }
        UUID uuid = player.getUniqueId();
        if (p.equals("gender") || p.equals("gender_emoji") || p.equals("gender_emoji_tab")) {
            GenderService genderService = this.plugin.getGenderService();
            if (genderService == null) {
                return notFound;
            }
            if (p.equals("gender_emoji_tab")) {
                return this.getGenderEmojiForTab(genderService, uuid);
            }
            if (p.equals("gender_emoji")) {
                return genderService.getGenderEmoji(uuid, true);
            }
            String label = genderService.getGenderLabel(uuid);
            String emoji = genderService.getGenderEmoji(uuid, true);
            if (emoji == null || emoji.trim().isEmpty()) {
                return label;
            }
            return emoji + " " + label;
        }
        if (p.equals("married") || p.equals("marriage_status") || p.equals("spouse") || p.equals("marriage_since") || p.equals("marriage_duration")) {
            if (this.plugin.getMarriageService() == null) {
                return notFound;
            }
            MarriageInfo info = this.plugin.getMarriageService().getMarriage(uuid);
            if (p.equals("married")) {
                return String.valueOf(info.isMarried());
            }
            if (p.equals("marriage_status")) {
                return info.isMarried() ? this.plugin.getConfig().getString("marriage.marriedText", "Married") : this.plugin.getConfig().getString("marriage.singleText", "Single");
            }
            if (p.equals("spouse")) {
                return info.isMarried() ? this.plugin.getMarriageService().getSpouseName(uuid) : this.plugin.getConfig().getString("marriage.notMarriedSpouseText", "-");
            }
            if (p.equals("marriage_duration")) {
                return info.isMarried() ? this.formatMarriageDuration(info.getSinceMillis()) : this.plugin.getConfig().getString("marriage.notMarriedSinceText", "-");
            }
            return info.isMarried() ? this.plugin.getMarriageService().getMarriageSinceFormatted(uuid) : this.plugin.getConfig().getString("marriage.notMarriedSinceText", "-");
        }
        if (p.equals("friends_count") || p.equals("friends_online")) {
            FriendsService friendsService = this.plugin.getFriendsService();
            if (friendsService == null || !friendsService.isEnabled()) {
                return "0";
            }
            if (p.equals("friends_online")) {
                return String.valueOf(friendsService.getOnlineFriendsCount(uuid));
            }
            return String.valueOf(friendsService.getFriendsCount(uuid));
        }
        if (p.equals("country")) {
            if (this.plugin.getCountryService() == null) {
                return notFound;
            }
            return this.plugin.getCountryService().getCountry(uuid);
        }
        if (p.equals("age")) {
            if (this.plugin.getAgeService() == null) {
                return notFound;
            }
            return String.valueOf(this.plugin.getAgeService().getAge(uuid));
        }
        if (p.equals("birthday")) {
            if (this.plugin.getAgeService() == null) {
                return notFound;
            }
            return this.plugin.getAgeService().getBirthday(uuid);
        }
        if (p.equals("birthday_emoji") || p.equals("birthday_tab")) {
            if (this.plugin.getAgeService() == null) {
                return "";
            }
            return this.plugin.getAgeService().getFormattedBirthdayForTab(uuid);
        }
        if (p.equals("score") || p.equals("likes") || p.equals("dislikes") || p.equals("votes") || p.equals("rank")) {
            PlayerRep rep = this.plugin.getReputationService().getOrCreate(uuid, player.getName() != null ? player.getName() : "\u0418\u0433\u0440\u043e\u043a");
            if (p.equals("score")) {
                return NumberUtil.formatScore(this.plugin, rep.getScore());
            }
            if (p.equals("likes")) {
                return String.valueOf(rep.getLikes());
            }
            if (p.equals("dislikes")) {
                return String.valueOf(rep.getDislikes());
            }
            if (p.equals("votes")) {
                return String.valueOf(rep.getVotes());
            }
            int rank = this.plugin.getReputationService().getRankCached(uuid);
            return rank > 0 ? String.valueOf(rank) : notFound;
        }
        TopQuery q = this.parseTopQuery(p);
        if (q == null) {
            return null;
        }
        int maxN = this.plugin.getConfig().getInt("placeholders.topMax", 200);
        if (maxN < 1) {
            maxN = 1;
        }
        if (q.place < 1 || q.place > maxN) {
            return notFound;
        }
        List<PlayerRep> top = this.plugin.getReputationService().getTopCached(TopCategory.SCORE, maxN, 0);
        if (top == null || top.size() < q.place) {
            return notFound;
        }
        PlayerRep rep = top.get(q.place - 1);
        String name = rep.getName();
        if (name == null || name.trim().isEmpty()) {
            name = this.plugin.getReputationService().getNameCached(rep.getUuid());
        }
        if (name == null || name.trim().isEmpty()) {
            name = notFound;
        }
        switch (q.field) {
            case NAME: {
                return name;
            }
            case SCORE: {
                return NumberUtil.formatScore(this.plugin, rep.getScore());
            }
            case LIKES: {
                return String.valueOf(rep.getLikes());
            }
            case DISLIKES: {
                return String.valueOf(rep.getDislikes());
            }
            case VOTES: {
                return String.valueOf(rep.getVotes());
            }
        }
        return null;
    }

    private String getGenderEmojiForTab(GenderService genderService, UUID uuid) {
        Gender stored = genderService.getStoredGender(uuid);
        if (stored == null || stored == Gender.UNDISCLOSED) {
            return "";
        }
        String emoji = genderService.getGenderEmoji(uuid, true);
        if (emoji == null || emoji.trim().isEmpty()) {
            return "";
        }
        String defaultColor = this.defaultGenderColor(stored);
        String color = this.plugin.getConfig().getString("gender.tabEmojiColors." + stored.getKey(), defaultColor);
        return color + emoji + "&r ";
    }

    private String defaultGenderColor(Gender gender) {
        if (gender == Gender.MALE) {
            return "&b";
        }
        if (gender == Gender.FEMALE) {
            return "&d";
        }
        if (gender == Gender.NONBINARY) {
            return "&5";
        }
        return "&7";
    }

    private String formatMarriageDuration(long sinceMillis) {
        long diffMs = Math.max(0L, System.currentTimeMillis() - sinceMillis);
        long totalMinutes = diffMs / 60000L;
        long days = totalMinutes / 1440L;
        long hours = (totalMinutes % 1440L) / 60L;
        long minutes = totalMinutes % 60L;
        if (days > 0L) {
            return days + "д " + hours + "ч " + minutes + "м";
        }
        if (hours > 0L) {
            return hours + "ч " + minutes + "м";
        }
        return Math.max(1L, minutes) + "м";
    }

    private TopQuery parseTopQuery(String p) {
        Field field;
        int n;
        int idx = p.lastIndexOf(95);
        if (idx <= 0 || idx >= p.length() - 1) {
            return null;
        }
        String left = p.substring(0, idx);
        String right = p.substring(idx + 1);
        try {
            n = Integer.parseInt(right);
        }
        catch (Exception e) {
            return null;
        }
        if (left.equals("nick") || left.equals("name")) {
            field = Field.NAME;
        } else if (left.equals("score")) {
            field = Field.SCORE;
        } else if (left.equals("like") || left.equals("likes")) {
            field = Field.LIKES;
        } else if (left.equals("dislike") || left.equals("dislikes")) {
            field = Field.DISLIKES;
        } else if (left.equals("votes")) {
            field = Field.VOTES;
        } else {
            return null;
        }
        return new TopQuery(field, n);
    }

    private static enum Field {
        NAME,
        SCORE,
        LIKES,
        DISLIKES,
        VOTES;

    }

    private static final class TopQuery {
        private final Field field;
        private final int place;

        private TopQuery(Field field, int place) {
            this.field = field;
            this.place = place;
        }
    }
}

