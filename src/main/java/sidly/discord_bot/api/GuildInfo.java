package sidly.discord_bot.api;

import sidly.discord_bot.Utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

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
        public int total;
        public Map<String, MemberInfo> owner;
        public Map<String, MemberInfo> chief;
        public Map<String, MemberInfo> strategist;
        public Map<String, MemberInfo> captain;
        public Map<String, MemberInfo> recruiter;
        public Map<String, MemberInfo> recruit;

        public Utils.RankList getRankOfMember(String uuid) {
            if (owner != null && owner.containsKey(uuid)) return Utils.RankList.Owner;
            if (chief != null && chief.containsKey(uuid)) return Utils.RankList.Chief;
            if (strategist != null && strategist.containsKey(uuid)) return Utils.RankList.Strategist;
            if (captain != null && captain.containsKey(uuid)) return Utils.RankList.Captain;
            if (recruiter != null && recruiter.containsKey(uuid)) return Utils.RankList.Recruiter;
            if (recruit != null && recruit.containsKey(uuid)) return Utils.RankList.Recruit;
            return null; // not found in any rank
        }
        public MemberInfo getMemberInfo(String uuid) {
            Utils.RankList rank = getRankOfMember(uuid);
            if (rank == null) return null;

            return switch (rank) {
                case Owner -> owner.get(uuid);
                case Chief -> chief.get(uuid);
                case Strategist -> strategist.get(uuid);
                case Captain -> captain.get(uuid);
                case Recruiter -> recruiter.get(uuid);
                case Recruit -> recruit.get(uuid);
            };
        }
        public Map<String, MemberInfo> getAllMembers() {
            Map<String, MemberInfo> combined = new HashMap<>();

            combined.putAll(owner);
            combined.putAll(chief);
            combined.putAll(strategist);
            combined.putAll(captain);
            combined.putAll(recruiter);
            combined.putAll(recruit);

            return combined;
        }

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
}
