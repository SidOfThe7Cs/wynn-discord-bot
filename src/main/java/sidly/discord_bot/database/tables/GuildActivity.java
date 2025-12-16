package sidly.discord_bot.database.tables;

import sidly.discord_bot.Utils;
import sidly.discord_bot.database.records.GuildAverages;
import sidly.discord_bot.database.records.GuildName;
import sidly.discord_bot.database.records.TimestampedDouble;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static sidly.discord_bot.database.SQLDB.connection;
import static sidly.discord_bot.database.SQLDB.executeQuery;

public class GuildActivity {
    private static final int MONTH_TABLE_COUNT = 5;
    private static final String TABLE_PREFIX = "guild_activity_";

    private static void initializeTableRotation() {
        // Clean old tables if they exist beyond our rotation count
        for (int i = MONTH_TABLE_COUNT + 1; i <= MONTH_TABLE_COUNT + 5; i++) {
            executeQuery("DROP TABLE IF EXISTS " + TABLE_PREFIX + i);
        }
    }

    // Get current month's table suffix (1-5)
    private static int getCurrentTableSuffix() {
        // Get current month (0-11) and map to 1-5 rotation
        int currentMonth = LocalDate.now().getMonthValue() - 1; // 0-11
        return (currentMonth % MONTH_TABLE_COUNT) + 1;
    }

    // Get table name for a specific month offset (0 = current month, 1 = previous month, etc.)
    private static String getTableName(int monthsAgo) {
        int suffix = ((LocalDate.now().getMonthValue() - 1 - monthsAgo) % MONTH_TABLE_COUNT);
        if (suffix < 0) suffix += MONTH_TABLE_COUNT;
        return TABLE_PREFIX + (suffix + 1);
    }

    // Clear oldest table when rotating (called at start of 5th month)
    private static void rotateTables() {
        // Get the oldest table suffix (current month + 1, but wrapped around 5)
        int oldestTableSuffix = (getCurrentTableSuffix() % MONTH_TABLE_COUNT) + 1;

        // Clear the oldest table
        String oldestTable = TABLE_PREFIX + oldestTableSuffix;
        executeQuery("DELETE FROM " + oldestTable);
        executeQuery("VACUUM");

        System.out.println("Cleared table: " + oldestTable);
    }

    // Check and perform rotation at the start of each month
    private static void checkAndRotate() {
        LocalDate now = LocalDate.now();
        if (now.getDayOfMonth() == 1) {
            // First day of month, rotate tables
            rotateTables();
        }
    }

