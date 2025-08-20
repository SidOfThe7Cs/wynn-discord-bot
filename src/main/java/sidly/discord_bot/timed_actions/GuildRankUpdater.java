package sidly.discord_bot.timed_actions;

import sidly.discord_bot.commands.GuildCommands;

import java.util.Timer;
import java.util.TimerTask;

public class GuildRankUpdater {
    private static int index = 0;
    private static Timer timer;
    private static boolean yourGuildTrackerRunning = false;

    public static void init(){
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

    public static void stopYourGuildTracker(){
        yourGuildTrackerRunning = false;
        timer.cancel();
    }

    public static boolean getYourGuildTrackerTimerStatus() {
        return yourGuildTrackerRunning;
    }



}
