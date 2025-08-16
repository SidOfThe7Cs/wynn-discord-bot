import sidly.discord_bot.Utils;

import java.io.IOException;

public class TestEntrypoint {

    public static void main(String[] args) throws IOException {
        //Map<String, GuildName> testApiResponce = ApiUtils.getAllGuildsList();

        //Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //System.out.println("there are a total of: " + testApiResponce.entrySet().size() + " guilds");

        System.out.println(Utils.getHoursSinceDayStarted(System.currentTimeMillis()));
    }
}
/*
TODO

allow playtime command to specify interval and max

inactivity

skey weekly playtime to be acurate instead of just entries

Addeception cmds
Backup database
Get all timer average weekly playtime
Make cmd that shows a players average playtime over the last week / 5 weeks / 20 weeks / alltime

database and 6 api token and track all guilds

 */
