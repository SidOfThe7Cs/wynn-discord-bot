package sidly.discord_bot;

import net.dv8tion.jda.api.entities.Member;
import sidly.discord_bot.commands.demotion_promotion.RequirementList;

import java.util.HashMap;
import java.util.Map;

public class Database {
    public Map<Utils.RankList, RequirementList> promotionRequirements;
    public Map<String, Member> verifiedMembers;

    public Database() {
        this.promotionRequirements = new HashMap<>();
        this.verifiedMembers = new HashMap<>();
    }
}
