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

    public double getAverage(int weeks) {
        if (playtimeHistory.size() < 2) { // Need at least 2 points to calculate an increase
            return 0;
        }

        // Limit to available number of *intervals* (one less than entries)
        int count = Math.min(weeks, playtimeHistory.size() - 1);
        double totalIncrease = 0;

        // Start from the end and calculate increases between consecutive entries
        for (int i = playtimeHistory.size() - count; i < playtimeHistory.size(); i++) {
            double current = playtimeHistory.get(i).playtime;
            double previous = playtimeHistory.get(i - 1).playtime;
            totalIncrease += (current - previous);
        }

        // Average the increases
        return totalIncrease / count;
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
