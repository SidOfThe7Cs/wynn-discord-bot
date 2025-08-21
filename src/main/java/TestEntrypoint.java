import sidly.discord_bot.database.SQLDB;
import sidly.discord_bot.timed_actions.DynamicTimer;

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

        SQLDB.init();
        for (int i = 0; i < 24; i++) {
            String firstUuidForHour = getFirstUuidForHour(i);
            System.out.println(i + " " + firstUuidForHour);
        }
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

allow playtime command to specify interval and max

inactivity

Add exception cmds

Server owner can always run commands

guild playtime per hour averages need to be divided by the number of entries so the end average isnt skewed based on one hour having more entries than another

/delete old versions

cmd to check for ppl in guild not in discord (get list of all ppl in guild and the ppl they are verified too)

asynch MassGuilds player things

figure out why promotion page has worng max
add a possition for your guild to tracked guilds

 */
