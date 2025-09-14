package sidly.discord_bot.database.tables;

import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.PlaytimeHistoryList;
import sidly.discord_bot.database.SQLDB;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
            String sql = "INSERT INTO playtime_history (uuid, playtime, timeLogged, wars) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid);
                pstmt.setDouble(2, playerData.latestPlaytime);
                pstmt.setLong(3, playerData.lastModified);
                pstmt.setInt(4, playerData.wars);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            // Update the latest entry
            String sql = "UPDATE playtime_history SET playtime = ?, timeLogged = ?, wars = ? WHERE uuid = ? AND timeLogged = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setDouble(1, playerData.latestPlaytime);
                pstmt.setLong(2, playerData.lastModified);
                pstmt.setInt(3, playerData.wars);
                pstmt.setString(4, uuid);
                pstmt.setLong(5, latestEntry.timeLogged);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static void addPlaytimeIfNeeded(Set<PlayerDataShortened> players) {
        if (players == null || players.isEmpty()) return;

        String insertSql = "INSERT INTO playtime_history (uuid, playtime, timeLogged, wars) VALUES (?, ?, ?, ?)";
        String updateSql = "UPDATE playtime_history SET playtime = ?, timeLogged = ?, wars = ? WHERE uuid = ? AND timeLogged = ?";

        try (
                PreparedStatement insertStmt = connection.prepareStatement(insertSql);
                PreparedStatement updateStmt = connection.prepareStatement(updateSql)
        ) {
            for (PlayerDataShortened playerData : players) {
                String uuid = playerData.uuid;
                List<PlaytimeHistoryList.PlaytimeHistoryEntry> playtimeHistory = getPlaytimeHistory(uuid).getPlaytimeHistory();
                playtimeHistory.sort(Comparator.comparingLong(PlaytimeHistoryList.PlaytimeHistoryEntry::getTimeLogged).reversed());
                PlaytimeHistoryList.PlaytimeHistoryEntry latestEntry = !playtimeHistory.isEmpty() ? playtimeHistory.get(0) : null;
                boolean shouldInsert = isShouldInsert(playtimeHistory, latestEntry);

                if (shouldInsert) {
                    insertStmt.setString(1, uuid);
                    insertStmt.setDouble(2, playerData.latestPlaytime);
                    insertStmt.setLong(3, playerData.lastModified);
                    insertStmt.setInt(4, playerData.wars);
                    insertStmt.addBatch();
                } else {
                    updateStmt.setDouble(1, playerData.latestPlaytime);
                    updateStmt.setLong(2, playerData.lastModified);
                    updateStmt.setInt(3, playerData.wars);
                    updateStmt.setString(4, uuid);
                    updateStmt.setLong(5, latestEntry.timeLogged);
                    updateStmt.addBatch();
                }
            }

            // Run all batched inserts and updates
            insertStmt.executeBatch();
            updateStmt.executeBatch();

        } catch (SQLException e) {
            e.printStackTrace();
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
        String sql = "SELECT playtime, timeLogged, wars FROM playtime_history WHERE uuid = ? ORDER BY timeLogged ASC";

        List<PlaytimeHistoryList.PlaytimeHistoryEntry> entries = new ArrayList<>();
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                PlaytimeHistoryList.PlaytimeHistoryEntry entry =
                        new PlaytimeHistoryList.PlaytimeHistoryEntry(
                                rs.getDouble("playtime"),
                                rs.getLong("timeLogged"),
                                rs.getInt("wars")
                        );
                entries.add(entry);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new PlaytimeHistoryList(entries);
    }

    public static Map<String, PlaytimeHistoryList> getPlaytimeHistoryForAll(Collection<String> uuids) {
        if (uuids.isEmpty()) return Collections.emptyMap();

        String placeholders = uuids.stream().map(u -> "?").collect(Collectors.joining(","));
        String sql = "SELECT uuid, playtime, timeLogged, wars FROM playtime_history WHERE uuid IN (" + placeholders + ") ORDER BY uuid, timeLogged ASC";

        Map<String, List<PlaytimeHistoryList.PlaytimeHistoryEntry>> tempMap = new HashMap<>();

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            int index = 1;
            for (String uuid : uuids) {
                pstmt.setString(index++, uuid);
            }

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                double playtime = rs.getDouble("playtime");
                long timeLogged = rs.getLong("timeLogged");
                int wars = rs.getInt("wars");

                PlaytimeHistoryList.PlaytimeHistoryEntry entry =
                        new PlaytimeHistoryList.PlaytimeHistoryEntry(playtime, timeLogged, wars);
                tempMap.computeIfAbsent(uuid, k -> new ArrayList<>()).add(entry);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        Map<String, PlaytimeHistoryList> result = new HashMap<>();
        for (Map.Entry<String, List<PlaytimeHistoryList.PlaytimeHistoryEntry>> e : tempMap.entrySet()) {
            result.put(e.getKey(), new PlaytimeHistoryList(e.getValue()));
        }

        return result;
    }

    public static List<String> getSortedPlaytimeReport() {
        List<String> results = new ArrayList<>();

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

        List<PlayerReport> reports = new ArrayList<>();
        for (String uuid : uuids) {
            List<PlaytimeHistoryList.PlaytimeHistoryEntry> entries = new ArrayList<>();
            String historySql = "SELECT playtime, timeLogged, wars FROM playtime_history WHERE uuid = ? ORDER BY timeLogged ASC";
            try (PreparedStatement ps = connection.prepareStatement(historySql)) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(new PlaytimeHistoryList.PlaytimeHistoryEntry(
                                rs.getDouble("playtime"),
                                rs.getLong("timeLogged"),
                                rs.getInt("wars")
                        ));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (entries.isEmpty()) continue;

            PlaytimeHistoryList historyList = new PlaytimeHistoryList(entries);
            PlayerDataShortened playerData = Players.get(uuid);

            if (playerData != null) {
                reports.add(new PlayerReport(
                        playerData.username,
                        historyList.getLinear10WeekAverage(),
                        historyList.getAverage(1),
                        historyList.getAverage(5),
                        historyList.getAverage(20),
                        playerData.getAllTimeWeeklyAverage()
                ));
            }
        }

        reports.sort((a, b) -> Double.compare(b.linear10WeekAverage, a.linear10WeekAverage));

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
