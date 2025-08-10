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
update all players in your guild peryodically
Log there playtime that week
restart command
Get current memory usage

update player should remove roles too
limit commands to channel

 */
