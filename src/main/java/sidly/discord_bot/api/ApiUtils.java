package sidly.discord_bot.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.database.GuildDataActivity;
import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.PlaytimeHistoryList;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiUtils {

    public static Map<RateLimitTypes, RateLimitInfo> rateLimitInfoMap = new HashMap<>();

    public static long getLastRateLimitUpdate() {
        return lastRateLimitUpdate;
    }

    private static long lastRateLimitUpdate = 0;

    public static PlayerProfile getPlayerData(String username){
        String apiToken = ConfigManager.getConfigInstance().other.get(Config.Settings.ApiToken);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request;
            if (apiToken == null || apiToken.isEmpty()) {
                request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.wynncraft.com/v3/player/" + username + "?fullResult"))
                        .GET()
                        .build();
            } else {
                request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.wynncraft.com/v3/player/" + username + "?fullResult"))
                        .header("Authorization", "Bearer " + apiToken)
                        .GET()
                        .build();
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 404) {
                return null;
            }

            String body = response.body().trim();
            if (!(body.startsWith("{") || body.startsWith("["))) {
                System.out.println("status code: " + status);
                System.err.println("Unexpected response: " + body);
                return null;
            }
            parseRateLimit(response);

            Gson gson = new GsonBuilder().create();


            if (status == 300) {
                JsonObject objects = gson.fromJson(response.body(), JsonObject.class).getAsJsonObject("objects");
                Type mapType = new TypeToken<LinkedHashMap<String, PlayerProfile>>(){}.getType();
                LinkedHashMap<String, PlayerProfile> playersMap = gson.fromJson(objects, mapType);
                return new PlayerProfile(playersMap);
            }


            Type type = new TypeToken<PlayerProfile>(){}.getType();
            PlayerProfile apiData = gson.fromJson(response.body(), type);
            if (apiData.username == null) return null;

            PlayerDataShortened playerDataShort = new PlayerDataShortened(apiData);
            ConfigManager.getDatabaseInstance().allPlayers.put(apiData.username, playerDataShort);
            Map<String, PlaytimeHistoryList> playtimeHistoryMap = ConfigManager.getDatabaseInstance().playtimeHistory;
            if (playtimeHistoryMap.get(apiData.username) == null){
                playtimeHistoryMap.put(apiData.username, new PlaytimeHistoryList());
            }
            playtimeHistoryMap.get(apiData.username).addPlaytimeIfNeeded(playerDataShort);
            ConfigManager.saveDatabase();

            return apiData;

        } catch (IOException e) {
            System.err.println("IOException: " + username);
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            System.err.println("InterruptedException");
            throw new RuntimeException(e);
        }
    }

    public static GuildInfo getGuildInfo(String prefix){
        String apiToken = ConfigManager.getConfigInstance().other.get(Config.Settings.ApiToken);
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request;
            if (apiToken == null || apiToken.isEmpty()) {
                request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.wynncraft.com/v3/guild/prefix/"+prefix+"?identifier=uuid"))
                    .GET()
                    .build();
            } else {
                request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.wynncraft.com/v3/guild/prefix/"+prefix+"?identifier=uuid"))
                        .header("Authorization", "Bearer " + apiToken)
                        .GET()
                        .build();
            }
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status == 404) {
                return null;
            }
            if (status == 300) System.out.println("a multiselector was actually returned and i havent added handling for it " + prefix);
            parseRateLimit(response);

            // Parse response with Gson
            Gson gson = new GsonBuilder().create();

            Type type = new TypeToken<GuildInfo>(){}.getType();
            GuildInfo apiData = gson.fromJson(response.body(), type);

            Map<String, GuildDataActivity> trackedGuildActivity = ConfigManager.getDatabaseInstance().trackedGuildActivity;
            if (trackedGuildActivity.get(prefix) == null) trackedGuildActivity.put(prefix, new GuildDataActivity(apiData.name));
            int onlinePlayerCount = 0;
            int onlineCaptainPlusCount = 0;
            GuildInfo.Members members = apiData.members;
            if (members != null) {
                onlinePlayerCount = members.getOnlineMembersCount();
                onlineCaptainPlusCount = members.getOnlineCaptainsPlusCount();
            }
            trackedGuildActivity.get(prefix).add(onlinePlayerCount, false);
            trackedGuildActivity.get(prefix).add(onlineCaptainPlusCount, true);

            if (ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix).equals(prefix)){
                ConfigManager.getDatabaseInstance().yourGuildInfo = apiData;
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

    public static Map<String, GuildName> getAllGuildsList() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.wynncraft.com/v3/guild/list/guild"))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            parseRateLimit(response);

            // Parse response with Gson
            Gson gson = new GsonBuilder().create();

            Type type = new TypeToken<Map<String, GuildName>>(){}.getType();
            Map<String, GuildName> apiData = gson.fromJson(response.body(), type);

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

        int rateLimitRemaining = -1;
        int rateLimitSecondsTillReset = -1;
        int rateLimitMax = -1;

        Map<String, List<String>> headers = response.headers().map();

        String remaining = headers.getOrDefault("ratelimit-remaining", List.of("unknown")).getFirst();
        String reset     = headers.getOrDefault("ratelimit-reset", List.of("unknown")).getFirst();
        String limit     = headers.getOrDefault("ratelimit-limit", List.of("unknown")).getFirst();
        String bucket     = headers.getOrDefault("Ratelimit-Bucket", List.of("UNKNOWN")).getFirst();
        String userId     = headers.getOrDefault("UserID", List.of("unknown")).getFirst();

        if (!remaining.equals("unknown")) {
            rateLimitRemaining = Integer.parseInt(remaining);
        }
        if (!reset.equals("unknown")) {
            rateLimitSecondsTillReset = Integer.parseInt(reset);
        }
        if (!limit.equals("unknown")) {
            rateLimitMax = Integer.parseInt(limit);
        }
        RateLimitTypes type = RateLimitTypes.valueOf(bucket);
        lastRateLimitUpdate = System.currentTimeMillis();

        rateLimitInfoMap.put(type, new RateLimitInfo(rateLimitRemaining, rateLimitSecondsTillReset, rateLimitMax, lastRateLimitUpdate));
    }

    public record RateLimitInfo(int rateLimitRemaining, int rateLimitSecondsTillReset, int rateLimitMax, long lastUpdated) {}

    public enum RateLimitTypes {
        SHARED,
        PLAYER,
        GUILD,
        ITEMS,
        LEADERBOARDS,
        MAP,
        UNKNOWN
    }
}
