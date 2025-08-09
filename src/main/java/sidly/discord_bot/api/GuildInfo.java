package sidly.discord_bot.api;

import java.util.List;
import java.util.Map;

public class GuildInfo {
    public long lastUpdated;

    public String uuid;
    public String name;
    public String prefix;
    public int level;
    public int xpPercent;
    public int territories;
    public int wars;
    public String created;
    public Members members;
    public int online;
    public Banner banner;
    public Map<String, SeasonRank> seasonRanks;

    public static class Members {
        public int total;
        public Map<String, MemberInfo> owner;
        public Map<String, MemberInfo> chief;
        public Map<String, MemberInfo> strategist;
        public Map<String, MemberInfo> captain;
        public Map<String, MemberInfo> recruiter;
        public Map<String, MemberInfo> recruit;
    }

    public static class MemberInfo {
        public String username;
        public boolean online;
        public String server; // nullable
        public long contributed;
        public int contributionRank;
        public String joined;
    }

    public static class Banner {
        public String base;
        public int tier;
        public String structure;
        public List<Layer> layers;

        public static class Layer {
            private String colour;
            private String pattern;
        }
    }

    public static class SeasonRank {
        public int rating;
        public int finalTerritories;
    }

    public void update(){
        lastUpdated = System.currentTimeMillis();
    }
}
