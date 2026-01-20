package sidly.discord_bot.timed_actions;

import sidly.discord_bot.api.MassGuild;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class GuildMemberUpdater {
    private static Timer timer;
    private static boolean running = false;

    private static Long lastRunTime = 0L;

    public static Long getLastRunTime() {
        return lastRunTime;
    }

    public static void start(){
        if (running) {
            return;
        }
        timer = new Timer();
        running = true;
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    MassGuild.updateAllGuildMembers();
                    lastRunTime = System.currentTimeMillis();
                } catch (Exception e) {
                    e.printStackTrace(); // Log and keep going
                }
            }
        },  4 * 1000, TimeUnit.HOURS.toMillis(1));
    }

    public static void stop(){
        running = false;
        timer.cancel();
    }

    public static boolean getStatus() {
        return running;
    }

}
