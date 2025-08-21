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
import sidly.discord_bot.api.MassGuild;
import sidly.discord_bot.api.PlayerProfile;
import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.PlaytimeHistoryList;
import sidly.discord_bot.database.records.GuildAverages;
import sidly.discord_bot.database.tables.*;
import sidly.discord_bot.page.PageBuilder;
import sidly.discord_bot.page.PaginationIds;

import java.awt.Color;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
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
        AllGuilds.addTracked(guildPrefix, false);
        event.reply("added " + guildPrefix + " to tracked guilds").queue();
    }

    public static void removeTrackedGuild(SlashCommandInteractionEvent event) {
        String guildPrefix = event.getOption("guild_prefix").getAsString();
        AllGuilds.unTracked(guildPrefix);
        event.reply("removed " + guildPrefix + " from tracked guilds").queue();
    }

    public static void viewActiveHours(SlashCommandInteractionEvent event) {
        String guildPrefix = event.getOption("guild_prefix").getAsString();
        String uuid = AllGuilds.getGuild(guildPrefix).uuid();
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


        PageBuilder.PaginationState pageState = PageBuilder.PaginationManager.get(PaginationIds.GUILD.name());

        event.deferReply(false).addComponents(PageBuilder.getPaginationActionRow(PaginationIds.GUILD)).queue(hook -> {
            List<GuildAverages> guildAverages = GuildActivity.getGuildAverages(guildDays);
            String prefix = ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix);
            String uuid = AllGuilds.getGuild(prefix).uuid();
            int index = -1;
            for (int i = 0; i < guildAverages.size(); i++) {
                if (guildAverages.get(i).uuid().equals(uuid)) {
                    index = i;
                    break;
                }
            }

            pageState.customData = "[" + prefix +"] is in position " + (index + 1) + "\n\n";

            pageState.reset(guildAverages);

            EmbedBuilder embed = pageState.buildEmbedPage();

            if (embed == null) {
                event.reply("no guilds").setEphemeral(true).queue();
                return;
            }

            hook.editOriginalEmbeds(embed.build()).queue();
        });
    }

    public static String guildConverter(GuildAverages trackedGuild) {
        String averagePlayers = String.format("%.2f", trackedGuild.averageOnline());
        String averageCaptains = String.format("%.2f", trackedGuild.averageCaptains());
        String prefix = AllGuilds.getPrefixByUuid((trackedGuild.uuid()));
        String guildName = AllGuilds.getGuild(prefix).name();

        StringBuilder sb = new StringBuilder();

        sb.append("**[").append(prefix).append("] ").append(guildName).append("**\n");
        sb.append("Avg. Online: ").append(averagePlayers).append("\n");
        sb.append("Avg. Captains+: ").append(averageCaptains).append("\n\n");

        return sb.toString();
    }


    public static String updatePlayerRanks() {
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
            if (uuid == null) continue;
            Config.Roles rankOfMember = guildinfo.members.getRankOfMemberRole(uuid);
            String rankId = ConfigManager.getConfigInstance().roles.get(rankOfMember);

            // add there rank should return null and therefor remove all if not in guild
            sb.append(VerificationCommands.removeRankRolesExcept(member, rankId));

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

    public static void showStats(SlashCommandInteractionEvent event) {
        String guildPrefix = event.getOption("guild_prefix").getAsString();

        event.deferReply(false).addComponents(PageBuilder.getPaginationActionRow(PaginationIds.GUILD_STATS)).queue(hook -> {

            hook.editOriginalEmbeds(Utils.getEmbed("Patience Woman", "waiting on api requests")).queue();

            GuildInfo guildInfo = ApiUtils.getGuildInfo(guildPrefix);
            if (guildInfo == null || guildInfo.members == null) {
                hook.editOriginalEmbeds(Utils.getEmbed(guildPrefix, " not found")).queue();
                return;
            }

            StringBuilder sb = new StringBuilder();

            sb.append("Level: ").append(guildInfo.level).append(" (").append(guildInfo.xpPercent).append("%)\n");
            sb.append("Held Territories: ").append(guildInfo.territories).append("\n");
            int latestSeason = -1;
            for (Map.Entry<String, GuildInfo.SeasonRank> entry : guildInfo.seasonRanks.entrySet()) {
                int season = Integer.parseInt(entry.getKey());
                if (season > latestSeason) latestSeason = season;
            }
            sb.append("Season Rating: ").append(guildInfo.seasonRanks.get(String.valueOf(latestSeason)).rating).append("\n");
            sb.append("Previous Rating: ").append(guildInfo.seasonRanks.get(String.valueOf(latestSeason - 1)).rating).append("\n");
            sb.append("Wars: ").append(guildInfo.wars).append("\n");
            double averageOnline = GuildActivity.getAverageOnline(AllGuilds.getGuild(guildPrefix).uuid(), 28, false);
            sb.append("Online Members: ").append(guildInfo.online).append(" / ").append(guildInfo.members.total).append(" avg: ").append(String.format("%.2f", averageOnline)).append("\n");

            Map<String, PlayerProfile> allPlayerData = MassGuild.getPlayerData(guildInfo.members.getAllMembers().keySet());
            AtomicLong totalXpPerDay = new AtomicLong();
            AtomicReference<Double> totalWeeklyPlaytime = new AtomicReference<>((double) 0);
            List<GuildStatEntry> sortedPlayerEntries = allPlayerData.values().stream()
                    .map(playerData -> {
                        GuildInfo.MemberInfo guildMemberData = guildInfo.members.getMemberInfo(playerData.uuid);

                        long joinedDaysAgo = Utils.timeSinceIso(guildMemberData.joined, ChronoUnit.DAYS);
                        double weeksSinceJoin = Utils.timeSinceIso(playerData.firstJoin, ChronoUnit.WEEKS);
                        double hoursPerWeek = weeksSinceJoin > 0 ? playerData.playtime / weeksSinceJoin : 0;

                        long xpPerDayMillions;
                        if (joinedDaysAgo > 0) {
                            xpPerDayMillions = guildMemberData.contributed / joinedDaysAgo / 1_000_000;
                        } else {
                            xpPerDayMillions = guildMemberData.contributed / 1_000_000;
                        }

                        // accumulate totals
                        totalXpPerDay.addAndGet(xpPerDayMillions);
                        totalWeeklyPlaytime.updateAndGet(v -> v + hoursPerWeek);

                        return new GuildStatEntry(playerData, guildMemberData, joinedDaysAgo, hoursPerWeek);
                    })
                    .sorted(Comparator.comparing(entry -> entry.guildMemberData.contributionRank))
                    .toList();

            sb.append("XP/day: ").append(totalXpPerDay.get()).append("M/day").append("\n");
            sb.append("Total weekly playtime: ").append(String.format("%.2f", totalWeeklyPlaytime.get())).append(" hours").append("\n");
            sb.append("\n");


            PageBuilder.PaginationState pageState = PageBuilder.PaginationManager.get(PaginationIds.GUILD_STATS.name());
            pageState.reset(sortedPlayerEntries);
            pageState.customData = sb.toString();
            pageState.title = "[" + guildPrefix + "] " + guildInfo.name;

            EmbedBuilder embed = pageState.buildEmbedPage();
            if (embed == null) {
                hook.editOriginalEmbeds(Utils.getEmbed("well this is awkward", "something went wrong")).queue();
                return;
            }
            hook.editOriginalEmbeds(embed.build()).queue();
        });
    }

    public static String guildStatsConverter(GuildStatEntry statEntry) {
        PlayerProfile playerData = statEntry.playerProfile();
        GuildInfo.MemberInfo guildMemberData = statEntry.guildMemberData();
        long xpPerDayMillions;
        if (statEntry.joinedDaysAgo > 0) {
            xpPerDayMillions = guildMemberData.contributed / statEntry.joinedDaysAgo / 1000000;
        } else xpPerDayMillions = guildMemberData.contributed / 1000000;
        int rank = guildMemberData.contributionRank;
        long joinedWynn = Utils.timeSinceIso(playerData.firstJoin, ChronoUnit.DAYS);

        StringBuilder sb = new StringBuilder();
        sb.append("**").append(rank).append(". ").append(Utils.escapeDiscordMarkdown(playerData.username)).append(" (").append(playerData.guild.rank).append(")**");
        sb.append(playerData.online ? "Online " + playerData.server + "\n" : "Offline, last seen " + Utils.timeSinceIso(playerData.lastJoin, ChronoUnit.HOURS) + " hours ago\n");
        sb.append("joined guild ").append(statEntry.joinedDaysAgo).append(" days ago\n");
        sb.append("joined wynn ").append(joinedWynn).append(" days ago\n");
        sb.append(Utils.addNumberFormattingCommas(String.valueOf(guildMemberData.contributed))).append(" XP (").append(xpPerDayMillions).append("M/day)\n");
        if (playerData.globalData != null) {
            sb.append(Utils.addNumberFormattingCommas(String.valueOf(playerData.globalData.wars))).append(" wars\n");
            Map<String, Integer> raids = playerData.globalData.raids.list;
            raids.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> sb.append(Utils.abbreviate(entry.getKey()))
                            .append(": ")
                            .append(entry.getValue())
                            .append(" "));

            sb.append("\n");
        }

        sb.append(Utils.addNumberFormattingCommas(String.valueOf(playerData.playtime))).append(" hours played\n");
        sb.append(String.format("%.2f", statEntry.hoursPerWeek)).append(" hours per week (all time)\n");
        PlaytimeHistoryList playtimeHistory = PlaytimeHistory.getPlaytimeHistory(playerData.uuid);
        sb.append(String.format("%.2f", playtimeHistory.getAverage(4))).append(" hours per week (last 4 weeks)\n");
        sb.append("\n");

        return sb.toString();
    }

    public record GuildStatEntry(PlayerProfile playerProfile,GuildInfo.MemberInfo guildMemberData, long joinedDaysAgo, double hoursPerWeek){}
}
