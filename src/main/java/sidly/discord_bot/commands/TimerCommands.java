package sidly.discord_bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.MassGuild;
import sidly.discord_bot.timed_actions.GuildMemberUpdater;
import sidly.discord_bot.timed_actions.GuildRankUpdater;
import sidly.discord_bot.timed_actions.UpdatePlayers;

public class TimerCommands {
    public static void getTimerStatus(SlashCommandInteractionEvent event) {
        boolean allGuildTrackerTimerStatus = MassGuild.getTimerStatus();
        Long allGuildTrackerLastRun = MassGuild.getTimerLastRun();
        boolean yourGuildTrackerTimerStatus = GuildRankUpdater.getStatus();
        Long yourGuildTrackerLastRun = GuildRankUpdater.getLastRunTime();
        boolean playerUpdater = UpdatePlayers.isRunning();
        Long playerUpdaterLastRun = UpdatePlayers.getLastRunTime();
        boolean yourGuildMemberUpdater = GuildMemberUpdater.getStatus();
        Long yourGuildMemberUpdaterLastRun = GuildMemberUpdater.getLastRunTime();

        String description = "playerUpdater: " + (playerUpdater ? "active" : "inactive")
                + " last run " + Utils.getDiscordTimestamp(playerUpdaterLastRun,true) + "\n" +
                "guildTracker: " + (allGuildTrackerTimerStatus ? "active" : "inactive")
                + " last run " + Utils.getDiscordTimestamp(allGuildTrackerLastRun,true) + "\n" +
                "yourGuildRankUpdater: " + (yourGuildTrackerTimerStatus ? "active" : "inactive")
                + " last run " + Utils.getDiscordTimestamp(yourGuildTrackerLastRun,true) + "\n" +
                "yourGuildMemberUpdater: " + (yourGuildMemberUpdater ? "active" : "inactive")
                + " last run " + Utils.getDiscordTimestamp(yourGuildMemberUpdaterLastRun,true) + "\n";

        event.replyEmbeds(Utils.getEmbed("Timers", description)).setEphemeral(true).queue();

    }

    public static void startTimer(SlashCommandInteractionEvent event) {
        String timerName = event.getOption("timer_name").getAsString();
        switch (timerName) {
            case "playerUpdater":
                UpdatePlayers.init();
                break;
            case "guildTracker":
                MassGuild.startTimer();
                break;
            case "yourGuildRankUpdater":
                GuildRankUpdater.start();
                break;
            case "yourGuildMemberUpdater":
                GuildMemberUpdater.start();
                break;
        }
        event.reply("timer " + timerName + " started").queue();
    }

    public static void stopTimer(SlashCommandInteractionEvent event) {
        String timerName = event.getOption("timer_name").getAsString();
        switch (timerName) {
            case "playerUpdater":
                UpdatePlayers.shutdown();
                break;
            case "guildTracker":
                MassGuild.stopTimer();
                break;
            case "yourGuildRankUpdater":
                GuildRankUpdater.stop();
                break;
            case "yourGuildMemberUpdater":
                GuildMemberUpdater.stop();
                break;
        }
        event.reply("timer " + timerName + " stopped").queue();
    }
}
