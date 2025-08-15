package sidly.discord_bot.database;

import sidly.discord_bot.Utils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.commands.demotion_promotion.RequirementList;

import java.util.*;

public class Database {
    public Map<Utils.RankList, RequirementList> promotionRequirements;
    public Map<String, String> verifiedMembersByIgn;
    public Map<String, String> verifiedMembersByDiscordId;
    public Map<String, PlayerDataShortened> allPlayers;
    public Map<String, PlaytimeHistoryList> playtimeHistory;
    public Map<String, GuildDataActivity> trackedGuildActivity;
    public List<String> trackedGuilds;
    public GuildInfo yourGuildInfo;

    public Database() {
        this.promotionRequirements = new HashMap<>();
        this.verifiedMembersByDiscordId = new HashMap<>();
        this.verifiedMembersByIgn = new HashMap<>();
        this.allPlayers = new HashMap<>();
        this.playtimeHistory = new HashMap<>();
        this.trackedGuildActivity = new HashMap<>();
        this.trackedGuilds = new ArrayList<>();
    }

    public void removeVerification(String string) {
        // Try removing by IGN first
        if (verifiedMembersByIgn.containsKey(string)) {
            String discordId = verifiedMembersByIgn.remove(string);
            verifiedMembersByDiscordId.remove(discordId);
            return;
        }

        // Otherwise try removing by Discord ID
        if (verifiedMembersByDiscordId.containsKey(string)) {
            String ign = verifiedMembersByDiscordId.remove(string);
            verifiedMembersByIgn.remove(ign);
        }
    }

}
