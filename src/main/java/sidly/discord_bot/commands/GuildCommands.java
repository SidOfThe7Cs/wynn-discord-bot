package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.MainEntrypoint;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.database.records.GuildAverages;
import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.tables.GuildActivity;
import sidly.discord_bot.database.tables.Players;
import sidly.discord_bot.database.tables.TrackedGuilds;
import sidly.discord_bot.database.tables.UuidMap;
import sidly.discord_bot.page.PageBuilder;
import sidly.discord_bot.page.PaginationIds;

import java.awt.Color;
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
        if (guild == null) return;
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

        return map.entrySet().stream()
                .filter(entry -> entry.getValue().online)
                .map(entry -> {
                    String key = entry.getKey();
                    GuildInfo.MemberInfo member = entry.getValue();

                    // Escape username
                    String username = Utils.escapeDiscordMarkdown(member.username);

                    // Get PlayerDataShortened using the key
                    PlayerDataShortened playerDataShortened = Players.get(key);

                    if (playerDataShortened != null && playerDataShortened.supportRank != null && !playerDataShortened.supportRank.isEmpty()) {
                        username += " [" + playerDataShortened.supportRank.toUpperCase() + "]";
                    }

                    return username;
                })
                .sorted()
                .collect(Collectors.joining("\n"));

    }


    public static void addTrackedGuild(SlashCommandInteractionEvent event) {
        String guildPrefix = event.getOption("guild_prefix").getAsString();
        if (!TrackedGuilds.isTracked(guildPrefix)) {
            TrackedGuilds.add(guildPrefix);
        }
        event.reply("added " + guildPrefix + " to tracked guilds").queue();
    }

    public static void removeTrackedGuild(SlashCommandInteractionEvent event) {
        String guildPrefix = event.getOption("guild_prefix").getAsString();
        if (TrackedGuilds.isTracked(guildPrefix)) {
            TrackedGuilds.remove(guildPrefix);
        }
        event.reply("removed " + guildPrefix + " from tracked guilds").queue();
    }

    public static void viewActiveHours(SlashCommandInteractionEvent event) {
        String guildPrefix = event.getOption("guild_prefix").getAsString();
        String uuid = UuidMap.getMinecraftIdByUsername(guildPrefix);
        int days = Optional.ofNullable(event.getOption("days"))
                .map(OptionMapping::getAsInt)
                .orElse(-1);


        if (!GuildActivity.containsPrefix(guildPrefix)) {
            event.reply("guild not found").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.CYAN);
        embed.setTitle("[" + guildPrefix + "] " + GuildActivity.getGuildName(uuid) + "Active Hours\n");

        List<String> hours = new ArrayList<>();
        List<String> players = new ArrayList<>();
        List<String> captains = new ArrayList<>();

        for (int hour = 0; hour < 24; hour++) {
            double averagePlayers = GuildActivity.getAverageOnline(uuid, hour, days, false);
            double averageCaptains = GuildActivity.getAverageOnline(uuid, hour, days, true);

            // If the average is negative (no data), show "--.00"
            String playersStr = averagePlayers < 0 ? "--.--" : String.format("%02.2f", averagePlayers);
            String captainsStr = averageCaptains < 0 ? "--.--" : String.format("%02.2f", averageCaptains);

            hours.add(Utils.getTimestampFromInt(hour));
            players.add(playersStr);
            captains.add(captainsStr);
        }

        embed.addField("Hours", String.join("\n", hours), true);
        embed.addField("Players", String.join("\n", players), true);
        embed.addField("Captains", String.join("\n", captains), true);

        event.replyEmbeds(embed.build()).queue();
    }

    public static int guildDays = -1;
    public static void viewTrackedGuilds(SlashCommandInteractionEvent event) {
         guildDays = Optional.ofNullable(event.getOption("days"))
            .map(OptionMapping::getAsInt)
            .orElse(-1);



        EmbedBuilder embed = buildGuildsPage();

        if (embed == null) {
            event.reply("no guilds").setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(embed.build())
                .addComponents(Utils.getPaginationActionRow(PaginationIds.GUILD))
                .queue();
    }

    public static EmbedBuilder buildGuildsPage() {
        PageBuilder.PaginationState paginationState = PageBuilder.PaginationManager.get(PaginationIds.GUILD.name());

        List<GuildAverages> sortedGuilds = GuildActivity.getGuildAverages(guildDays);

        if (sortedGuilds.isEmpty()) {
            return null;
        }

        List<String> entries = new ArrayList<>();
        for (GuildAverages trackedGuild : sortedGuilds) {
            StringBuilder sb = new StringBuilder();

            String averagePlayers = String.format("%.2f", trackedGuild.averageOnline());
            String averageCaptains = String.format("%.2f", trackedGuild.averageCaptains());
            String guildName = GuildActivity.getGuildName(trackedGuild.uuid());

            sb.append("[**").append(UuidMap.getUsernameByMinecraftId(trackedGuild.uuid())).append("**] ").append(guildName).append("\n");
            sb.append("Avg. Online: ").append(averagePlayers).append("\n");
            sb.append("Avg. Captains+: ").append(averageCaptains).append("\n\n");

            entries.add(sb.toString());
        }

        return PageBuilder.buildEmbedPage(entries, paginationState, 10, "Average activity for tracked guilds");
    }


    public static String updatePlayerRanks(){
        GuildInfo guildinfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
        if (guildinfo == null || guildinfo.members == null) return "";

        // Get the guild ID from config
        String guildId = ConfigManager.getConfigInstance().other.get(Config.Settings.YourDiscordServerId);
        if (guildId == null) {
            System.err.println("Server ID is not set in config.");
            return "";
        }

        // Get the guild from JDA
        Guild guild = MainEntrypoint.jda.getGuildById(guildId);
        if (guild == null) {
            System.err.println("Guild not found for ID: " + guildId);
            return "";
        }

        List<Member> members = guild.getMembers();
        Map<String, GuildInfo.MemberInfo> allMembers = guildinfo.members.getAllMembers();


        StringBuilder sbfinal = new StringBuilder();
        for (Member member : members) { // for each user in the discord
            StringBuilder sb = new StringBuilder();
            String username = member.getEffectiveName().split("\\[")[0].trim().toLowerCase();
            String uuid = UuidMap.getMinecraftIdByUsername(username);
                Config.Roles rankOfMember = guildinfo.members.getRankOfMemberRole(uuid);
                String rankId = ConfigManager.getConfigInstance().roles.get(rankOfMember);

                // add there rank should return null and therefor remove all if not in guild
                sb.append(VerificationCommands.removeRankRolesExcept(member, rankId));

                //if they have roles or not log tothe string builder if there were any updates add there mention

            if (allMembers.containsKey(uuid)) { // they are in the wynncraft guild
                sb.append(Utils.addRole(member, Config.Roles.MemberRole));
            } else sb.append(Utils.removeRole(member, Config.Roles.MemberRole));

            if (!sb.toString().isEmpty()) {
                sbfinal.append(member.getAsMention()).append("\n").append(sb);
            }
        }

        String text = sbfinal.toString();
        if (text.isEmpty()) return "";

        Utils.sendToModChannel("Updated Player Ranks", text, false);
        return text;
    }

    public static void updatePlayerRanks(SlashCommandInteractionEvent event) {
        String result = updatePlayerRanks();
        event.replyEmbeds(Utils.getEmbed("Updated Players", result)).setEphemeral(true).queue();
    }
}
