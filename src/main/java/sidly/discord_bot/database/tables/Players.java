package sidly.discord_bot.database.tables;

import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.SQLDB;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Players {
    public static void add(PlayerDataShortened player) {
        String sql = "INSERT OR REPLACE INTO players " +
                "(uuid, username, level, guildWars, latestPlaytime, lastModified, lastJoined, firstJoined, supportRank, highestLvl, wars) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = SQLDB.connection.prepareStatement(sql)) {
            pstmt.setString(2, player.uuid);
            pstmt.setString(1, player.username);
            pstmt.setInt(3, player.level);
            pstmt.setInt(4, player.guildWars);
            pstmt.setDouble(5, player.latestPlaytime);
            pstmt.setLong(6, player.lastModified);
            pstmt.setString(7, player.lastJoined);
            pstmt.setString(8, player.firstJoined);
            pstmt.setString(9, player.supportRank);
            pstmt.setInt(10, player.highestLvl);
            pstmt.setInt(11, player.wars);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error inserting player: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static PlayerDataShortened get(String uuid) {
        String sql = "SELECT * FROM players WHERE uuid = ?";

        try (PreparedStatement pstmt = SQLDB.connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                PlayerDataShortened player = new PlayerDataShortened();
                player.uuid = rs.getString("uuid");
                player.username = rs.getString("username");
                player.level = rs.getInt("level");
                player.guildWars = rs.getInt("guildWars");
                player.latestPlaytime = rs.getDouble("latestPlaytime");
                player.lastModified = rs.getLong("lastModified");
                player.lastJoined = rs.getString("lastJoined");
                player.firstJoined = rs.getString("firstJoined");
                player.supportRank = rs.getString("supportRank");
                player.highestLvl = rs.getInt("highestLvl");
                player.wars = rs.getInt("wars");
                return player;
            }
        } catch (SQLException e) {
            System.err.println("Error fetching player: " + e.getMessage());
            e.printStackTrace();
        }

        return null; // Not found
    }

}
