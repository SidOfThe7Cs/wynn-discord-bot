import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildName;

import java.io.IOException;
import java.util.Map;

public class TestEntrypoint {

    public static void main(String[] args) throws IOException {
        Map<String, GuildName> testApiResponce = ApiUtils.getAllGuildsList();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println("there are a total of: " + testApiResponce.entrySet().size() + " guilds");

    }
}
/*
TODO

tracked guilds playtime as well as own
allow playtime command to specify interval and max

Get current memory usage

inactivity

Get the time till rate limit refresh and then get the rate limit remaining and sutract 20 from it then send that many playersaya requests as quickly as possible
tracked guild show average players online and avg captains+

 */
