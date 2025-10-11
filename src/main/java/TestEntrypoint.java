import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.database.PlaytimeHistoryList;
import sidly.discord_bot.database.SQLDB;
import sidly.discord_bot.database.tables.PlaytimeHistory;
import sidly.discord_bot.database.tables.UuidMap;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static sidly.discord_bot.database.SQLDB.connection;

public class TestEntrypoint {
    private static List<String> list = new ArrayList<>();

    public static void main(String[] args) throws SQLException {
        SQLDB.init();

        GuildInfo hoc = ApiUtils.getGuildInfo("HOC");
        System.out.println(hoc.name + " " + hoc.online + " / " + hoc.members.total);
    }

    public static void testPriont() {
        list.add("e");
        list.add("e");
        list.add("e");
        list.add("e");
        list.add("e");
        System.out.println("function run");
    }

    public static String getFirstUuidForHour(int hour) {
        String sql = "SELECT uuid FROM guild_activity WHERE hour = ? ORDER BY id ASC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, hour);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("uuid");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
/*
TODO
/leaderboard takes in any type from api

ppl who are not in the discord it fails to get the time this week in inactive players?
change playtim req for ranks

make a new table and have it store the average per hour for each guild and the number of entries in a way that lets you add more entries without messing up averages and only get the average over the past x days

maybe /getplaytimedata user that shows all playtime entries and how far apart they are
add /resstartalltimers

Server owner can always run commands

/delete old versions

/edit config cmd that sends a thing with components v2 maybe
have either pages or cmd parameters and have tokens as a seperate cmd

allow bot in multiple servers
 */
