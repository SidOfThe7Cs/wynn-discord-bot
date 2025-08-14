package sidly.discord_bot.timed_actions;

import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.commands.GuildCommands;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class TrackedGuilds {
    private static int index = 0;
    private static final Timer timer = new Timer();

    public static void init(){
        new DynamicTimer(ConfigManager.getDatabaseInstance().trackedGuilds, TrackedGuilds::trackNext, TimeUnit.MINUTES.toMillis(8), 2000).start();

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

    private static void trackNext() {
        List<String> guilds = ConfigManager.getDatabaseInstance().trackedGuilds;
        if (guilds.isEmpty()) return;

        if (index >= guilds.size()) index = 0;
        String prefix = guilds.get(index);
        index++;

        ApiUtils.getGuildInfo(prefix);
    }
}
