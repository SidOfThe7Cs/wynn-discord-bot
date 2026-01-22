package sidly.discord_bot.api;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.commands.GuildCommands;
import sidly.discord_bot.commands.VerificationCommands;
import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.records.GuildName;
import sidly.discord_bot.database.tables.*;
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
import java.util.stream.Collectors;

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

    public static ApiUtils.RateLimitInfo guildRateLimitInfo;

    private static int count = 0;
    private static int attempsCounter = 0;
    private static long startTime;

    public static String getOverfullQueues() {
        return tempHighPrioQueue.size() > 200 ? "to many guilds are failing and its infinitely cycling the same errors" : "";
    }

    private static final Object LOCK = new Object();

    private static ConcurrentSkipListMap<Integer, Set<String>> sizeToPrefixes =
            new ConcurrentSkipListMap<>(Comparator.reverseOrder());

    public static void init() {
        client = HttpClient.newHttpClient();
        client2 = HttpClient.newHttpClient();

        for (Config.Settings tokenSetting : new Config.Settings[]{
                Config.Settings.ApiToken1,
                Config.Settings.ApiToken2,
                Config.Settings.ApiToken3,
                Config.Settings.ApiToken4,
                Config.Settings.ApiToken5,
                Config.Settings.ApiToken6,
                Config.Settings.ApiToken7
        }) {
            String token = ConfigManager.getConfigInstance().other.get(tokenSetting);
            if (token != null && !token.isEmpty()) {
                apiTokens.add(token);
            }
        }

        multiselectorApiToken = (ConfigManager.getConfigInstance().other.get(Config.Settings.ApiToken8));

        Map<String, Integer> tracked = AllGuilds.getTracked(false);
        queue.addAll(tracked.keySet());
        for (Map.Entry<String, Integer> entry : tracked.entrySet()) {
            String prefix = entry.getKey();
            Integer memberCount = entry.getValue();
            if (memberCount != null) {
                sizeToPrefixes.computeIfAbsent(memberCount,
                        k -> ConcurrentHashMap.newKeySet()).add(prefix);
            }
        }
        System.out.println("tracked guilds: " + queue.size() + " should be same: " +
                sizeToPrefixes.values().stream().mapToInt(Set::size).sum());

        lowPriorityQueue.addAll(AllGuilds.getTracked(true).keySet());
        cleanQueue(ApiUtils.getAllGuildsList());

        next();

        startTime = System.currentTimeMillis();

        // these timers are 7x for target time between because it does one call for every api token
        mainTimer = new DynamicTimer(queue, MassGuild::next, TimeUnit.MINUTES.toMillis(35), 600);
        lowPrioTimer = new DynamicTimer(lowPriorityQueue, MassGuild::nextLowPrio, TimeUnit.HOURS.toMillis(10), 6000);
        startTimer();

    }

    public static void stopTimer() {
        mainTimer.cancel();
        lowPrioTimer.cancel();
        timersRunning = false;
    }

    public static void startTimer() {
        if (!timersRunning) {
            mainTimer.start();
            lowPrioTimer.start();
            timersRunning = true;
        }
    }

    public static boolean getTimerStatus() {
        return timersRunning;
    }
    public static Long getTimerLastRun() {
        return Math.min(lowPrioTimer.getLastTimeRan(), mainTimer.getLastTimeRan());
    }

    private static void nextLowPrio() {
        if (lowPriorityQueue.isEmpty()) return;
        for (int i = 0; i < apiTokens.size(); i++) {
            if (lowPrioIndex >= lowPriorityQueue.size()) lowPrioIndex = 0;
            updateFromApi(lowPriorityQueue.get(lowPrioIndex), i);
            lowPrioIndex++;
        }
    }

    public static void cleanQueue(Map<String, GuildName> guildMap) {
        // Collect all valid prefixes
        Set<String> validPrefixes = new HashSet<>();
        for (GuildName guild : guildMap.values()) {
            if (guild.prefix() != null) {
                validPrefixes.add(guild.prefix());
            }
        }

        // For main queue
        List<String> removedFromQueue = new ArrayList<>();
        queue.removeIf(id -> {
            if (!validPrefixes.contains(id)) {
                removedFromQueue.add(id);
                return true;
            }
            return false;
        });

        // For low-priority queue
        List<String> removedFromLowPriority = new ArrayList<>();
        lowPriorityQueue.removeIf(id -> {
            if (!validPrefixes.contains(id)) {
                removedFromLowPriority.add(id);
                return true;
            }
            return false;
        });

        // Combine removed items and print
        List<String> allRemoved = new ArrayList<>();
        allRemoved.addAll(removedFromQueue);
        allRemoved.addAll(removedFromLowPriority);

        if (!allRemoved.isEmpty()) {
            //System.out.println("Deleted Guilds: " + allRemoved);
            for (String prefix : allRemoved) {
                AllGuilds.unTracked(prefix);
            }
        }
    }



    public static void next() {

        if (updateNext && !isUpdating) {
            isUpdating = true;
            Map<String, GuildName> allGuildsList = ApiUtils.getAllGuildsList();
            cleanQueue(allGuildsList);
            AllGuilds.addGuilds(allGuildsList);
            queue.addAll(
                    allGuildsList.values().stream()
                            .map(GuildName::prefix)
                            .filter(prefix -> !queue.contains(prefix) && !lowPriorityQueue.contains(prefix))
                            //.peek(prefix -> System.out.println("new guild!: " + prefix))
                            .toList()
            );
            isUpdating = false;
            updateNext = false;
        }

        for (int i = 0; i < apiTokens.size(); i++) {
            if (!tempHighPrioQueue.isEmpty()) {
                updateFromApi(tempHighPrioQueue.removeFirst(), i);
                continue;
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
        //System.out.println(attempsCounter);

        request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.wynncraft.com/v3/guild/prefix/" + prefix + "?identifier=uuid"))
                .header("Authorization", "Bearer " + apiToken)
                .GET()
                .build();
        CompletableFuture<HttpResponse<String>> httpResponseCompletableFuture = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .orTimeout(30, TimeUnit.SECONDS);
        httpResponseCompletableFuture.thenAccept(response -> handleApiResponse(prefix, response)).exceptionally(ex -> {
            Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;

            if (cause instanceof java.net.SocketException) {
                tempHighPrioQueue.addFirst(prefix); // retry
            } else if (cause instanceof javax.net.ssl.SSLHandshakeException) {
                tempHighPrioQueue.addFirst(prefix); // retry
            } else if (cause instanceof TimeoutException) {
                tempHighPrioQueue.addFirst(prefix); // retry
            } else if ("too many concurrent streams".equals(cause.getMessage())) {
                tempHighPrioQueue.addFirst(prefix); // retry
            } else if (cause instanceof IOException && cause.getMessage().contains("GOAWAY")) {
                tempHighPrioQueue.addFirst(prefix); // retry
            } else if (cause instanceof IOException && cause.getMessage().contains("Connection reset")) {
                tempHighPrioQueue.addFirst(prefix); // retry
            } else {
                cause.printStackTrace();
            }
            return null;
        });
    }

    private static void handleApiResponse(String prefix, HttpResponse<String> response) {
        count++;
        //System.out.println(count);

        //String reset = response.headers().map().getOrDefault("ratelimit-reset", List.of("unknown")).getFirst();
        //String limit = response.headers().map().getOrDefault("ratelimit-limit", List.of("unknown")).getFirst();
        //String remaining = response.headers().map().getOrDefault("ratelimit-remaining", List.of("unknown")).getFirst();

        //System.out.println(remaining + "/" + limit + " " + reset);

        int status = response.statusCode();
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
            System.out.println("ratelimited (did you get temp banned from api?)");
            tempHighPrioQueue.addFirst(prefix); // retry current
            return;
        }
        if (status == 520) {
            tempHighPrioQueue.addFirst(prefix); // retry current
            return;
        }
        if (status == 500) {
            System.out.println("is this is being spammed api is probably down" + prefix);
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

        int onlineCaptainPlusCount;
        GuildInfo.Members members = apiData.members;
        if (members != null) {
            synchronized (LOCK) {
                onlineCaptainPlusCount = members.getOnlineCaptainsPlusCount();
                GuildActivity.add(apiData.uuid, apiData.prefix, apiData.name, apiData.online, onlineCaptainPlusCount);

                int size = sizeToPrefixes.values().stream().mapToInt(Set::size).sum();
                int memberCount = members.total;

                // check for duplicates
                Integer existingSize = findExistingSize(prefix);
                if (existingSize != null) {
                    if (existingSize == memberCount) {
                        trackGuildIfNot(prefix, memberCount);
                        return; // Already tracked with same size
                    } else {
                        trackGuildIfNot(prefix, memberCount);
                        moveSize(prefix, existingSize, memberCount);
                        return;
                    }
                }

                if (size < 300) {
                    trackGuildIfNot(prefix, memberCount);
                    sizeToPrefixes.computeIfAbsent(memberCount, k -> ConcurrentHashMap.newKeySet()).add(prefix);
                    return;
                }

                Map.Entry<Integer, Set<String>> smallestPrefixes = getSmallest();
                if (memberCount > smallestPrefixes.getKey()) {
                    trackGuildIfNot(prefix, memberCount);
                    sizeToPrefixes.computeIfAbsent(memberCount, k -> ConcurrentHashMap.newKeySet()).add(prefix);
                } else {
                    unTrackGuildIfTracked(prefix, memberCount);
                    return;
                }

                if (size > 300) {
                    int count = size - 300;
                    removeSmallest(count, memberCount);
                }
            }
        } else {
            tempHighPrioQueue.add(prefix);
        }
    }

    private static Integer findExistingSize(String prefix) {
        for (Map.Entry<Integer, Set<String>> entry : sizeToPrefixes.entrySet()) {
            if (entry.getValue().contains(prefix)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static void moveSize(String prefix, Integer existingSize, Integer newSize) {
        if (existingSize != null && newSize != null) {
            Set<String> bucket = sizeToPrefixes.get(existingSize);
            if (bucket != null) {
                bucket.remove(prefix);
                sizeToPrefixes.computeIfAbsent(newSize, k -> ConcurrentHashMap.newKeySet()).add(prefix);
                if (bucket.isEmpty()) {
                    sizeToPrefixes.remove(existingSize);
                }
                return;
            }
        }
        System.out.println("failed to move " + prefix + " from " + existingSize + " to " + newSize);
    }

    private static void removeSmallest(int count, int memberCount) {
        if (count <= 0) return;
        Map.Entry<Integer, Set<String>> smallest = getSmallest();
        int size = smallest.getValue().size();
        if (size <= count) {
            Set<String> prefixes = new HashSet<>(smallest.getValue());
            for (String prefix : prefixes) {
                unTrackGuildIfTracked(prefix, memberCount);
            }
            sizeToPrefixes.remove(smallest.getKey());
            removeSmallest(count - size, memberCount);
        } else { // need to remove less than are in the set
            int removedCounter = 0;
            Iterator<String> iterator = smallest.getValue().iterator();
            while (iterator.hasNext() && removedCounter < count) {
                String prefix = iterator.next();
                unTrackGuildIfTracked(prefix, memberCount);
                iterator.remove();
                removedCounter++;
            }
        }
    }

    private static Map.Entry<Integer, Set<String>> getSmallest() {
        Map.Entry<Integer, Set<String>> entry = sizeToPrefixes.lastEntry();
        if (entry.getValue().isEmpty()) {
            sizeToPrefixes.remove(entry.getKey()); // dont need an empty set
            return getSmallest();
        } else return entry;
    }

    private static void trackGuildIfNot(String prefix, int memberCount) {
        if (!AllGuilds.isTracked(prefix)) {
            int size = sizeToPrefixes.values().stream().mapToInt(Set::size).sum();
            AllGuilds.addTracked(prefix, false, memberCount);
            System.out.println("tracking guild " + prefix + " there are now " + size);
            if (!queue.contains(prefix) && !lowToHighMoveQueue.contains(prefix)) {
                lowToHighMoveQueue.add(prefix);
            }
        }
    }

    private static void unTrackGuildIfTracked(String prefix, int memberCount) {
        if (AllGuilds.isTracked(prefix)) {
            int size = sizeToPrefixes.values().stream().mapToInt(Set::size).sum();
            AllGuilds.addTracked(prefix, true, memberCount);
            System.out.println("un-tracking guild " + prefix + " there are now " + size);
            if (!lowPriorityQueue.contains(prefix) && !highToLowMoveQueue.contains(prefix)) {
                highToLowMoveQueue.add(prefix);
            }
        }
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
                if (cause instanceof java.net.SocketException) {
                    tempHighPrioQueue.addFirst(prefix); // retry
                } else if (cause instanceof javax.net.ssl.SSLHandshakeException) {
                    tempHighPrioQueue.addFirst(prefix); // retry
                } else if ("too many concurrent streams".equals(cause.getMessage())) {
                    tempHighPrioQueue.addFirst(prefix); // retry
                } else if (cause instanceof TimeoutException) {
                    tempHighPrioQueue.addFirst(prefix); // retry
                } else if (cause instanceof IOException && cause.getMessage().contains("GOAWAY")) {
                    tempHighPrioQueue.addFirst(prefix); // retry
                } else if (cause instanceof IOException && cause.getMessage().contains("Connection reset")) {
                    tempHighPrioQueue.addFirst(prefix); // retry
                } else {
                    cause.printStackTrace();
                }
                return null;
            });
        }
    }

    private static final AtomicInteger tokenIndex = new AtomicInteger(0);
    public static Map<String, PlayerProfile> getPlayerData(Set<String> uuids, String guildUuid) {
        if (apiTokens.isEmpty()) throw new IllegalStateException("No API tokens available");

        // to close to ratelimit
        if (guildRateLimitInfo != null && guildRateLimitInfo.getRemaining() < 20) {
            Map<String, PlayerProfile> errorReply = new HashMap<>();
            errorReply.put(null, new PlayerProfile(429));
            return errorReply;
        }

        Gson gson = new GsonBuilder().create();
        Type type = new TypeToken<PlayerProfile>(){}.getType();
        Map<String, PlayerProfile> results = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        Set<PlayerDataShortened> toUpdate = new HashSet<>();

        for (String uuid : uuids) {
            String apiToken = apiTokens.get(tokenIndex.getAndIncrement() % apiTokens.size());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.wynncraft.com/v3/player/" + uuid + "?fullResult"))
                    .header("Authorization", "Bearer " + apiToken)
                    .GET()
                    .build();

            CompletableFuture<Void> future = client2.sendAsync(request, HttpResponse.BodyHandlers.ofString()).orTimeout(30, TimeUnit.SECONDS)
                    .thenAccept(response -> {
                        int status = response.statusCode();

                        String remaining = response.headers().map().getOrDefault("ratelimit-remaining", List.of("unknown")).getFirst();
                        String reset     = response.headers().map().getOrDefault("ratelimit-reset", List.of("unknown")).getFirst();
                        String limit     = response.headers().map().getOrDefault("ratelimit-limit", List.of("unknown")).getFirst();

                        if (Objects.equals(limit, "unknown") || Objects.equals(reset, "unknown") || Objects.equals(remaining, "unknown")) {
                            List<String> invalidTokens = ConfigManager.getConfigInstance().other.entrySet().stream()
                                    .filter(entry -> entry.getValue().equals(apiToken))
                                    .map(entry -> entry.getKey().toString())
                                    .toList();
                            System.out.println(invalidTokens.isEmpty() ? "where http headers?? (bad)" : "token: " + invalidTokens + " is possibly invalid");
                            return;
                        }

                        int limitInt = Integer.parseInt(limit);

                        guildRateLimitInfo = new ApiUtils.RateLimitInfo(
                                Integer.parseInt(remaining),
                                Integer.parseInt(reset),
                                limitInt,
                                System.currentTimeMillis()
                        );

                        if (status == 404) {
                            return; // no player found
                        }
                        if (status == 500) {
                            //System.out.println("failed ot connect to api " + UuidMap.getUsernameByMinecraftId(uuid));
                            return;
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

                        //System.out.println(apiData.username + " " + apiData.lastJoin);

                        apiData.statusCode = status;
                        results.put(uuid, apiData);

                        PlayerDataShortened playerDataShortened = new PlayerDataShortened(apiData);
                        toUpdate.add(playerDataShortened);
                    })
                    .exceptionally(ex -> {
                        Throwable cause = ex instanceof CompletionException ? ex.getCause() : ex;
                        if (cause instanceof java.net.SocketException) {
                            System.err.println("SocketException: " + Players.get(uuid));
                        } else if ("too many concurrent streams".equals(cause.getMessage())) {
                            System.err.println("Too many concurrent streams, skipping request.");
                        } else if (cause instanceof javax.net.ssl.SSLHandshakeException) {

                        } else if (cause instanceof TimeoutException) {
                            System.out.println("timeout " + Players.get(uuid));
                        } else if (cause instanceof IOException && cause.getMessage().contains("GOAWAY")) {

                        } else if (cause instanceof IOException && cause.getMessage().contains("Connection reset")) {

                        } else {
                            cause.printStackTrace();
                        }
                        return null;
                    });

            futures.add(future);
        }

        // Wait for all to finish
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        PlaytimeHistory.addPlaytimeIfNeeded(toUpdate);
        String prefix = ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix);
        if (Objects.equals(AllGuilds.getGuild(prefix).uuid(), guildUuid)) {
            VerificationCommands.updatePlayers(results);
        }
        return results;
    }

    public static void updateAllGuildMembers() {
        GuildInfo guildInfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
        Set<String> memberUuids = guildInfo.members.getAllMembers().keySet();
        getPlayerData(memberUuids, guildInfo.uuid);
    }

    public static Map<String, PlayerProfile> getAllGuildMembers(String prefix) {
        GuildInfo guildInfo = ApiUtils.getGuildInfo(prefix);
        Set<String> memberUuids = guildInfo.members.getAllMembers().keySet();
        return getPlayerData(memberUuids, guildInfo.uuid);
    }

    public static void debug(SlashCommandInteractionEvent event) {
        event.reply("check console").queue();
        Map<String, Integer> allInMem = sizeToPrefixes.entrySet().stream()
                .flatMap(entry ->
                        entry.getValue().stream()
                                .map(prefix -> Map.entry(prefix, entry.getKey()))
                )
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
        Map<String, Integer> tracked = AllGuilds.getTracked(false);

        Map<String, Integer> trackedNotInMem = tracked.entrySet().stream()
                .filter(entry -> !allInMem.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Map<String, Integer> inMemNotTracked = allInMem.entrySet().stream()
                .filter(entry -> !tracked.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        System.out.println("\ntrackedNotInMem " + trackedNotInMem.size() + "\n" + trackedNotInMem);
        System.out.println("\ninMemNotTracked " + inMemNotTracked.size() + "\\n" + inMemNotTracked);

    }
}
