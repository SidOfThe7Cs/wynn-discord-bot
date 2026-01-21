package sidly.discord_bot.timed_actions;

import sidly.discord_bot.Utils;
import sidly.discord_bot.api.MassGuild;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class DetectTimerBreaks {

    private static Long lastRunTime = 0L;

    public static Long getLastRunTime() {
        return lastRunTime;
    }

    public static void init(){
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    lastRunTime = System.currentTimeMillis();
                    StringBuilder sb = new StringBuilder();

                    Long allGuildTrackerLastRun = MassGuild.getTimerLastRun();
                    Long yourGuildTrackerLastRun = GuildRankUpdater.getLastRunTime();
                    Long playerUpdaterLastRun = UpdatePlayers.getLastRunTime();
                    Long yourGuildMemberUpdaterLastRun = GuildMemberUpdater.getLastRunTime();

                    Long now = System.currentTimeMillis();

                    if (MassGuild.getTimerStatus() && now - allGuildTrackerLastRun > 60000) {
                        sb.append("guildTracker has not run since ");
                        sb.append(Utils.getDiscordTimestamp(allGuildTrackerLastRun, true));
                        sb.append("\n");
                    }

                    if (GuildRankUpdater.getStatus() && now - yourGuildTrackerLastRun > 300000) {
                        sb.append("yourGuildRankUpdater has not run since ");
                        sb.append(Utils.getDiscordTimestamp(yourGuildTrackerLastRun, true));
                        sb.append("\n");
                    }

                    if (UpdatePlayers.isRunning() && now - playerUpdaterLastRun > 10000) {
                        sb.append("playerUpdater has not run since ");
                        sb.append(Utils.getDiscordTimestamp(playerUpdaterLastRun, true));
                        sb.append("\n");
                    }

                    if (GuildMemberUpdater.getStatus() && now - yourGuildMemberUpdaterLastRun > TimeUnit.MINUTES.toMillis(150)) {
                        sb.append("yourGuildMemberUpdater has not run since ");
                        sb.append(Utils.getDiscordTimestamp(yourGuildMemberUpdaterLastRun, true));
                        sb.append("\n");
                    }

                    sb.append(MassGuild.getOverfullQueues());

                    if (!sb.isEmpty()) {
                        sb.append("\n").append("maybe try running /stoptimer and /starttimer");
                    }

                    Utils.sendToModChannel("TIMER ERROR", sb.toString(), false);

                } catch (Exception e) {
                    Utils.sendToModChannel("AHHHHHHHHHH", "THE TIMER THAT CHECKS IF THE OTHER TIMERS ARE RUNNING FAILED", false);
                    e.printStackTrace();
                }
            }
        },  120000, 10000);
    }
}
