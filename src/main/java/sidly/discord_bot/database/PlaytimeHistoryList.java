package sidly.discord_bot.database;

import sidly.discord_bot.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaytimeHistoryList {
    private List<PlaytimeHistoryEntry> playtimeHistory = new ArrayList<>();

    public PlaytimeHistoryList() {}
    public PlaytimeHistoryList(List<PlaytimeHistoryEntry> playTimeHistory) {
        this.playtimeHistory = playTimeHistory;
    }

    public List<PlaytimeHistoryEntry> getPlaytimeHistory() {
        return playtimeHistory;
    }

    public double getAverage(int weeks) {
        if (playtimeHistory.size() < 2) {
            return 0;
        }

        // Start from the end and go back up to 'weeks' entries
        int endIndex = playtimeHistory.size() - 1;
        int startIndex = Math.max(0, playtimeHistory.size() - weeks - 1);

        double startPlaytime = playtimeHistory.get(startIndex).playtime;
        double endPlaytime = playtimeHistory.get(endIndex).playtime;

        long startTime = playtimeHistory.get(startIndex).timeLogged; // assuming millis or seconds
        long endTime = playtimeHistory.get(endIndex).timeLogged;

        // Total increase across the real time span
        double totalIncrease = endPlaytime - startPlaytime;
        long timeSpan = endTime - startTime;

        if (timeSpan <= 0) {
            return 0;
        }

        // Scale to a 7-day (per-week) rate
        double millisInWeek = TimeUnit.DAYS.toMillis(7);
        return totalIncrease / (timeSpan / millisInWeek);
    }

    public double getLinear10WeekAverage() {
        if (playtimeHistory.size() < 2) {
            return 0;
        }

        double totalWeighted = 0;
        double totalWeights = 0;

        // Max 10 weeks back (so up to 11 entries)
        int count = Math.min(10, playtimeHistory.size() - 1);

        for (int i = 0; i < count; i++) {
            int currentIndex = playtimeHistory.size() - 1 - i;
            int prevIndex = currentIndex - 1;

            double current = playtimeHistory.get(currentIndex).playtime;
            double previous = playtimeHistory.get(prevIndex).playtime;

            long startTime = playtimeHistory.get(prevIndex).timeLogged;
            long endTime = playtimeHistory.get(currentIndex).timeLogged;
            long timeSpan = endTime - startTime;
            double millisInWeek = TimeUnit.DAYS.toMillis(7);
            double timeSpanWeeks = (double) timeSpan / millisInWeek;
            double increase = (current - previous) / timeSpanWeeks;


            // Weight = (3/4)^i
            double weight = Math.pow(0.75, i);

            totalWeighted += increase * weight;
            totalWeights += weight;
        }

        return totalWeighted / totalWeights;
    }

    public static class PlaytimeHistoryEntry {
        public double playtime;
        public long timeLogged;

        public PlaytimeHistoryEntry(double playtime, long timeLogged) {
            this.playtime = playtime;
            this.timeLogged = timeLogged;
        }

        @Override
        public String toString(){
            return playtime + " hours " + Utils.getDiscordTimestamp(timeLogged, true);
        }
    }
}
