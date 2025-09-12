package sidly.discord_bot.database.tables;

import sidly.discord_bot.Utils;
import sidly.discord_bot.database.records.GuildAverages;
import sidly.discord_bot.database.records.TimestampedDouble;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static sidly.discord_bot.database.SQLDB.connection;

public class GuildActivity {
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

    public static List<GuildAverages> getGuildAverages(int daysInPast) {
        List<GuildAverages> results = new ArrayList<>();



        long startTime = 0;
        if (daysInPast > 0) {
            startTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(daysInPast);
        }

        String sql = """
                SELECT sub.uuid,
                               AVG(sub.hourly_avg_online)   AS avg_online,
                               AVG(sub.hourly_avg_captains) AS avg_captains
                        FROM (
                            SELECT a.uuid,
                                   a.hour,
                                   AVG(a.online_count)    AS hourly_avg_online,
                                   AVG(a.captains_online) AS hourly_avg_captains
                            FROM guild_activity a
                            JOIN guilds_40_plus g
                              ON a.prefix = g.prefix
                            WHERE a.timestamp >= ?
                              AND g.low_priority = 0
                            GROUP BY a.uuid, a.hour
                        ) sub
                        GROUP BY sub.uuid
                        ORDER BY avg_online DESC;
        """;

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setLong(1, startTime);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String uuid = rs.getString("uuid");
                    double avgOnline = rs.getDouble("avg_online");
                    double avgCaptains = rs.getDouble("avg_captains");

                    results.add(new GuildAverages(uuid, avgOnline, avgCaptains));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return results;
    }


}
