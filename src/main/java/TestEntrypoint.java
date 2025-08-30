import sidly.discord_bot.Utils;
import sidly.discord_bot.database.SQLDB;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static sidly.discord_bot.database.SQLDB.connection;

public class TestEntrypoint {
    private static List<String> list = new ArrayList<>();

    public static void main(String[] args) throws SQLException {
        //Map<String, GuildName> testApiResponce = ApiUtils.getAllGuildsList();

        //Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //System.out.println("there are a total of: " + testApiResponce.entrySet().size() + " guilds");

        //new DynamicTimer(list, TestEntrypoint::testPriont, 7000, 400).start();

        //UuidMap.put("testUser", "123456789");
        //System.out.println(UuidMap.get("testUser"));

        System.out.println(Utils.getTotalGuildExperience(93, 1));
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

Add exception cmds

Server owner can always run commands

/delete old versions

Active hours and tracked guild should only be last 4 weeks

/lastlogins Takes in 1 input which is a guild identifier (name or prefix) and will return a list of all the members of that guild ordered by how long it has been since they last logged in. It also shows their guild rank and highest character level

/updateallplayers

active hours uses code block if sent from mobile




/edit config cmd that sends a thing with components v2 maybe
have either pages or cmd parameters and have tokens as a seperate cmd
 */
