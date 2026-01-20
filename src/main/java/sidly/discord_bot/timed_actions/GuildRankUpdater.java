package sidly.discord_bot.timed_actions;

import sidly.discord_bot.commands.GuildCommands;

import java.util.Timer;
import java.util.TimerTask;

public class GuildRankUpdater {
    private static Timer timer;
    private static boolean yourGuildTrackerRunning = false;

    private static Long lastRunTime = 0L;

    public static Long getLastRunTime() {
        return lastRunTime;
    }

    public static void start(){
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
                    lastRunTime = System.currentTimeMillis();
                } catch (Exception e) {
                    e.printStackTrace(); // Log and keep going
                }
            }
        },  4 * 1000, 25 * 1000); // 25 seconds
    }

    public static void stop(){
        yourGuildTrackerRunning = false;
        timer.cancel();
    }

    public static boolean getStatus() {
        return yourGuildTrackerRunning;
    }



}
