package sidly.discord_bot.api;

import java.util.List;
import java.util.Map;

public class GuildInfo {
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
        private int total;
        private Map<String, MemberInfo> owner;
        private Map<String, MemberInfo> chief;
        private Map<String, MemberInfo> strategist;
        private Map<String, MemberInfo> captain;
        private Map<String, MemberInfo> recruiter;
        private Map<String, MemberInfo> recruit;
    }

    public static class MemberInfo {
        private String nameOrUuid;
        private boolean online;
        private String server; // nullable
        private long contributed;
        private int contributionRank;
        private String joined;
    }

    public static class Banner {
        private String base;
        private int tier;
        private String structure;
        private List<Layer> layers;

        public static class Layer {
            private String colour;
            private String pattern;
        }
    }

    public static class SeasonRank {
        private int rating;
        private int finalTerritories;
    }
}
