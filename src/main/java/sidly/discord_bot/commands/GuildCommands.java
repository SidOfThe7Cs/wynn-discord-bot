package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.database.GuildDataActivity;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class GuildCommands {

    public static void showGuildXpLeaderboard(SlashCommandInteractionEvent event) {
        // Get guild members
        GuildInfo guild = ApiUtils.getGuildInfo(event.getOption("guild_prefix").getAsString());
        GuildInfo.Members members = guild.members;

        if (members == null) {
            event.reply("Could not find guild").setEphemeral(true).queue();
            return;
        }

        // Collect all members into a list
        List<GuildInfo.MemberInfo> allMembers = new ArrayList<>(members.total);

        if (members.owner != null) allMembers.addAll(members.owner.values());
        if (members.chief != null) allMembers.addAll(members.chief.values());
        if (members.strategist != null) allMembers.addAll(members.strategist.values());
        if (members.captain != null) allMembers.addAll(members.captain.values());
        if (members.recruiter != null) allMembers.addAll(members.recruiter.values());
        if (members.recruit != null) allMembers.addAll(members.recruit.values());

        // Sort by contributionRank ascending
        allMembers.sort(Comparator.comparingInt(a -> a.contributionRank));

        StringBuilder leaderboard = new StringBuilder();
        for (GuildInfo.MemberInfo member : allMembers) {
            leaderboard
                    .append(member.contributionRank)
                    .append(". ")
                    .append(member.username)
                    .append(": ")
                    .append(member.contributed)
                    .append("\n");
        }

        if (leaderboard.isEmpty()) {
            leaderboard.append("No members found.");
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Guild XP Leaderboard")
                .setDescription(leaderboard.toString())
                .setColor(Color.CYAN)
                .setFooter("Last updated");

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    public static void showOnlineMembers(SlashCommandInteractionEvent event) {
        GuildInfo guild = ApiUtils.getGuildInfo(event.getOption("guild_prefix").getAsString());
        GuildInfo.Members members = guild.members;
        int count = guild.online;

        if (members == null) {
            event.reply("Could not find guild members.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Online Members in " + guild.name + " [" + guild.prefix + "]" + "\n" + count + "/" + guild.members.total)
                .setColor(Color.CYAN)
                .setFooter("Last updated");



        embed.addField("Owner", membersList(members.owner), false);
        embed.addField("Chief", membersList(members.chief), false);
        embed.addField("Strategist", membersList(members.strategist), false);
        embed.addField("Captain", membersList(members.captain), false);
        embed.addField("Recruiter", membersList(members.recruiter), false);
        embed.addField("Recruit", membersList(members.recruit), false);

        event.replyEmbeds(embed.build()).queue();
    }

    // Helper function to build a string of online members for a rank
    static String membersList(Map<String, GuildInfo.MemberInfo> map) {
        if (map == null || map.isEmpty()) return "_None_";
        return map.values().stream()
                .filter(member -> member.online)
                .map(member -> Utils.escapeDiscordMarkdown(member.username))
                .sorted()
                .collect(Collectors.joining("\n"));
    }

    public static void addTrackedGuild(SlashCommandInteractionEvent event) {
        String guildPrefix = event.getOption("guild_prefix").getAsString();
        ConfigManager.getDatabaseInstance().trackedGuilds.add(guildPrefix);
        event.reply("added " + guildPrefix).setEphemeral(true).queue();
    }

    public static void removeTrackedGuild(SlashCommandInteractionEvent event) {
        String guildPrefix = event.getOption("guild_prefix").getAsString();
        ConfigManager.getDatabaseInstance().trackedGuilds.remove(guildPrefix);
        event.reply("removed " + guildPrefix).setEphemeral(true).queue();
    }

    public static void viewActiveHours(SlashCommandInteractionEvent event) {
        String guildPrefix = event.getOption("guild_prefix").getAsString();
        int days = Optional.ofNullable(event.getOption("days"))
                .map(OptionMapping::getAsInt)
                .orElse(-1);

        GuildDataActivity guildDataActivity = ConfigManager.getDatabaseInstance().trackedGuildActivity.get(guildPrefix);

        if (guildDataActivity == null) {
            event.reply("guild not found").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.CYAN);
        embed.setTitle("[" + guildPrefix + "] " + guildDataActivity.getGuildName() + "Active Hours");
        StringBuilder sb = new StringBuilder();
        sb.append(" Hour ┃ Players ┃ Captains\n━━━━━━╋━━━━━━━━━╋━━━━━━━━━\n");

        for (int hour = 0; hour < 24; hour++) {
            double averagePlayers = guildDataActivity.getAverageOnline(hour, days, false);
            double averageCaptains = guildDataActivity.getAverageOnline(hour, days, true);

            // If the average is negative (no data), show "--.00"
            String playersStr = averagePlayers < 0 ? "--.--" : String.format("%02.2f", averagePlayers);
            String captainsStr = averageCaptains < 0 ? "--.--" : String.format("%02.2f", averageCaptains);

            sb.append(String.format(
                    "%s ┃ %6s  ┃ %6s%n",
                    Utils.getTimestampFromInt(hour),
                    playersStr,
                    captainsStr
            ));

        }

        embed.setDescription(sb.toString());
        event.replyEmbeds(embed.build()).queue();
    }

    public static void viewTrackedGuilds(SlashCommandInteractionEvent event) {
        int days = Optional.ofNullable(event.getOption("days"))
                .map(OptionMapping::getAsInt)
                .orElse(-1);

        for (String trackedGuild : ConfigManager.getDatabaseInstance().trackedGuilds) {
            GuildDataActivity guildDataActivity = ConfigManager.getDatabaseInstance().trackedGuildActivity.get(trackedGuild);

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.CYAN);
            embed.setTitle("Average activity for tracked guilds");
            StringBuilder sb = new StringBuilder();

            String averagePlayers = "?";
            String averageCaptains = "?";;

            if (guildDataActivity != null) {
                averagePlayers = String.valueOf(guildDataActivity.getAverageOnline(days, false));
                averageCaptains = String.valueOf(guildDataActivity.getAverageOnline(days, true));
            }

            sb.append("[**").append(trackedGuild).append("**] ").append(guildDataActivity.getGuildName()).append("\n");
            sb.append("Avg. Online: ").append(averagePlayers).append("\n");
            sb.append("Avg. Captains+: ").append(averageCaptains).append("\n");

            embed.setDescription(sb.toString());
            event.replyEmbeds(embed.build()).queue();
        }
    }
}
