package sidly.discord_bot.database.tables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static sidly.discord_bot.database.SQLDB.connection;

public class PromotionExceptions {

    public static Map<String, Long> getAll() {
        Map<String, Long> results = new HashMap<>();
        String sql = "SELECT uuid, exp_timestamp FROM promotion_exceptions";
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                results.put(rs.getString("uuid"), rs.getLong("exp_timestamp"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public static Long get(String uuid) {
        String sql = "SELECT exp_timestamp FROM promotion_exceptions WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getLong("exp_timestamp");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void add(String uuid, long expTimestamp) {
        String sql = "INSERT OR REPLACE INTO promotion_exceptions (uuid, exp_timestamp) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid);
            stmt.setLong(2, expTimestamp);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
