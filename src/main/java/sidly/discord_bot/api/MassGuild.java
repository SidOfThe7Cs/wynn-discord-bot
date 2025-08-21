package sidly.discord_bot.api;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.records.GuildName;
import sidly.discord_bot.database.tables.AllGuilds;
import sidly.discord_bot.database.tables.GuildActivity;
import sidly.discord_bot.database.tables.Players;
import sidly.discord_bot.timed_actions.DynamicTimer;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MassGuild {
    private static final List<String> apiTokens = new ArrayList<>();
    private static final List<String> lowPriorityQueue = new ArrayList<>();
    private static final List<String> queue = new ArrayList<>();
    private static final List<String> tempHighPrioQueue = new ArrayList<>();
    private static int mainIndex = 0;
    private static int lowPrioIndex = 0;
    private static boolean isUpdating = false;
    private static boolean updateNext = true;
    private static HttpClient client;
    private static HttpClient client2;
    private static String multiselectorApiToken;
    private static DynamicTimer mainTimer;
    private static DynamicTimer lowPrioTimer;
    private static boolean timersRunning = false;
    private static List<String> handledMultis = new ArrayList<>();
    private static List<String> lowToHighMoveQueue = new ArrayList<>();
    private static List<String> highToLowMoveQueue = new ArrayList<>();

    private static int count = 0;
    private static int attempsCounter = 0;
    private static long startTime;
    private static List<String> statusCodes = new ArrayList<>();

    public static void init() {
        client = HttpClient.newHttpClient();
        client2 = HttpClient.newHttpClient();

        apiTokens.add(ConfigManager.getConfigInstance().other.get(Config.Settings.ApiToken1));
        apiTokens.add(ConfigManager.getConfigInstance().other.get(Config.Settings.ApiToken2));
        apiTokens.add(ConfigManager.getConfigInstance().other.get(Config.Settings.ApiToken3));
        apiTokens.add(ConfigManager.getConfigInstance().other.get(Config.Settings.ApiToken4));
        apiTokens.add(ConfigManager.getConfigInstance().other.get(Config.Settings.ApiToken5));
        apiTokens.add(ConfigManager.getConfigInstance().other.get(Config.Settings.ApiToken6));
        apiTokens.add(ConfigManager.getConfigInstance().other.get(Config.Settings.ApiToken7));

        multiselectorApiToken = (ConfigManager.getConfigInstance().other.get(Config.Settings.ApiToken8));

        queue.addAll(AllGuilds.getTracked(false));
        System.out.println("tracked guilds: " + queue.size());
        lowPriorityQueue.addAll(AllGuilds.getTracked(true));

        next();

        startTime = System.currentTimeMillis();

        // these timers are 7x for target time between because it does one call for every api token
        mainTimer = new DynamicTimer(queue, MassGuild::next, TimeUnit.MINUTES.toMillis(30), 600);
        lowPrioTimer = new DynamicTimer(lowPriorityQueue, MassGuild::nextLowPrio, TimeUnit.HOURS.toMillis(10), 6000);
        mainTimer.start();
        lowPrioTimer.start();
        timersRunning = true;

    }

    public static void stopTimer() {
        mainTimer.cancel();
        lowPrioTimer.cancel();
        timersRunning = true;
    }

    public static void startTimer() {
        if (!timersRunning) {
            mainTimer.start();
            lowPrioTimer.start();
            timersRunning = false;
        }
    }

    public static boolean getTimerStatus() {
        return timersRunning;
    }

    private static void nextLowPrio() {
        if (lowPriorityQueue.isEmpty()) return;
        for (int i = 0; i < apiTokens.size(); i++) {
            if (lowPrioIndex >= lowPriorityQueue.size()) lowPrioIndex = 0;
            updateFromApi(lowPriorityQueue.get(lowPrioIndex), i);
            lowPrioIndex++;
        }
    }

    public static void next() {

        if (updateNext && !isUpdating) {
            isUpdating = true;
            Map<String, GuildName> allGuildsList = ApiUtils.getAllGuildsList();
            AllGuilds.addGuilds(allGuildsList);
            queue.addAll(
                    allGuildsList.values().stream()
                            .map(GuildName::prefix)
                            .filter(prefix -> !queue.contains(prefix) && !lowPriorityQueue.contains(prefix))
                            .peek(prefix -> System.out.println("new guild!: " + prefix))
                            .toList()
            );
            isUpdating = false;
            updateNext = false;
        }

        for (int i = 0; i < apiTokens.size(); i++) {
            if (!tempHighPrioQueue.isEmpty()) {
                updateFromApi(tempHighPrioQueue.removeFirst(), i);
            }
            if (mainIndex >= queue.size()) {

                queue.removeAll(highToLowMoveQueue);
                lowPriorityQueue.addAll(highToLowMoveQueue);
                highToLowMoveQueue.clear();

                queue.addAll(lowToHighMoveQueue);
                lowPriorityQueue.removeAll(lowToHighMoveQueue);
                lowToHighMoveQueue.clear();

                /*
                System.out.println("queue finished looping size is: " + queue.size());
                long timePassed = System.currentTimeMillis() - startTime;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(timePassed);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(timePassed) % 60;
                System.out.println("in the last " + minutes + "m " + seconds + "s");
                System.out.println("resuests: " + attempsCounter + " responces: " + count);
                Map<String, Long> counts = statusCodes.stream().collect(Collectors.groupingBy(code -> code, Collectors.counting()));
                StringBuilder sb = new StringBuilder("codes: ");
                counts.forEach((code, count) -> sb.append(code).append(": ").append(count).append(" "));
                System.out.println(sb.toString().trim());
                 */

                handledMultis.clear();
                updateNext = true;
                mainIndex = 0;
            }
            if (!queue.isEmpty()) updateFromApi(queue.get(mainIndex), i);
            mainIndex++;
        }
    }

    private static void updateFromApi(String prefix, int index) {
        String apiToken = apiTokens.get(index);
        HttpRequest request;

        attempsCounter++;

        request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.wynncraft.com/v3/guild/prefix/" + prefix + "?identifier=uuid"))
                .header("Authorization", "Bearer " + apiToken)
                .GET()
                .build();
        CompletableFuture<HttpResponse<String>> httpResponseCompletableFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .orTimeout(30, TimeUnit.SECONDS);
        httpResponseCompletableFuture.thenAccept(response -> handleApiResponse(prefix, response)).exceptionally(ex -> {
            Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;

            if (cause instanceof TimeoutException) {
                statusCodes.add("timeout");
                tempHighPrioQueue.addFirst(prefix); // retry
            } else if (cause instanceof IOException && cause.getMessage().contains("GOAWAY")) {
                statusCodes.add("goaway");
                tempHighPrioQueue.addFirst(prefix); // retry
            } else {
                statusCodes.add("network/connection error");
                cause.printStackTrace();
            }
            return null;
        });
    }

    private static void handleApiResponse(String prefix, HttpResponse<String> response) {
        count++;

        //String reset = response.headers().map().getOrDefault("ratelimit-reset", List.of("unknown")).getFirst();
        //String limit = response.headers().map().getOrDefault("ratelimit-limit", List.of("unknown")).getFirst();
        //String remaining = response.headers().map().getOrDefault("ratelimit-remaining", List.of("unknown")).getFirst();

        int status = response.statusCode();
        statusCodes.add(String.valueOf(status));
        if (status == 404) {
            System.out.println("not found");
            return;
        }

        Gson gson = new GsonBuilder().create();

        if (status == 300) {
            if(!handledMultis.contains(prefix)) handleMultiselecters(response);
            handledMultis.add(prefix);
            return;
        }
        if (status == 429) { // rate limit
            tempHighPrioQueue.addFirst(prefix); // retry current
            return;
        }
        if (status == 520) {
            System.out.println("failed to connect to api (520) " + prefix);
            tempHighPrioQueue.addFirst(prefix); // retry current
            return;
        }
        if (status == 500) {
            System.out.println("failed to connect to api (500) " + prefix);
            return;
        }

        String body = response.body().trim();
        if (!(body.startsWith("{") || body.startsWith("["))) {
            System.out.println("status code: " + status);
            System.err.println("Unexpected response: " + body);
            return;
        }


        Type type = new TypeToken<GuildInfo>() {
        }.getType();
        GuildInfo apiData = gson.fromJson(response.body(), type);

        int onlinePlayerCount = 0;
        int onlineCaptainPlusCount = 0;
        GuildInfo.Members members = apiData.members;
        if (members != null) {
            onlinePlayerCount = members.getOnlineMembersCount();
            onlineCaptainPlusCount = members.getOnlineCaptainsPlusCount();

            if (members.total > 20) {
                // Track large guilds
                if (!queue.contains(prefix) && !lowToHighMoveQueue.contains(prefix)) {
                    lowToHighMoveQueue.add(prefix);
                    System.out.println("tracking guild " + prefix);
                }
                AllGuilds.addTracked(prefix, false);
            } else {
                // Untrack small guilds
                if (!lowPriorityQueue.contains(prefix) && !highToLowMoveQueue.contains(prefix)) {
                    highToLowMoveQueue.add(prefix);
                    System.out.println("un-tracking guild " + prefix);
                }
                AllGuilds.addTracked(prefix, true);
            }

        } else System.err.println("members was null " + prefix);

        GuildActivity.add(apiData.uuid, apiData.prefix, apiData.name, onlinePlayerCount, onlineCaptainPlusCount);
    }

    private static void handleMultiselecters(HttpResponse<String> response) {
        JsonObject objects = JsonParser.parseString(response.body()).getAsJsonObject();

        for (Map.Entry<String, JsonElement> entry : objects.entrySet()) {
            JsonObject guild = entry.getValue().getAsJsonObject();
            String name = guild.get("name").getAsString();
            String prefix = guild.get("prefix").getAsString();

            String encodedName = name.replace(" ", "%20");
            String url = "https://api.wynncraft.com/v3/guild/" + encodedName + "?identifier=uuid";

            HttpRequest request;
            attempsCounter++;
            request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + multiselectorApiToken)
                    .GET()
                    .build();
            CompletableFuture<HttpResponse<String>> httpResponseCompletableFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .orTimeout(30, TimeUnit.SECONDS);
            httpResponseCompletableFuture.thenAccept(responseName -> handleApiResponse(prefix, responseName)).exceptionally(ex -> {
                Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;

                if (cause instanceof TimeoutException) {
                    statusCodes.add("timeout");
                    tempHighPrioQueue.addFirst(prefix); // retry
                } else if (cause instanceof IOException && cause.getMessage().contains("GOAWAY")) {
                    statusCodes.add("goaway");
                    tempHighPrioQueue.addFirst(prefix); // retry
                } else {
                    statusCodes.add("network/connection error");
                    cause.printStackTrace();
                }
                return null;
            });
        }
    }

    public static Map<String, PlayerProfile> getPlayerData(Set<String> uuids) {
        if (apiTokens.isEmpty()) throw new IllegalStateException("No API tokens available");

        Gson gson = new GsonBuilder().create();
        Type type = new TypeToken<PlayerProfile>(){}.getType();
        Map<String, PlayerProfile> results = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        AtomicInteger tokenIndex = new AtomicInteger(0);

        for (String uuid : uuids) {
            String apiToken = apiTokens.get(tokenIndex.getAndIncrement() % apiTokens.size());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.wynncraft.com/v3/player/" + uuid + "?fullResult"))
                    .header("Authorization", "Bearer " + apiToken)
                    .GET()
                    .build();

            CompletableFuture<Void> future = client2.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        int status = response.statusCode();

                        if (status == 404) {
                            return; // no player found
                        }

                        String body = response.body().trim();
                        if (!(body.startsWith("{") || body.startsWith("["))) {
                            System.out.println("status code: " + status);
                            System.err.println("Unexpected response: " + body);
                            results.put(uuid, new PlayerProfile(response.statusCode()));
                            return;
                        }

                        PlayerProfile apiData = gson.fromJson(body, type);
                        if (apiData.username == null) return;

                        apiData.statusCode = status;
                        results.put(uuid, apiData);

                        PlayerDataShortened playerDataShortened = new PlayerDataShortened(apiData);
                        Players.add(playerDataShortened);
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });

            futures.add(future);
        }

        // Wait for all to finish
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return results;
    }


}
