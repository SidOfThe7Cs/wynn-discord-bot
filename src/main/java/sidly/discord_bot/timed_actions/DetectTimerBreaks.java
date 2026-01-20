package sidly.discord_bot.timed_actions;

import sidly.discord_bot.Utils;
import sidly.discord_bot.api.MassGuild;

import java.util.Timer;
import java.util.TimerTask;

public class DetectTimerBreaks {
    public static void init(){
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    StringBuilder sb = new StringBuilder();

                    Long allGuildTrackerLastRun = MassGuild.getTimerLastRun();
                    Long yourGuildTrackerLastRun = GuildRankUpdater.getLastRunTime();
                    Long playerUpdaterLastRun = UpdatePlayers.getLastRunTime();
                    Long yourGuildMemberUpdaterLastRun = GuildMemberUpdater.getLastRunTime();

                    Long now = System.currentTimeMillis();

                    if (now - allGuildTrackerLastRun > 20000) {
                        sb.append("guildTracker has not run since ");
                        sb.append(Utils.getDiscordTimestamp(allGuildTrackerLastRun, true));
                        sb.append("\n");
                    }

                    if (now - yourGuildTrackerLastRun > 20000) {
                        sb.append("yourGuildRankUpdater has not run since ");
                        sb.append(Utils.getDiscordTimestamp(yourGuildTrackerLastRun, true));
                        sb.append("\n");
                    }

                    if (now - playerUpdaterLastRun > 20000) {
                        sb.append("playerUpdater has not run since ");
                        sb.append(Utils.getDiscordTimestamp(playerUpdaterLastRun, true));
                        sb.append("\n");
                    }

                    if (now - yourGuildMemberUpdaterLastRun > 20000) {
                        sb.append("yourGuildMemberUpdater has not run since ");
                        sb.append(Utils.getDiscordTimestamp(yourGuildMemberUpdaterLastRun, true));
                        sb.append("\n");
                    }

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
