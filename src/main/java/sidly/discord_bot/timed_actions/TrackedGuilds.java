package sidly.discord_bot.timed_actions;

import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.api.ApiUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackedGuilds {
    private static int index = 0;

    public static void init(){
        new DynamicTimer(ConfigManager.getDatabaseInstance().trackedGuilds, TrackedGuilds::trackNext, TimeUnit.MINUTES.toMillis(8), 3000).start();
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
