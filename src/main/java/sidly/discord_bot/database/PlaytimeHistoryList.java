package sidly.discord_bot.database;

import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaytimeHistoryList {
    private static final long SIX_AND_A_HALF_DAYS_IN_MILLIS = TimeUnit.HOURS.toMillis(156);

    public List<PlaytimeHistoryEntry> getPlaytimeHistory() {
        return playtimeHistory;
    }

    private List<PlaytimeHistoryEntry> playtimeHistory = new ArrayList<>();

    public void addPlaytimeIfNeeded(PlayerDataShortened playerData){
        if (playtimeHistory.isEmpty()){
            playtimeHistory.add(new PlaytimeHistoryEntry(playerData.latestPlaytime));
        } else if (playtimeHistory.getLast().timeLogged + SIX_AND_A_HALF_DAYS_IN_MILLIS <= playerData.lastModified){
            playtimeHistory.add(new PlaytimeHistoryEntry(playerData.latestPlaytime));
        }
    }

    public PlaytimeHistoryList() {
    }

    public static class PlaytimeHistoryEntry {
        double playtime;
        long timeLogged;

        public PlaytimeHistoryEntry(double playtime) {
            this.playtime = playtime;
            this.timeLogged = System.currentTimeMillis();
        }

        @Override
        public String toString(){
            return playtime + " hours " + Utils.getDiscordTimestamp(timeLogged, true);
        }
    }
}
