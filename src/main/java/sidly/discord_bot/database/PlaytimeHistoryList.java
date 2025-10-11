package sidly.discord_bot.database;

import sidly.discord_bot.Utils;

import java.util.AbstractMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaytimeHistoryList {
    private final List<PlaytimeHistoryEntry> playtimeHistory;

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

        if (weeks == 1) return totalIncrease; // if its one week return the playtime that week dont average it to 7d

        // Scale to a 7-day (per-week) rate
        double millisInWeek = TimeUnit.DAYS.toMillis(7);
        double result = totalIncrease / (timeSpan / millisInWeek);
        return result >= 0 ? result : -1;
    }

    public AbstractMap.SimpleEntry<Long, Long> getAverageTimeSpan(int weeks) {
        if (playtimeHistory.size() < 2) {
            return new AbstractMap.SimpleEntry<>(0L, 0L);
        }

        // Start from the end and go back up to 'weeks' entries
        int endIndex = playtimeHistory.size() - 1;
        int startIndex = Math.max(0, playtimeHistory.size() - weeks - 1);

        long startTime = playtimeHistory.get(startIndex).timeLogged; // assuming millis or seconds
        long endTime = playtimeHistory.get(endIndex).timeLogged;

        return new AbstractMap.SimpleEntry<>(startTime, endTime);
    }

    @Deprecated
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

    public String getWarsReport() {
        if (playtimeHistory.isEmpty()) {
            return "No data available";
        }

        // Weeks we want to check back
        int[] weeks = {1, 2, 4, 6, 12};
        long now = playtimeHistory.getLast().timeLogged;
        int currentWars = playtimeHistory.getLast().wars;

        long millisInWeek = TimeUnit.DAYS.toMillis(7);

        StringBuilder sb = new StringBuilder();

        // Line 1: current wars
        sb.append("Since ")
                .append(Utils.getDiscordTimestamp(now, true))
                .append(" total wars: ")
                .append(currentWars)
                .append("\n");

        // Lines for each week checkpoint
        for (int week : weeks) {
            long targetTime = now - (week * millisInWeek);

            // Find the closest entry at or before this time
            PlaytimeHistoryEntry closest = null;
            for (int j = playtimeHistory.size() - 1; j >= 0; j--) {
                if (playtimeHistory.get(j).timeLogged <= targetTime) {
                    closest = playtimeHistory.get(j);
                    break;
                }
            }

            if (closest != null) {
                int warsThen = closest.wars;
                int diff = currentWars - warsThen;

                sb.append("Since ")
                        .append(Utils.getDiscordTimestamp(closest.timeLogged, true))
                        .append(" (").append(week).append("w ago): ")
                        .append(diff >= 0 ? "+" : "").append(diff)
                        .append(" wars (was ").append(warsThen).append(")")
                        .append("\n");
            } else {
                sb.append("Since ").append(week).append("w ago: n/a\n");
            }
        }

        return sb.toString().trim();
    }


    public static class PlaytimeHistoryEntry {
        public double playtime;
        public long timeLogged;
        public int wars;

        public long getTimeLogged() {
            return timeLogged;
        }

        public PlaytimeHistoryEntry(double playtime, long timeLogged, int wars) {
            this.playtime = playtime;
            this.timeLogged = timeLogged;
            this.wars = wars;
        }

        @Override
        public String toString(){
            return playtime + " hours " + Utils.getDiscordTimestamp(timeLogged, true);
        }
    }
}
