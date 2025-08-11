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
import java.util.List;
import java.util.Map;

public class ApiUtils {

    public static int getRateLimitRemaining() {
        return rateLimitRemaining;
    }

    public static int getRateLimitSecondsTillReset() {
        return rateLimitSecondsTillReset;
    }

    public static int getRateLimitMax() {
        return rateLimitMax;
    }

    private static int rateLimitRemaining = -1;
    private static int rateLimitSecondsTillReset = -1;
    private static int rateLimitMax = -1;

    public static long getLastRateLimitUpdate() {
        return lastRateLimitUpdate;
    }

    private static long lastRateLimitUpdate = 0;

    public static PlayerProfile getPlayerData(String username){
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.wynncraft.com/v3/player/" + username + "?fullResult"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            parseRateLimit(response);

            // Parse response with Gson
            Gson gson = new GsonBuilder().create();

            Type type = new TypeToken<PlayerProfile>(){}.getType();
            PlayerProfile apiData = gson.fromJson(response.body(), type);
            apiData.update();

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
            parseRateLimit(response);

            // Parse response with Gson
            Gson gson = new GsonBuilder().create();

            Type type = new TypeToken<GuildInfo>(){}.getType();
            GuildInfo apiData = gson.fromJson(response.body(), type);
            apiData.update();

            return apiData;

        } catch (IOException e) {
            System.err.println("IOException");
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            System.err.println("InterruptedException");
            throw new RuntimeException(e);
        }
    }

    private static void parseRateLimit(HttpResponse<String> response){
        Map<String, List<String>> headers = response.headers().map();

        String remaining = headers.getOrDefault("ratelimit-remaining", List.of("unknown")).getFirst();
        String reset     = headers.getOrDefault("ratelimit-reset", List.of("unknown")).getFirst();
        String limit     = headers.getOrDefault("ratelimit-limit", List.of("unknown")).getFirst();

        rateLimitRemaining = Integer.parseInt(remaining);
        rateLimitSecondsTillReset = Integer.parseInt(reset);
        rateLimitMax = Integer.parseInt(limit);
        lastRateLimitUpdate = System.currentTimeMillis();
    }
}
