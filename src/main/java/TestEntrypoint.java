import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.api.ApiUtils;

public class TestEntrypoint {

    public static void main(String[] args) {
        GuildInfo testApiResponce = ApiUtils.getGuildInfo("HOC");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(testApiResponce));
    }
}
