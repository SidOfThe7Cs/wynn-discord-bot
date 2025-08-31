package sidly.discord_bot.database.tables;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static sidly.discord_bot.database.SQLDB.connection;

public class UuidMap {
    public static void addDiscordId(String username, String discordId) {
        try (PreparedStatement insertStmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO uuidMap (username) VALUES (?)")) {
            insertStmt.setString(1, username.toLowerCase());
            insertStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (PreparedStatement updateStmt = connection.prepareStatement(
                "UPDATE uuidMap SET discord_id = ? WHERE username = ?")) {
            updateStmt.setString(1, discordId);
            updateStmt.setString(2, username);
            updateStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addMinecraftId(String username, String minecraftId) {
        try (PreparedStatement insertStmt = connection.prepareStatement(
                "INSERT OR IGNORE INTO uuidMap (username) VALUES (?)")) {
            insertStmt.setString(1, username.toLowerCase());
            insertStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try (PreparedStatement updateStmt = connection.prepareStatement(
                "UPDATE uuidMap SET minecraft_id = ? WHERE username = ?")) {
            updateStmt.setString(1, minecraftId);
            updateStmt.setString(2, username);
            updateStmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getDiscordIdByUsername(String username) {
        String sql = "SELECT discord_id FROM uuidMap WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username.toLowerCase());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("discord_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // not found
    }

    public static String getMinecraftIdByUsername(String username) {
        String sql = "SELECT minecraft_id FROM uuidMap WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username.toLowerCase());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("minecraft_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // not found
    }

    public static String getDiscordIdByMinecraftId(String minecraftId) {
        String sql = "SELECT discord_id FROM uuidMap WHERE minecraft_id = ? LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, minecraftId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("discord_id");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // not found
    }


    public static String getUsernameByMinecraftId(String minecraftId) {
        String sql = "SELECT username FROM uuidMap WHERE minecraft_id = ? LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, minecraftId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Return null if no match found
    }

    public static boolean containsMinecraftId(String minecraftId) {
        String sql = "SELECT 1 FROM uuidMap WHERE minecraft_id = ? LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, minecraftId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // true if a row exists
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false; // Return false if no match or error
    }

    public static void remove(String username) {
        String sql = "DELETE FROM uuidMap WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
