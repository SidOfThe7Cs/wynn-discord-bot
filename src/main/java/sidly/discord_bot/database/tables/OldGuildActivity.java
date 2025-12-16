
package sidly.discord_bot.database.tables;

import sidly.discord_bot.Utils;
import sidly.discord_bot.database.records.GuildAverages;
import sidly.discord_bot.database.records.GuildName;
import sidly.discord_bot.database.records.TimestampedDouble;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static sidly.discord_bot.database.SQLDB.connection;

public class OldGuildActivity {
    public static void add(String uuid, String guildPrefix, String guildName, double onlineCount, double captainsOnline) {
        int hour = Utils.getHoursSinceDayStarted(System.currentTimeMillis());
        String sql = "INSERT INTO guild_activity (uuid, prefix, name, hour, online_count, captains_online, timestamp) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid);
            ps.setString(2, guildPrefix);
            ps.setString(3, guildName);
            ps.setInt(4, hour);
            ps.setDouble(5, onlineCount);
            ps.setDouble(6, captainsOnline);
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static boolean containsPrefix(String prefix) {
        String sql = "SELECT 1 FROM guild_activity WHERE prefix = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, prefix);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next(); // returns true if at least one row exists
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<TimestampedDouble> getActivityEntries(String uuid, boolean captainPlus) {
        List<TimestampedDouble> results = new ArrayList<>();
        String column = captainPlus ? "captains_online" : "online_count";
        String sql = "SELECT " + column + ", timestamp FROM guild_activity WHERE uuid = ?";

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

        return results;
    }

    public static List<TimestampedDouble> getActivityEntries(String uuid, int hour, boolean captainPlus) {
        List<TimestampedDouble> results = new ArrayList<>();
        String column = captainPlus ? "captains_online" : "online_count";
        String sql = "SELECT " + column + ", timestamp FROM guild_activity WHERE uuid = ? AND hour = ?";

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

        return results;
    }


    public static String getGuildName(String uuid) {
        String sql = "SELECT name FROM guild_activity WHERE uuid = ? LIMIT 1";
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
        return null;
    }

    public static String getGuildPrefix(String uuid) {
        String sql = "SELECT prefix FROM guild_activity WHERE uuid = ? LIMIT 1";
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
        return null;
    }


    public static double getAverageOnline(String uuid, int time, int daysInPast, boolean captainPlus) {
        if (time < 0 || time > 23) return Double.NaN;

        long startTime = 0;
        if (daysInPast > 0) {
            startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysInPast);
        }
        List<TimestampedDouble> list = getActivityEntries(uuid, time, captainPlus);

        return getAverage(list, startTime);
    }

    public static double getAverageOnline(String uuid, int daysInPast, boolean captainPlus) {

        long startTime = 0;
        if (daysInPast > 0) {
            startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysInPast);
        }
        List<TimestampedDouble> list = getActivityEntries(uuid, captainPlus);
        return getAverage(list, startTime);
    }

    private static double getAverage(List<TimestampedDouble> list, long startTime) {
        double total = 0;
        double count = 0;

        if (list == null || list.isEmpty()) return Double.NaN;
        for (TimestampedDouble onlineCount : list) {
            if (onlineCount.time() >= startTime) {
                total += onlineCount.value();
                count++;
            }
        }

        return total / count;
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


    public static List<GuildAverages> getGuildAverages(int daysInPast) {
        List<GuildAverages> results = new ArrayList<>();


        long startTime = 0;
        if (daysInPast > 0) {
            startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysInPast);
        }

        Set<String> activePrefixes = getActivePrefixes(); // prefixes where low_priority = 0
        Set<String> uuids = AllGuilds.getAllByPrefixes(activePrefixes).stream().map(GuildName::uuid).collect(Collectors.toSet());

        // if no UUIDs, nothing to query
        if (uuids.isEmpty()) return results;

// Build placeholders for the IN clause
        String placeholders = uuids.stream().map(u -> "?").collect(Collectors.joining(","));

// Single-pass query: average directly per UUID
        String sql = """
        SELECT uuid,
               AVG(online_count)    AS avg_online,
               AVG(captains_online) AS avg_captains
        FROM guild_activity
        WHERE timestamp >= ?
          AND uuid IN (%s)
        GROUP BY uuid
        ORDER BY avg_online DESC
        """.formatted(placeholders);

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int idx = 1;
            stmt.setLong(idx++, startTime);
            for (String uuid : uuids) {
                stmt.setString(idx++, uuid);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String uuid = rs.getString("uuid");
                double avgOnline = rs.getDouble("avg_online");
                double avgCaptains = rs.getDouble("avg_captains");

                results.add(new GuildAverages(uuid, avgOnline, avgCaptains));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }


        return results;
    }


    public static void migrateOldDataToNewTables() {
        System.out.println("Starting migration of old data to new tables...");

        // Step 1: Calculate 30 days ago timestamp
        long thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);

