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

update all players in your server a couple every 65 seconds
once all have been updated refresh list from discord (not cache) and repeat

tracked guilds playtime as well as own
Log there playtime that week
allow playtime command to specify interval and max

restart command
Get current memory usage

unverified role on join

when there not in a guild it crashes

 */
