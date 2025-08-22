package sidly.discord_bot.database.tables;

import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.PlaytimeHistoryList;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static sidly.discord_bot.database.SQLDB.connection;

public class PlaytimeHistory {
    private static final long SIX_AND_A_HALF_DAYS_IN_MILLIS = TimeUnit.HOURS.toMillis(156);

    public static void addPlaytimeIfNeeded(PlayerDataShortened playerData) {
        String uuid = playerData.uuid;
        List<PlaytimeHistoryList.PlaytimeHistoryEntry> playtimeHistory = getPlaytimeHistory(uuid).getPlaytimeHistory();

        // Sort entries by timeLogged descending (latest first)
        playtimeHistory.sort(Comparator.comparingLong(PlaytimeHistoryList.PlaytimeHistoryEntry::getTimeLogged).reversed());

        PlaytimeHistoryList.PlaytimeHistoryEntry latestEntry = !playtimeHistory.isEmpty() ? playtimeHistory.get(0) : null;
        boolean shouldInsert = isShouldInsert(playtimeHistory, latestEntry);

        if (shouldInsert) {
            // Insert new entry
            String sql = "INSERT INTO playtime_history (uuid, playtime, timeLogged) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid);
                pstmt.setDouble(2, playerData.latestPlaytime);
                pstmt.setLong(3, playerData.lastModified);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            // Update the latest entry
            String sql = "UPDATE playtime_history SET playtime = ?, timeLogged = ? WHERE uuid = ? AND timeLogged = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setDouble(1, playerData.latestPlaytime);
                pstmt.setLong(2, playerData.lastModified);
                pstmt.setString(3, uuid);
                pstmt.setLong(4, latestEntry.timeLogged);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isShouldInsert(List<PlaytimeHistoryList.PlaytimeHistoryEntry> playtimeHistory, PlaytimeHistoryList.PlaytimeHistoryEntry latestEntry) {
        PlaytimeHistoryList.PlaytimeHistoryEntry secondLatestEntry = playtimeHistory.size() > 1 ? playtimeHistory.get(1) : null;

        boolean shouldInsert = false;

        if (latestEntry == null) {
            // No entries exist, always insert
            shouldInsert = true;
        } else if (secondLatestEntry == null) {
            // Only one entry exists, insert new
            shouldInsert = true;
        } else if (latestEntry.timeLogged - secondLatestEntry.timeLogged >= SIX_AND_A_HALF_DAYS_IN_MILLIS) {
            // Latest is at least 6.5 days after the second-latest → insert new
            shouldInsert = true;
        }
        return shouldInsert;
    }


    public static PlaytimeHistoryList getPlaytimeHistory(String uuid) {
        String sql = "SELECT playtime, timeLogged FROM playtime_history WHERE uuid = ? ORDER BY timeLogged ASC";

        List<PlaytimeHistoryList.PlaytimeHistoryEntry> entries = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                PlaytimeHistoryList.PlaytimeHistoryEntry entry = new PlaytimeHistoryList.PlaytimeHistoryEntry(rs.getDouble("playtime"), rs.getLong("timeLogged"));
                entries.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new PlaytimeHistoryList(entries);
    }

    public static List<String> getSortedPlaytimeReport() {
        List<String> results = new ArrayList<>();

        // Step 1: get all unique UUIDs from the table
        List<String> uuids = new ArrayList<>();
        String uuidSql = "SELECT DISTINCT uuid FROM playtime_history";
        try (PreparedStatement ps = connection.prepareStatement(uuidSql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                uuids.add(rs.getString("uuid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Step 2: fetch all playtime histories
        List<PlayerReport> reports = new ArrayList<>();
        for (String uuid : uuids) {
            List<PlaytimeHistoryList.PlaytimeHistoryEntry> entries = new ArrayList<>();
            String historySql = "SELECT playtime, timeLogged FROM playtime_history WHERE uuid = ? ORDER BY timeLogged ASC";
            try (PreparedStatement ps = connection.prepareStatement(historySql)) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(new PlaytimeHistoryList.PlaytimeHistoryEntry(
                                rs.getDouble("playtime"),
                                rs.getLong("timeLogged")
                        ));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (entries.isEmpty()) continue;

            PlaytimeHistoryList historyList = new PlaytimeHistoryList(entries);

            // Retrieve player info if needed
            PlayerDataShortened playerData = Players.get(uuid);

            PlayerDataShortened player = Players.get(uuid);
            if (player != null) {
                reports.add(new PlayerReport(
                        player.username,
                        historyList.getLinear10WeekAverage(),
                        historyList.getAverage(1),
                        historyList.getAverage(5),
                        historyList.getAverage(20),
                        playerData != null ? playerData.getAllTimeWeeklyAverage() : 0
                ));
            }
        }

        // Step 3: sort descending by linear10WeekAverage
        reports.sort((a, b) -> Double.compare(b.linear10WeekAverage, a.linear10WeekAverage));

        // Step 4: build output strings
        for (PlayerReport report : reports) {
            results.add(String.format("**%s**, %.2f, %.2f, %.2f, %.2f, %.2f\n",
                    report.uuid,
                    report.linear10WeekAverage,
                    report.avg1Week,
                    report.avg5Week,
                    report.avg20Week,
                    report.allTimeWeeklyAverage
            ));
        }

        return results;
    }

    // Helper record to hold report data
    private record PlayerReport(
            String uuid,
            double linear10WeekAverage,
            double avg1Week,
            double avg5Week,
            double avg20Week,
            double allTimeWeeklyAverage
    ) {}

}
