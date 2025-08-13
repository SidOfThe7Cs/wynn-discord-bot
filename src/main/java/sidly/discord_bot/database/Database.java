package sidly.discord_bot.database;

import sidly.discord_bot.Utils;
import sidly.discord_bot.commands.demotion_promotion.RequirementList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    public Map<Utils.RankList, RequirementList> promotionRequirements;
    public Map<String, String> verifiedMembersByIgn;
    public Map<String, String> verifiedMembersByDiscordId;
    public Map<String, PlayerDataShortened> allPlayers;
    public Map<String, PlaytimeHistoryList> playtimeHistory;

    public Database() {
        this.promotionRequirements = new HashMap<>();
        this.verifiedMembersByDiscordId = new HashMap<>();
        this.verifiedMembersByIgn = new HashMap<>();
        this.allPlayers = new HashMap<>();
        this.playtimeHistory = new HashMap<>();
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
