import sidly.discord_bot.database.SQLDB;

import java.sql.SQLException;

public class TestEntrypoint {

    public static void main(String[] args) {
        //Map<String, GuildName> testApiResponce = ApiUtils.getAllGuildsList();

        //Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //System.out.println("there are a total of: " + testApiResponce.entrySet().size() + " guilds");

        try {
            SQLDB.init();
        } catch (SQLException e) {
            System.out.println("failed to connect to database");
            e.printStackTrace();
        }

        //UuidMap.put("testUser", "123456789");
        //System.out.println(UuidMap.get("testUser"));
    }
}
/*
TODO

allow playtime command to specify interval and max

inactivity

Add exception cmds

database and 6 api token and track all guilds

When i track all guilds sort then less often
Server owner can always run commands

test guildrank updater

guild playtime per hour averages need to be divided by the number of entries so the end average isnt skewed based on one hour having more entries than another

 */
