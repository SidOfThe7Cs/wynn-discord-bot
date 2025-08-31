package sidly.discord_bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.MassGuild;
import sidly.discord_bot.timed_actions.GuildRankUpdater;
import sidly.discord_bot.timed_actions.UpdatePlayers;

public class TimerCommands {
    public static void getTimerStatus(SlashCommandInteractionEvent event) {
        boolean allGuildTrackerTimerStatus = MassGuild.getTimerStatus();
        boolean yourGuildTrackerTimerStatus = GuildRankUpdater.getYourGuildTrackerTimerStatus();
        boolean playerUpdater = UpdatePlayers.isRunning();

        String description = "PlayerUpdater: " + (playerUpdater ? "active" : "inactive") + "\n" +
                "guildTracker: " + (allGuildTrackerTimerStatus ? "active" : "inactive") + "\n" +
                "yourGuildRankUpdater: " + (yourGuildTrackerTimerStatus ? "active" : "inactive") + "\n";

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
                GuildRankUpdater.startYourGuildTracker();
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
                GuildRankUpdater.stopYourGuildTracker();
                break;
        }
        event.reply("timer " + timerName + " stopped").queue();
    }
}
