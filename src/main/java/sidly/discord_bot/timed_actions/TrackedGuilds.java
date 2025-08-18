package sidly.discord_bot.timed_actions;

import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.commands.GuildCommands;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class TrackedGuilds {
    private static int index = 0;
    private static Timer timer;
    private static boolean yourGuildTrackerRunning = false;
    private static DynamicTimer guildTracker;
    private static boolean guildTrackerRunning = false;

    public static void init(){
        startTrackedGuildsTimer();
        startYourGuildTracker();
    }

    public static void startYourGuildTracker(){
        if (yourGuildTrackerRunning) {
            return;
        }
        timer = new Timer();
        yourGuildTrackerRunning = true;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    GuildCommands.updatePlayerRanks();
                } catch (Exception e) {
                    e.printStackTrace(); // Log and keep going
                }
            }
        },  4 * 1000, 25 * 1000); // 25 seconds
    }

    public static void startTrackedGuildsTimer() {
        if (guildTrackerRunning) {
            return;
        }
        guildTrackerRunning = true;
        guildTracker = new DynamicTimer(sidly.discord_bot.database.tables.TrackedGuilds.get(), TrackedGuilds::trackNext, TimeUnit.MINUTES.toMillis(8), 2000);
        guildTracker.start();
    }

    private static void trackNext() {
        List<String> guilds = sidly.discord_bot.database.tables.TrackedGuilds.get();
        if (guilds.isEmpty()) return;

        if (index >= guilds.size()) index = 0;
        String prefix = guilds.get(index);
        index++;

        ApiUtils.getGuildInfo(prefix);
    }

    public static void stopYourGuildTracker(){
        yourGuildTrackerRunning = false;
        timer.cancel();
    }

    public static void stopAllGuildTracker(){
        guildTrackerRunning = false;
        guildTracker.cancel();
    }

    public static boolean getYourGuildTrackerTimerStatus() {
        return yourGuildTrackerRunning;
    }

    public static boolean getAllGuildTrackerTimerStatus() {
        return guildTrackerRunning;
    }

}
