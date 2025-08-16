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

inactivity / promotion / demotion

timer commands and status


database and 6 api token and track all guilds

 */