    public static void add(String uuid, String guildPrefix, String guildName, double onlineCount, double captainsOnline) {
        checkAndRotate(); // Check if rotation is needed

        int hour = Utils.getHoursSinceDayStarted(System.currentTimeMillis());
        long currentTime = System.currentTimeMillis();
        String tableName = getTableName(0); // Current month's table

        // Calculate new average using incremental averaging formula
        // new_avg = old_avg + (new_value - old_avg) / (counter + 1)
        String sql = "INSERT INTO " + tableName + " (uuid, prefix, name, hour, avg_online_count, avg_captains_online, counter, timestamp, last_updated) " +
                "VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?) " +
                "ON CONFLICT(uuid, hour) DO UPDATE SET " +
                "avg_online_count = avg_online_count + (? - avg_online_count) / (counter + 1), " +
                "avg_captains_online = avg_captains_online + (? - avg_captains_online) / (counter + 1), " +
                "counter = counter + 1, " +
                "last_updated = ?, " +
                "prefix = CASE WHEN prefix IS NULL THEN ? ELSE prefix END, " +
                "name = CASE WHEN name IS NULL THEN ? ELSE name END";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int idx = 1;
            // INSERT values
            ps.setString(idx++, uuid);
            ps.setString(idx++, guildPrefix);
            ps.setString(idx++, guildName);
            ps.setInt(idx++, hour);
            ps.setDouble(idx++, onlineCount);
            ps.setDouble(idx++, captainsOnline);
            ps.setLong(idx++, currentTime);
            ps.setLong(idx++, currentTime);

            // UPDATE values for online count
            ps.setDouble(idx++, onlineCount);

            // UPDATE values for captains online
            ps.setDouble(idx++, captainsOnline);

            // UPDATE last_updated
            ps.setLong(idx++, currentTime);

            // UPDATE prefix and name (only if null)
            ps.setString(idx++, guildPrefix);
            ps.setString(idx++, guildName);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean containsPrefix(String prefix) {
        // Check all tables for the prefix
        for (int i = 0; i < MONTH_TABLE_COUNT; i++) {
            String tableName = getTableName(i);
            String sql = "SELECT 1 FROM " + tableName + " WHERE prefix = ? LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, prefix);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return true;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static List<TimestampedDouble> getActivityEntries(String uuid, boolean captainPlus) {
        List<TimestampedDouble> results = new ArrayList<>();
        String column = captainPlus ? "avg_captains_online" : "avg_online_count";

        // Query all tables
        for (int i = 0; i < MONTH_TABLE_COUNT; i++) {
            String tableName = getTableName(i);
            String sql = "SELECT " + column + ", timestamp FROM " + tableName +
                    " WHERE uuid = ? AND counter > 0";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double value = rs.getDouble(column);
                        long time = rs.getLong("timestamp");
                        results.add(new TimestampedDouble(value, time));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return results;
    }

    public static List<TimestampedDouble> getActivityEntries(String uuid, int hour, boolean captainPlus) {
        List<TimestampedDouble> results = new ArrayList<>();
        String column = captainPlus ? "avg_captains_online" : "avg_online_count";

        // Query all tables
        for (int i = 0; i < MONTH_TABLE_COUNT; i++) {
            String tableName = getTableName(i);
            String sql = "SELECT " + column + ", timestamp FROM " + tableName +
                    " WHERE uuid = ? AND hour = ? AND counter > 0";

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid);
                ps.setInt(2, hour);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        double value = rs.getDouble(column);
                        long time = rs.getLong("timestamp");
                        results.add(new TimestampedDouble(value, time));
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return results;
    }

    public static String getGuildName(String uuid) {
        // Check all tables, starting with current month
        for (int i = 0; i < MONTH_TABLE_COUNT; i++) {
            String tableName = getTableName(i);
            String sql = "SELECT name FROM " + tableName + " WHERE uuid = ? AND name IS NOT NULL LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("name");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getGuildPrefix(String uuid) {
        // Check all tables, starting with current month
        for (int i = 0; i < MONTH_TABLE_COUNT; i++) {
            String tableName = getTableName(i);
            String sql = "SELECT prefix FROM " + tableName + " WHERE uuid = ? AND prefix IS NOT NULL LIMIT 1";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, uuid);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("prefix");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static double getAverageOnline(String uuid, int hour, int daysInPast, boolean captainPlus) {
        if (hour < 0 || hour > 23) return Double.NaN;

        long startTime = 0;
        if (daysInPast > 0) {
            startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysInPast);
        }

        List<TimestampedDouble> list = getActivityEntries(uuid, hour, captainPlus);
        return getWeightedAverage(list, startTime);
    }

    public static double getAverageOnline(String uuid, int daysInPast, boolean captainPlus) {
        long startTime = 0;
        if (daysInPast > 0) {
            startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysInPast);
        }

        List<TimestampedDouble> list = getActivityEntries(uuid, captainPlus);
        return getWeightedAverage(list, startTime);
    }

    private static double getWeightedAverage(List<TimestampedDouble> list, long startTime) {
        if (list == null || list.isEmpty()) return Double.NaN;

        double weightedSum = 0;
        double totalWeight = 0;

        for (TimestampedDouble entry : list) {
            if (entry.time() >= startTime) {
                // Use recency weighting - more recent entries get higher weight
                double weight = 1.0 + (entry.time() - startTime) / (double)(TimeUnit.DAYS.toMillis(30));
                weightedSum += entry.value() * weight;
                totalWeight += weight;
            }
        }

        return totalWeight > 0 ? weightedSum / totalWeight : Double.NaN;
    }

    public static Set<String> getActivePrefixes() {
        Set<String> prefixes = new HashSet<>();
        String sql = "SELECT prefix FROM guilds_40_plus WHERE low_priority = 0";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                prefixes.add(rs.getString("prefix"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return prefixes;
    }
    public static List<GuildAverages> getGuildAverages(int monthValue) {
        List<GuildAverages> results = new ArrayList<>();

        // Validate month parameter
        if (monthValue < 1 || monthValue > MONTH_TABLE_COUNT) {
            throw new IllegalArgumentException("Month value must be between 1 and " + MONTH_TABLE_COUNT);
        }

        Set<String> activePrefixes = getActivePrefixes();
        Set<String> uuids = AllGuilds.getAllByPrefixes(activePrefixes).stream()
                .map(GuildName::uuid)
                .collect(Collectors.toSet());

        if (uuids.isEmpty()) return results;

        // Build placeholders for the IN clause
        String uuidPlaceholders = uuids.stream().map(u -> "?").collect(Collectors.joining(","));

        // Build UNION query for the most recent monthValue tables
        StringBuilder unionQuery = new StringBuilder();

        // Get the most recent monthValue tables (0 = current, 1 = previous, etc.)
        for (int i = 0; i < monthValue; i++) {
            String tableName = getTableName(i); // getTableName(0) = most recent, getTableName(1) = previous, etc.
            if (i > 0) unionQuery.append(" UNION ALL ");

            unionQuery.append("SELECT uuid, avg_online_count, avg_captains_online, counter FROM ")
                    .append(tableName)
                    .append(" WHERE uuid IN (").append(uuidPlaceholders).append(")");
        }

        // Final aggregation query with weighted averages
        String sql = "SELECT uuid, " +
                "SUM(avg_online_count * counter) / SUM(counter) as weighted_avg_online, " +
                "SUM(avg_captains_online * counter) / SUM(counter) as weighted_avg_captains " +
                "FROM (" + unionQuery + ") AS all_tables " +
                "WHERE counter > 0 " +
                "GROUP BY uuid " +
                "ORDER BY weighted_avg_online DESC";


        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int idx = 1;
            for (String uuid : uuids) {
                stmt.setString(idx++, uuid);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                double avgOnline = rs.getDouble("weighted_avg_online");
                double avgCaptains = rs.getDouble("weighted_avg_captains");

                results.add(new GuildAverages(uuid, avgOnline, avgCaptains));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }
}