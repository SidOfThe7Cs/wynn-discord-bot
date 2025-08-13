import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.api.ApiUtils;

import java.io.IOException;

public class TestEntrypoint {

    public static void main(String[] args) throws IOException {
        GuildInfo testApiResponce = ApiUtils.getGuildInfo("HOC");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(testApiResponce));

    }
}
/*
TODO

tracked guilds playtime as well as own
get guild and online players if there online track playtime
Log there playtime that week
allow playtime command to specify interval and max

Get current memory usage

mod admin content team role

inactivity

Get the time till rate limit refresh and then get the rate limit remaining and sutract 20 from it then send that many playersaya requests as quickly as possible
If its been over 7 days log there playtime

 */
