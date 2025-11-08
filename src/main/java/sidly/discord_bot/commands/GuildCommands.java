package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ClientType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import sidly.discord_bot.*;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.api.MassGuild;
import sidly.discord_bot.api.PlayerProfile;
import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.PlaytimeHistoryList;
import sidly.discord_bot.database.records.GuildAverages;
import sidly.discord_bot.database.records.GuildName;
import sidly.discord_bot.database.tables.*;
import sidly.discord_bot.page.PageBuilder;
import sidly.discord_bot.page.PaginationIds;

import java.awt.Color;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
        event.deferReply(false).queue(hook -> {
            String prefix = event.getOption("guild_prefix").getAsString();
            GuildInfo guild = ApiUtils.getGuildInfo(prefix);
            if (guild == null) return;
            GuildInfo.Members members = guild.members;

            if (members == null) {
                hook.editOriginalEmbeds(Utils.getEmbed("", "Could not find guild members.")).queue();
                return;
            }

            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("Online Members in " + guild.name + " [" + guild.prefix + "]" + "\n" + guild.online + "/" + guild.members.total)
                    .setColor(Color.CYAN)
                    .setFooter("Last updated");


            Map<String, PlayerProfile> allGuildMembers = MassGuild.getAllGuildMembers(prefix);
            Set<String> recentlyOffline = allGuildMembers.entrySet().stream()
                    .filter(entry -> {
                        PlayerProfile playerData = entry.getValue();
                        if (playerData == null) return false;
                        if (playerData.online) return false;

                        long lastSeen = Utils.timeSinceIso(playerData.lastJoin, ChronoUnit.SECONDS);
                        return lastSeen < 90;
                    })
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());


            embed.addField("Owner", membersList(members.owner, recentlyOffline), false);
            embed.addField("Chief", membersList(members.chief, recentlyOffline), false);
            embed.addField("Strategist", membersList(members.strategist, recentlyOffline), false);
            embed.addField("Captain", membersList(members.captain, recentlyOffline), false);
            embed.addField("Recruiter", membersList(members.recruiter, recentlyOffline), false);
            embed.addField("Recruit", membersList(members.recruit, recentlyOffline), false);

            hook.editOriginalEmbeds(embed.build()).queue();
        });
    }

    // Helper function to build a string of online members for a rank string is uuid
    static String membersList(Map<String, GuildInfo.MemberInfo> map, Set<String> streamers) {
        if (map == null || map.isEmpty()) return "_None_";

        return map.entrySet().stream()
                .filter(entry -> entry.getValue().online || streamers.contains(entry.getKey()))
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

                    if (streamers.contains(key)) {
                        username += " (Streamer)";
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
        event.deferReply(false).queue(hook -> {

            boolean codeBlock = Optional.ofNullable(event.getOption("use_code_block"))
                    .map(OptionMapping::getAsBoolean)
                    .orElse(true);

            String guildPrefix = event.getOption("guild_prefix").getAsString();
            GuildName guild = AllGuilds.getGuild(guildPrefix);

            if (guild == null) {
                hook.editOriginalEmbeds(Utils.getEmbed("", "could not find guild")).queue();
                return;
            }

            String uuid = guild.uuid();
            int days = Optional.ofNullable(event.getOption("days"))
                    .map(OptionMapping::getAsInt)
                    .orElse(30);


            if (!GuildActivity.containsPrefix(guildPrefix)) {
                hook.editOriginal("guild not found").queue();
                return;
            }

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.CYAN);
            embed.setTitle("[" + guildPrefix + "] " + GuildActivity.getGuildName(uuid) + " Active Hours\n");

            StringBuilder sb = new StringBuilder();
            sb.append("00:00 is your ").append(Utils.getTimestampFromInt(0)).append("\n");
            sb.append("``` Hour ┃ Players ┃ Captains\n━━━━━━╋━━━━━━━━━╋━━━━━━━━━\n");

            List<String> hours = new ArrayList<>();
            List<String> players = new ArrayList<>();
            List<String> captains = new ArrayList<>();

            for (int hour = 0; hour < 24; hour++) {
                double averagePlayers = GuildActivity.getAverageOnline(uuid, hour, days, false);
                double averageCaptains = GuildActivity.getAverageOnline(uuid, hour, days, true);

                if (codeBlock) {
                    sb.append(String.format(
                            "%02d:00 ┃ %s%.2f  ┃ %s%.2f%n",
                            hour,
                            Double.isNaN(averagePlayers) ? "   " : (averagePlayers >= 10.0 ? " " : "  "),
                            averagePlayers,
                            Double.isNaN(averageCaptains) ? "   " : (averageCaptains >= 10.0 ? " " : "  "),
                            averageCaptains
                    ));

                } else {
                    String playersStr = averagePlayers < 0 ? "--.--" : String.format("%02.2f", averagePlayers);
                    String captainsStr = averageCaptains < 0 ? "--.--" : String.format("%02.2f", averageCaptains);

                    hours.add(Utils.getTimestampFromInt(hour));
                    players.add(playersStr);
                    captains.add(captainsStr);
                }
            }

            if (codeBlock) {
                sb.append("```");
                embed.setDescription(sb.toString());
            } else {
                embed.addField("Hours", String.join("\n", hours), true);
                embed.addField("Players", String.join("\n", players), true);
                embed.addField("Captains", String.join("\n", captains), true);
            }

            hook.editOriginalEmbeds(embed.build()).queue();
        });
    }

    public static int guildDays = -1;

    public static void viewTrackedGuilds(SlashCommandInteractionEvent event) {
        guildDays = Optional.ofNullable(event.getOption("days"))
                .map(OptionMapping::getAsInt)
                .orElse(30);


        PageBuilder.PaginationState pageState = PageBuilder.PaginationManager.get(PaginationIds.GUILD.name());

        event.deferReply(false).addComponents(PageBuilder.getPaginationActionRow(PaginationIds.GUILD)).queue(hook -> {
            List<GuildAverages> guildAverages = GuildActivity.getGuildAverages(guildDays);

            // check the position of your guild
            String prefix = ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix);
            GuildName guild = AllGuilds.getGuild(prefix);

            if (guild == null) {
                hook.editOriginalEmbeds(Utils.getEmbed("", "could not find guild")).queue();
                return;
            }

            String uuid = guild.uuid();
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
                hook.editOriginalEmbeds(Utils.getEmbed("", "no guilds")).queue();
                return;
            }

            hook.editOriginalEmbeds(embed.build()).queue();
        });
    }

    public static String guildConverter(GuildAverages trackedGuild) {
        ApiUtils.RateLimitInfo rateLimitInfo = ApiUtils.rateLimitInfoMap.get(ApiUtils.RateLimitTypes.GUILD);

        String onlineCount = "?";
        String onlineCaptainCount = "?";

        if (rateLimitInfo.getRemaining() > 20) {
            GuildInfo guildInfo = ApiUtils.getGuildInfo(AllGuilds.getPrefixByUuid(trackedGuild.uuid())); // afaik there is no api for getting guild by uuid idk why
            if (guildInfo != null && guildInfo.members != null) {
                onlineCount = String.valueOf(guildInfo.online);
                onlineCaptainCount = String.valueOf(guildInfo.members.getOnlineCaptainsPlusCount());
            }
        }

        String averagePlayers = String.format("%.2f", trackedGuild.averageOnline()) + " (" + onlineCount + ")";
        String averageCaptains = String.format("%.2f", trackedGuild.averageCaptains()) + " (" + onlineCaptainCount + ")";
        String prefix = AllGuilds.getPrefixByUuid((trackedGuild.uuid()));
        GuildName guild = AllGuilds.getGuild(prefix);
        String guildName = "guild name not found";
        if (guild != null) {
             guildName = guild.name();
        }

        return "**[" + prefix + "] " + guildName + "**\n" +
                "Avg. Online: " + averagePlayers + "\n" +
                "Avg. Captains+: " + averageCaptains + "\n\n";
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
            if (!RoleUtils.hasRole(member, Config.Roles.VerifiedRole)) continue;
            StringBuilder sb = new StringBuilder();
            String username = member.getEffectiveName().split("\\[")[0].trim().toLowerCase();
            String uuid = UuidMap.getMinecraftIdByUsername(username);
            if (uuid == null) continue;
            Config.Roles rankOfMember = guildinfo.members.getRankOfMemberRole(uuid);
            String rankId = ConfigManager.getConfigInstance().roles.get(rankOfMember);

            // add there rank should return null and therefor remove all if not in guild
            sb.append(RoleUtils.removeRankRolesExcept(member, rankId));

            if (allMembers.containsKey(uuid)) { // they are in the wynncraft guild
                sb.append(RoleUtils.addRole(member, Config.Roles.MemberRole));
            } else sb.append(RoleUtils.removeRole(member, Config.Roles.MemberRole));

            if (!sb.toString().isEmpty()) {
                sbfinal.append(member.getAsMention()).append("\n").append(sb);
            }
        }

        String text = sbfinal.toString();
        if (text.isEmpty()) return "";

        //Utils.sendToModChannel("Updated Player Ranks", text, false);
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
            sb.append("Season Rating: ").append(Utils.formatNumber(guildInfo.seasonRanks.get(String.valueOf(latestSeason)).rating)).append("\n");
            sb.append("Previous Rating: ").append(Utils.formatNumber(guildInfo.seasonRanks.get(String.valueOf(latestSeason - 1)).rating)).append("\n");
            sb.append("Wars: ").append(Utils.formatNumber(guildInfo.wars)).append("\n");
            double averageOnline = GuildActivity.getAverageOnline(AllGuilds.getGuild(guildPrefix).uuid(), 28, false);
            sb.append("Online Members: ").append(guildInfo.online).append(" / ").append(guildInfo.members.total).append(" avg: ").append(String.format("%.2f", averageOnline)).append("\n");

            double totalGuildExperience = Utils.getTotalGuildExperience(guildInfo.level, guildInfo.xpPercent);
            double totalXpPerDay = totalGuildExperience / Utils.timeSinceIso(guildInfo.created, ChronoUnit.DAYS);
            double totalWeeklyPlaytime = 0;
            for (String uuid : guildInfo.members.getAllMembers().keySet()) {
                PlaytimeHistoryList playtimeHistory = PlaytimeHistory.getPlaytimeHistory(uuid);
                totalWeeklyPlaytime += playtimeHistory.getAverage(4);
            }

            Map<String, GuildInfo.MemberInfo> allMembers = guildInfo.members.getAllMembers();

            List<GuildStatEntry> sortedEntries = allMembers.entrySet().stream()
                    .sorted(Comparator.comparingInt(entry -> entry.getValue().contributionRank))
                    .map(entry -> new GuildStatEntry(entry.getKey(), entry.getValue()))
                    .toList();


            sb.append("XP/day: ").append(Utils.formatNumber(totalXpPerDay)).append("/day").append("\n");
            sb.append("Total weekly playtime: ").append(Utils.formatNumber(totalWeeklyPlaytime)).append(" hours").append("\n");
            sb.append("\n");

            PageBuilder.PaginationState pageState = PageBuilder.PaginationManager.get(PaginationIds.GUILD_STATS.name());
            pageState.reset(sortedEntries);
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

        PlayerProfile playerData = MassGuild.getPlayerData(Collections.singleton(statEntry.uuid())).values().iterator().next();
        if (playerData.statusCode == 429) return "ratelimit hit :(";

        GuildInfo.MemberInfo guildMemberData = statEntry.guildMemberData();
        long joinedDaysAgo = Utils.timeSinceIso(guildMemberData.joined, ChronoUnit.DAYS);

        long xpPerDay;
        if (joinedDaysAgo > 0) {
            xpPerDay = guildMemberData.contributed / joinedDaysAgo;
        } else xpPerDay = guildMemberData.contributed;
        int rank = guildMemberData.contributionRank;

        StringBuilder sb = new StringBuilder();
        String guildRank = playerData.guild != null ? playerData.guild.rank : "not in a guild?";
        sb.append("**").append(rank).append(". ").append(Utils.escapeDiscordMarkdown(playerData.username)).append(" (").append(guildRank).append(")**");
        long lastSeen = Utils.timeSinceIso(playerData.lastJoin, ChronoUnit.SECONDS);
        if (lastSeen < 90 && !playerData.online) sb.append("Online in streamer\n");
        else if (lastSeen == Long.MAX_VALUE) sb.append("Data is Hidden\n");
        else sb.append(playerData.online ?
                "Online " + playerData.server + "\n"
                : "Offline, last seen " + Utils.formatTime(lastSeen, ChronoUnit.SECONDS) + "\n");
        sb.append(Utils.formatNumbersInString(String.valueOf(guildMemberData.contributed))).append(" XP (").append(Utils.formatNumber(xpPerDay)).append("/day)\n");
        sb.append("Joined ").append(joinedDaysAgo).append(" days ago\n");
        if (playerData.globalData != null) {
            sb.append("Wars: ").append(Utils.formatNumber(playerData.globalData.wars)).append(", ");
            Map<String, Integer> raids = playerData.globalData.raids.list;
            raids.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> sb.append(Utils.abbreviate(entry.getKey()))
                            .append(": ")
                            .append(entry.getValue())
                            .append(", "));

            sb.append("\n");
        }

        PlaytimeHistoryList playtimeHistory = PlaytimeHistory.getPlaytimeHistory(playerData.uuid);
        double average4w = playtimeHistory.getAverage(4);
        String average4wS = average4w > 0 ? String.format("%.2f", average4w) : "No access";
        sb.append(average4wS).append(" ");
        sb.append("hours per week (").append(String.format("%.2f", playtimeHistory.getAverage(1))).append(" since ")
                .append(Utils.getDiscordTimestamp(playtimeHistory.getAverageTimeSpan(1).getKey(), true)).append(")");
        sb.append("\n\n");

        return sb.toString();
    }

    // this could be optimized so much
    public static String notInDiscord(Guild guild) {
        List<Member> discordMembers = guild.getMembers();
        GuildInfo guildInfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
        Map<String, GuildInfo.MemberInfo> wynnMembers = guildInfo.members.getAllMembersByUsername();

        StringBuilder sb = new StringBuilder();

        for (String username : wynnMembers.keySet()) {
            boolean inDisct = false;
            boolean verified = false;
            for (Member member : discordMembers) {
                String discordName = member.getEffectiveName().toLowerCase();
                if (username.toLowerCase().equals(discordName)) {
                    inDisct = true;
                    if (RoleUtils.hasRole(member, Config.Roles.VerifiedRole)) {
                        verified = true;
                    }
                }
            }

            if (!inDisct) {
                sb.append(username).append(" is not in the discord (or is in discord with a different name)\n");
            } else if (!verified) {
                sb.append(username).append(" is in the discord but not verified\n");
            }
        }
        return sb.toString();
    }

    public static void notInDiscord(SlashCommandInteractionEvent event) {
        event.deferReply(false).queue(hook -> hook.editOriginalEmbeds(Utils.getEmbed("", notInDiscord(event.getGuild()))).queue());
    }

    public static void viewLastLogins(SlashCommandInteractionEvent event) {
        String guildPrefix = event.getOption("guild").getAsString();

        event.deferReply(false).addComponents(PageBuilder.getPaginationActionRow(PaginationIds.LAST_LOGINS)).queue(hook -> {

            GuildInfo guildInfo = ApiUtils.getGuildInfo(guildPrefix);
            if (guildInfo == null || guildInfo.members == null) {
                hook.editOriginal("failed").queue();
                return;
            }

            Map<String, GuildInfo.MemberInfo> allMembers = guildInfo.members.getAllMembers();

            List<LastLoginInfo> sortedMembers = new ArrayList<>();

            Set<PlayerDataShortened> memberData = Players.getAll(allMembers.keySet());

            for (PlayerDataShortened playerDataShortened : memberData) {
                if (playerDataShortened == null) {
                    continue;
                }
                String username = playerDataShortened.username;
                long lastJoined = Utils.timeSinceIso(playerDataShortened.lastJoined, ChronoUnit.DAYS);
                Utils.RankList rankOfMember = guildInfo.members.getRankOfMember(playerDataShortened.uuid);
                int highestLvl = playerDataShortened.highestLvl;

                sortedMembers.add(new LastLoginInfo(playerDataShortened.uuid, username, lastJoined, rankOfMember, highestLvl));
            }

            sortedMembers.sort(Comparator.comparingLong(LastLoginInfo::lastJoinedDays).reversed());

            PageBuilder.PaginationState pageState = PageBuilder.PaginationManager.get(PaginationIds.LAST_LOGINS.name());
            pageState.reset(sortedMembers);
            pageState.title = "[" + guildInfo.prefix + "] " + guildInfo.name + " Last Logins";

            EmbedBuilder embed = pageState.buildEmbedPage();

            if (embed == null) {
                hook.editOriginalEmbeds(Utils.getEmbed("well this is awkward", "something went wrong")).queue();
                return;
            }

            hook.editOriginalEmbeds(embed.build()).queue();
        });
    }

    public static String lastLoginsConverter(LastLoginInfo m) {
        return String.format("%s last joined %d days ago (%s, lvl %d)%n",
                Utils.escapeDiscordMarkdown(m.username()), m.lastJoinedDays(), m.rank(), m.highestLvl());
    }

    public static void getWarReport(SlashCommandInteractionEvent event) {
        event.deferReply(false).addComponents(PageBuilder.getPaginationActionRow(PaginationIds.WAR_REPORT)).queue(hook -> {

            GuildInfo guildInfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
            Map<String, PlaytimeHistoryList> playtimeHistoryForAll = PlaytimeHistory.getPlaytimeHistoryForAll(guildInfo.members.getAllMembers().keySet());

            List<Map.Entry<String, PlaytimeHistoryList>> entriesList = new ArrayList<>(playtimeHistoryForAll.entrySet());
            entriesList.sort((a, b) -> compareIncreases(a.getValue().getWarsReport(), b.getValue().getWarsReport(), "\\(1w ago\\): ([+-]?\\d+)"));

            // Build the sortedEntries list
            List<String> sortedEntries = new ArrayList<>();
            for (Map.Entry<String, PlaytimeHistoryList> entry : entriesList) {
                PlayerDataShortened playerDataShortened = Players.get(entry.getKey());
                String username = playerDataShortened == null ? "null" : playerDataShortened.username;
                String warsReport = entry.getValue().getWarsReport();

                sortedEntries.add("**" + username + "**\n" + warsReport + "\n\n");
            }

            PageBuilder.PaginationState pageState = PageBuilder.PaginationManager.get(PaginationIds.WAR_REPORT.name());
            pageState.reset(sortedEntries);

            EmbedBuilder embed = pageState.buildEmbedPage();

            if (embed == null) {
                hook.editOriginalEmbeds(Utils.getEmbed("well this is awkward", "something went wrong")).queue();
                return;
            }

            hook.editOriginalEmbeds(embed.build()).queue();

        });
    }
    private static int compareReports(String reportA, String reportB) {
        // Priority: 4w > 2w > 1w > total

        // 4w
        int cmp = compareIncreases(reportA, reportB, "\\(4w ago\\): ([+-]?\\d+)");
        if (cmp != 0) return cmp;

        // 2w
        cmp = compareIncreases(reportA, reportB, "\\(2w ago\\): ([+-]?\\d+)");
        if (cmp != 0) return cmp;

        // 1w
        cmp = compareIncreases(reportA, reportB, "\\(1w ago\\): ([+-]?\\d+)");
        if (cmp != 0) return cmp;

        // total
        return compareIncreases(reportA, reportB, "total wars: (\\d+)");
    }
    private static int compareIncreases(String reportA, String reportB, String regex) {
        int valA = extractOrZero(reportA, regex);
        int valB = extractOrZero(reportB, regex);

        // Only compare if at least one has non-zero
        if (valA != 0 || valB != 0) {
            return Integer.compare(valB, valA); // descending
        }
        return 0;
    }
    private static int extractOrZero(String report, String regex) {
        Matcher m = Pattern.compile(regex).matcher(report);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return 0;
    }

    public record GuildStatEntry(String uuid, GuildInfo.MemberInfo guildMemberData){}
    public record LastLoginInfo(
            String uuid,
            String username,
            long lastJoinedDays,
            Utils.RankList rank,
            int highestLvl
    ) {}

}
