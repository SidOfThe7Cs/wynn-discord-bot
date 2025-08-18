package sidly.discord_bot.database.tables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static sidly.discord_bot.database.SQLDB.connection;

public class TrackedGuilds {
    public static void add(String prefix) {
        String sql = "INSERT OR IGNORE INTO tracked_guilds (uuid) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, prefix);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String> get() {
        List<String> guilds = new ArrayList<>();
        String sql = "SELECT uuid FROM tracked_guilds";
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                guilds.add(rs.getString("uuid"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return guilds;
    }

    public static boolean isTracked(String prefix) {
        String sql = "SELECT 1 FROM tracked_guilds WHERE uuid = ? LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, prefix);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void remove(String prefix) {
        String sql = "DELETE FROM tracked_guilds WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, prefix);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
