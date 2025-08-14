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

tracked guilds playtime as well as own
allow playtime command to specify interval and max

Get current memory usage

inactivity

every 90 seconds update the rank of everyone in your guild

fix formating for activeHours
add full guild name to tracked guild

format trackedguild to 2 decimals and sort it by activity and add a optional limit to the command

 */
