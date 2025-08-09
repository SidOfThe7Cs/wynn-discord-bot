package sidly.discord_bot;

import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.api.PlayerProfile;
import sidly.discord_bot.commands.demotion_promotion.Requirement;
import sidly.discord_bot.commands.demotion_promotion.RequirementList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Database {
    public Map<String, PlayerProfile> allPlayers;
    public Map<String, GuildInfo> allGuilds;
    public Map<Utils.RankList, RequirementList> promotionRequirements;

    public Database() {
        this.allPlayers = new HashMap<>();
        this.allGuilds = new HashMap<>();
        this.promotionRequirements = new HashMap<>();
    }
}
