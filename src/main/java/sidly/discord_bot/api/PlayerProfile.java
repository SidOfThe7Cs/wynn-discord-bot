package sidly.discord_bot.api;

import java.util.List;
import java.util.Map;

public class PlayerProfile {
    public long lastUpdated;

    public String username;
    public boolean online;
    public String server;
    public String activeCharacter; // Nullable
    public String uuid;
    public String rank;
    public String rankBadge;
    public LegacyRankColour legacyRankColour;
    public String shortenedRank;
    public String supportRank;
    public boolean veteran;
    public String firstJoin;
    public String lastJoin;
    public double playtime;
    public Guild guild;
    public GlobalData globalData;
    public Integer forumLink; // Nullable
    public Map<String, Integer> ranking;
    public Map<String, Integer> previousRanking;
    public boolean publicProfile;
    public Map<String, CharacterData> characters;

    public static class LegacyRankColour {
        public String main;
        public String sub;
    }

    public static class Guild {
        public String name;
        public String prefix;
        public String rank;
        public String rankStars;
    }

    public static class GlobalData {
        public int wars;
        public int totalLevels;
        public int killedMobs;
        public int chestsFound;
        public Dungeons dungeons;
        public Raids raids;
        public int completedQuests;
        public Pvp pvp;
    }

    public static class Dungeons {
        public int total;
        public Map<String, Integer> list;
    }

    public static class Raids {
        public int total;
        public Map<String, Integer> list;
    }

    public static class Pvp {
        public int kills;
        public int deaths;
    }

    public static class CharacterData {
        public String type;
        public String nickname;
        public int level;
        public int xp;
        public int xpPercent;
        public int totalLevel;
        public int wars;
        public double playtime;
        public int mobsKilled;
        public int chestsFound;
        public int blocksWalked;
        public int itemsIdentified;
        public int logins;
        public int deaths;
        public int discoveries;
        public Pvp pvp;
        public List<String> gamemode;
        public SkillPoints skillPoints;
        public Map<String, Profession> professions;
        public Dungeons dungeons;
        public Raids raids;
        public List<String> quests;
    }

    public static class SkillPoints {
        public int strength;
        public int dexterity;
        public int intelligence;
        public int defence;
        public int agility;
    }

    public static class Profession {
        public int level;
        public int xpPercent;
    }

    public void update(){
        lastUpdated = System.currentTimeMillis();
    }
}
