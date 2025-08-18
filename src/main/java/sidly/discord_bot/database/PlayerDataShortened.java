package sidly.discord_bot.database;

import sidly.discord_bot.Utils;
import sidly.discord_bot.api.PlayerProfile;

public class PlayerDataShortened {
    public String username;
    public String uuid;
    public int level;
    public int guildWars;
    public double latestPlaytime;
    public long lastModified;
    public String lastJoined;
    public String firstJoined;
    public String supportRank;
    public int highestLvl;
    public int wars;

    public PlayerDataShortened() {}

    public PlayerDataShortened(PlayerProfile player){
        this.username = player.username;
        this.uuid = player.uuid;
        this.level = player.getHighestLevel();
        this.guildWars = (player.globalData == null) ? 0 : player.globalData.wars;
        this.latestPlaytime = player.playtime;
        this.lastModified = System.currentTimeMillis();
        this.lastJoined = player.lastJoin;
        this.supportRank = player.supportRank;
        this.highestLvl = player.getHighestLevel();
        if (player.globalData != null) {
            this.wars = player.globalData.wars;
        } else this.wars = 0;
        this.firstJoined = player.firstJoin;
    }


    public double getAllTimeWeeklyAverage() {
        long days = Utils.daysSinceIso(firstJoined);
        double weeks = (double) (days / 7L);
        return latestPlaytime / weeks;
    }
}
