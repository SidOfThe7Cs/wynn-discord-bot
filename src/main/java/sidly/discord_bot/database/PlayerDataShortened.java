package sidly.discord_bot.database;

import sidly.discord_bot.api.PlayerProfile;

public class PlayerDataShortened {
    public String username;
    public String uuid;
    public int level;
    public int guildWars;
    public double latestPlaytime;
    public long lastModified;
    public String lastJoined;
    public String supportRank;

    public PlayerDataShortened(PlayerProfile player){
        this.username = player.username;
        this.uuid = player.uuid;
        this.level = player.getHighestLevel();
        this.guildWars = (player.globalData == null) ? 0 : player.globalData.wars;
        this.latestPlaytime = player.playtime;
        this.lastModified = System.currentTimeMillis();
        this.lastJoined = player.lastJoin;
        this.supportRank = player.supportRank;
    }
}
