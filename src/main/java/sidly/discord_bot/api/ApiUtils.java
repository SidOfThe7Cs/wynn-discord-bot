package sidly.discord_bot.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import sidly.discord_bot.ConfigManager;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiUtils {

    public static PlayerProfile getPlayerData(String username){
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.wynncraft.com/v3/player/" + username + "?fullResult"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse response with Gson
            Gson gson = new GsonBuilder().create();

            Type type = new TypeToken<PlayerProfile>(){}.getType();
            PlayerProfile apiData = gson.fromJson(response.body(), type);
            apiData.update();
            ConfigManager.getDatabaseInstance().allPlayers.put(apiData.username, apiData);

            if (apiData == null) {
                System.err.println("player profile was null");
                return null;
            }

            return apiData;

        } catch (IOException e) {
            System.err.println("IOException");
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            System.err.println("InterruptedException");
            throw new RuntimeException(e);
        }
    }

    public static GuildInfo getGuildInfo(String prefix){
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.wynncraft.com/v3/guild/prefix/"+prefix+"?identifier=uuid"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse response with Gson
            Gson gson = new GsonBuilder().create();

            Type type = new TypeToken<GuildInfo>(){}.getType();
            GuildInfo apiData = gson.fromJson(response.body(), type);
            apiData.update();
            ConfigManager.getDatabaseInstance().allGuilds.put(apiData.prefix, apiData);

            return apiData;

        } catch (IOException e) {
            System.err.println("IOException");
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            System.err.println("InterruptedException");
            throw new RuntimeException(e);
        }
    }
}