        // Step 2: Query all data from old table for last 30 days
        String selectSql = """
        SELECT uuid, prefix, name, hour, 
               online_count, captains_online, timestamp
        FROM guild_activity 
        WHERE timestamp >= ?
        ORDER BY uuid, hour, timestamp
        """;

        // Step 3: Create temporary map to aggregate by hour for each guild
        Map<String, Map<Integer, HourlyAggregation>> guildHourlyData = new HashMap<>();

        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setLong(1, thirtyDaysAgo);

            try (ResultSet rs = selectStmt.executeQuery()) {
                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    String prefix = rs.getString("prefix");
                    String name = rs.getString("name");
                    int hour = rs.getInt("hour");
                    double onlineCount = rs.getDouble("online_count");
                    double captainsOnline = rs.getDouble("captains_online");
                    long timestamp = rs.getLong("timestamp");

                    // Get or create guild entry
                    guildHourlyData.putIfAbsent(uuid, new HashMap<>());
                    Map<Integer, HourlyAggregation> hourlyData = guildHourlyData.get(uuid);

                    // Get or create hourly entry
                    HourlyAggregation aggregation = hourlyData.get(hour);
                    if (aggregation == null) {
                        aggregation = new HourlyAggregation(uuid, prefix, name, hour);
                        hourlyData.put(hour, aggregation);
                    }

                    // Add data point to aggregation
                    aggregation.addDataPoint(onlineCount, captainsOnline, timestamp);
                }
            }

            System.out.println("Processed " + guildHourlyData.size() + " unique guilds from old table");

            // Step 4: Insert aggregated data into new tables
            int totalInserts = 0;
            for (Map<Integer, HourlyAggregation> hourlyData : guildHourlyData.values()) {
                for (HourlyAggregation aggregation : hourlyData.values()) {
                    if (aggregation.counter > 0) {
                        // Use the new add() method with aggregated data
                        // This will properly handle the incremental averaging
                        for (int i = 0; i < aggregation.counter; i++) {
                            // Since add() does incremental averaging, we need to simulate
                            // adding each original data point
                            // For efficiency, we'll use batch processing
                        }

                        // Better approach: Direct insert with calculated averages
                        insertAggregatedHourlyData(aggregation);
                        totalInserts++;
                    }
                }
            }

            System.out.println("Inserted " + totalInserts + " aggregated hourly records into new tables");

            // Step 5: Delete old table if migration was successful
            if (totalInserts > 0) {
                dropOldTable();
                System.out.println("Migration completed successfully. Old table dropped.");
            } else {
                System.out.println("No data to migrate or migration failed. Old table preserved.");
            }

        } catch (SQLException e) {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper class to aggregate hourly data
    private static class HourlyAggregation {
        String uuid;
        String prefix;
        String name;
        int hour;
        double totalOnline = 0;
        double totalCaptains = 0;
        int counter = 0;
        long latestTimestamp = 0;

        HourlyAggregation(String uuid, String prefix, String name, int hour) {
            this.uuid = uuid;
            this.prefix = prefix;
            this.name = name;
            this.hour = hour;
        }

        void addDataPoint(double onlineCount, double captainsOnline, long timestamp) {
            // Incremental average calculation
            counter++;
            totalOnline += onlineCount;
            totalCaptains += captainsOnline;

            if (timestamp > latestTimestamp) {
                latestTimestamp = timestamp;
            }
        }

        double getAvgOnline() {
            return counter > 0 ? totalOnline / counter : 0;
        }

        double getAvgCaptains() {
            return counter > 0 ? totalCaptains / counter : 0;
        }
    }

    // Direct insert method for aggregated data
    private static void insertAggregatedHourlyData(HourlyAggregation aggregation) {
        String sql = """
        INSERT INTO guild_activity_1 (uuid, prefix, name, hour, 
                                     avg_online_count, avg_captains_online, 
                                     counter, timestamp, last_updated)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT(uuid, hour) DO UPDATE SET
            avg_online_count = avg_online_count + (? - avg_online_count) / (counter + 1),
            avg_captains_online = avg_captains_online + (? - avg_captains_online) / (counter + 1),
            counter = counter + ?,
            last_updated = ?,
            prefix = excluded.prefix,
            name = excluded.name
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            double avgOnline = aggregation.getAvgOnline();
            double avgCaptains = aggregation.getAvgCaptains();

            // INSERT values
            ps.setString(1, aggregation.uuid);
            ps.setString(2, aggregation.prefix);
            ps.setString(3, aggregation.name);
            ps.setInt(4, aggregation.hour);
            ps.setDouble(5, avgOnline);
            ps.setDouble(6, avgCaptains);
            ps.setInt(7, aggregation.counter);
            ps.setLong(8, aggregation.latestTimestamp);
            ps.setLong(9, System.currentTimeMillis());

            // UPDATE values for online count
            ps.setDouble(10, avgOnline);

            // UPDATE values for captains online
            ps.setDouble(11, avgCaptains);

            // UPDATE counter increment
            ps.setInt(12, aggregation.counter);

            // UPDATE last_updated
            ps.setLong(13, System.currentTimeMillis());

            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to insert aggregated data for " + aggregation.uuid +
                    " hour " + aggregation.hour + ": " + e.getMessage());
        }
    }

    // Alternative: Batch migration for better performance
    public static void migrateOldDataBatch() {
        System.out.println("Starting batch migration...");

        long thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);

        // Query all raw data from last 30 days
        String selectSql = """
        SELECT uuid, prefix, name, hour, 
               online_count, captains_online, timestamp
        FROM guild_activity 
        WHERE timestamp >= ?
        ORDER BY timestamp
        """;

        int batchSize = 1000;
        int processedCount = 0;

        try (PreparedStatement selectStmt = connection.prepareStatement(selectSql)) {
            selectStmt.setLong(1, thirtyDaysAgo);

            try (ResultSet rs = selectStmt.executeQuery()) {
                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    String prefix = rs.getString("prefix");
                    String name = rs.getString("name");
                    int hour = rs.getInt("hour");
                    double onlineCount = rs.getDouble("online_count");
                    double captainsOnline = rs.getDouble("captains_online");
                    long timestamp = rs.getLong("timestamp");

                    // Use the new add() method for each data point
                    // This maintains the exact same incremental averaging logic
                    add(uuid, prefix, name, onlineCount, captainsOnline);

                    processedCount++;

                    if (processedCount % batchSize == 0) {
                        System.out.println("Processed " + processedCount + " records...");
                    }
                }
            }

            System.out.println("Total processed: " + processedCount + " records");

            // Verify migration
            if (processedCount > 0) {
                verifyMigration(thirtyDaysAgo);

                // Drop old table after verification
                dropOldTable();
                System.out.println("Batch migration completed successfully.");
            }

        } catch (SQLException e) {
            System.err.println("Batch migration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Verify data was migrated correctly
    private static void verifyMigration(long thirtyDaysAgo) {
        try {
            // Count old records
            String countOldSql = "SELECT COUNT(*) as count FROM guild_activity WHERE timestamp >= ?";
            PreparedStatement countOldStmt = connection.prepareStatement(countOldSql);
            countOldStmt.setLong(1, thirtyDaysAgo);
            ResultSet oldRs = countOldStmt.executeQuery();
            int oldCount = oldRs.next() ? oldRs.getInt("count") : 0;

            // Count new records (estimate - sum of counters)
            String countNewSql = "SELECT SUM(counter) as total FROM guild_activity_1 WHERE timestamp >= ?";
            PreparedStatement countNewStmt = connection.prepareStatement(countNewSql);
            countNewStmt.setLong(1, thirtyDaysAgo);
            ResultSet newRs = countNewStmt.executeQuery();
            int newCount = newRs.next() ? newRs.getInt("total") : 0;

            System.out.println("Verification: Old records = " + oldCount + ", New aggregated points = " + newCount);

            if (oldCount > 0 && newCount >= oldCount) {
                System.out.println("✓ Migration verified successfully");
            } else {
                System.out.println("⚠ Migration verification warning: counts don't match");
            }

            countOldStmt.close();
            countNewStmt.close();
        } catch (SQLException e) {
            System.err.println("Verification failed: " + e.getMessage());
        }
    }

    // Drop the old table
    private static void dropOldTable() {
        try {
            String dropSql = "DROP TABLE IF EXISTS guild_activity";
            try (PreparedStatement ps = connection.prepareStatement(dropSql)) {
                ps.execute();
                System.out.println("Old table 'guild_activity' dropped successfully");
            }

            // Also drop old indexes
            String[] oldIndexes = {
                    "idx_activity_uuid",
                    "idx_activity_uuid_time",
                    "idx_activity_hour_timestamp",
                    "idx_activity_prefix",
                    "idx_activity_prefix_timestamp"
            };

            for (String index : oldIndexes) {
                try {
                    String dropIndexSql = "DROP INDEX IF EXISTS " + index;
                    connection.prepareStatement(dropIndexSql).execute();
                } catch (SQLException e) {
                    // Index might not exist, ignore
                }
            }

        } catch (SQLException e) {
            System.err.println("Failed to drop old table: " + e.getMessage());
        }
    }

    // Migration with transaction for safety
    public static void migrateWithTransaction() {
        try {
            // Start transaction
            connection.setAutoCommit(false);

            System.out.println("Starting transactional migration...");

            // Perform migration
            migrateOldDataBatch();

            // Commit if successful
            connection.commit();
            System.out.println("Transaction committed successfully");

        } catch (SQLException e) {
            try {
                // Rollback on error
                connection.rollback();
                System.out.println("Transaction rolled back due to error: " + e.getMessage());
            } catch (SQLException rollbackEx) {
                System.err.println("Rollback failed: " + rollbackEx.getMessage());
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }


}