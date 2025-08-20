import sidly.discord_bot.database.SQLDB;
import sidly.discord_bot.timed_actions.DynamicTimer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TestEntrypoint {
    private static List<String> list = new ArrayList<>();

    public static void main(String[] args) {
        //Map<String, GuildName> testApiResponce = ApiUtils.getAllGuildsList();

        //Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //System.out.println("there are a total of: " + testApiResponce.entrySet().size() + " guilds");

        //new DynamicTimer(list, TestEntrypoint::testPriont, 7000, 400).start();

        //UuidMap.put("testUser", "123456789");
        //System.out.println(UuidMap.get("testUser"));
    }

    public static void testPriont() {
        list.add("e");
        list.add("e");
        list.add("e");
        list.add("e");
        list.add("e");
        System.out.println("function run");
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

max page buttons store the sorted list probably in pageination stata

track if players are online or not
/guildstats prefix
figure out if last login is

 */
