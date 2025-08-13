package sidly.discord_bot.database;

import sidly.discord_bot.api.PlayerProfile;

public class PlayerDataShortened {
    public String username;
    public String uuid;
    public int level;
    public int guildWars;
    public double latestPlaytime;
    public long lastModified;

    public PlayerDataShortened(PlayerProfile player){
        this.username = player.username;
        this.uuid = player.uuid;
        this.level = player.getHighestLevel();
        this.guildWars = player.globalData.wars;
        this.latestPlaytime = player.playtime;
        this.lastModified = System.currentTimeMillis();
    }
}
