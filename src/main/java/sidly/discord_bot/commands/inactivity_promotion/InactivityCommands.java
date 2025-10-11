package sidly.discord_bot.commands.inactivity_promotion;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.commands.GuildCommands;
import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.PlaytimeHistoryList;
import sidly.discord_bot.database.tables.*;
import sidly.discord_bot.page.PageBuilder;
import sidly.discord_bot.page.PaginationIds;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class InactivityCommands {
    private record RankThreshold(double inactiveDays, int averageWeeks) {}

    private static final Map<Utils.RankList, RankThreshold> thresholds = Map.of(
            Utils.RankList.Recruit,    new RankThreshold(10, 2),
            Utils.RankList.Recruiter,  new RankThreshold(10, 4),
            Utils.RankList.Captain,    new RankThreshold(16, 8),
            Utils.RankList.Strategist, new RankThreshold(30, 20),
            Utils.RankList.Chief,      new RankThreshold(365, 100),
            Utils.RankList.Owner,      new RankThreshold(Long.MAX_VALUE, 4)
    );


    public static void checkForInactivity(SlashCommandInteractionEvent event) {

        event.deferReply(false).addComponents(PageBuilder.getPaginationActionRow(PaginationIds.CHECK_INACTIVITY)).queue(hook -> {
            String notInDiscord = GuildCommands.notInDiscord(event.getGuild());

            GuildInfo guildinfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
            if (guildinfo == null || guildinfo.members == null) return;

            List<InactivityEntry> inactiveMembers = new ArrayList<>();
            List<String> membersNotInDiscord = new ArrayList<>();

            Map<String, GuildInfo.MemberInfo> allMembers = guildinfo.members.getAllMembers();
            int totalGuildMembers = guildinfo.members.total;
            int currentGuildMembers = allMembers.size();
            double averageOnline = GuildActivity.getAverageOnline(guildinfo.uuid, 28, false);
            double multiplier = (averageOnline > 5) ? 1.2 : 0.8;
            double usedPercent = (double) currentGuildMembers / (double) totalGuildMembers;
            multiplier = usedPercent > 0.9 ? multiplier + 0.3 : multiplier - 0.3;

            Map<String, PlaytimeHistoryList> playtimeHistoryForAll = PlaytimeHistory.getPlaytimeHistoryForAll(allMembers.keySet());

            for (Map.Entry<String, GuildInfo.MemberInfo> entry : allMembers.entrySet()) {
                String username = entry.getValue().username;

                if (notInDiscord.contains(username)) {
                    membersNotInDiscord.add(username);
                }

                Long inactiveException = InactivityExceptions.get(UuidMap.getMinecraftIdByUsername(username.toLowerCase()));
                if (inactiveException != null && inactiveException > System.currentTimeMillis()) continue;


                PlayerDataShortened playerDataShortened = Players.get(entry.getKey());
                if (playerDataShortened == null || playerDataShortened.lastJoined == null) continue;

                PlaytimeHistoryList playtimeHistory = playtimeHistoryForAll.get(playerDataShortened.uuid);
                if (playtimeHistory == null) {
                    playtimeHistory = new PlaytimeHistoryList(new ArrayList<>());
                    System.out.println("no playtime data for " + playerDataShortened.username);
                }

                Utils.RankList rankOfMember = guildinfo.members.getRankOfMember(entry.getKey());
                RankThreshold threshold = thresholds.get(rankOfMember);
                double inactiveThreshold = threshold.inactiveDays() / multiplier;
                double averagePlaytimeReq = 4 * multiplier;
                int averageWeeks = threshold.averageWeeks();
                double averagePlaytime = playtimeHistory.getAverage(averageWeeks);

                if (Utils.timeSinceIso(entry.getValue().joined, ChronoUnit.DAYS) < 8) {
                    inactiveThreshold = 5;
                    averagePlaytimeReq = 0;
                }

                double lastOnline = Utils.timeSinceIso(playerDataShortened.lastJoined, ChronoUnit.DAYS);

                if (averagePlaytime < averagePlaytimeReq || lastOnline > inactiveThreshold) {
                    inactiveMembers.add(new InactivityEntry(
                            username,
                            averagePlaytime,
                            averagePlaytimeReq,
                            Utils.getEpochTimeFromIso(playerDataShortened.lastJoined),
                            inactiveThreshold,
                            playtimeHistory.getAverageTimeSpan(averageWeeks),
                            rankOfMember,
                            Utils.formatTime(Utils.timeSinceIso(entry.getValue().joined, ChronoUnit.SECONDS), ChronoUnit.SECONDS)));
                }
            }

            PageBuilder.PaginationState pageState = PageBuilder.PaginationManager.get(PaginationIds.CHECK_INACTIVITY.name());
            pageState.reset(inactiveMembers);
            pageState.customData = membersNotInDiscord;

            EmbedBuilder embed = pageState.buildEmbedPage();

            if (embed == null) {
                hook.editOriginalEmbeds(Utils.getEmbed("well this is awkward", "something went wrong")).queue();
                return;
            }

            hook.editOriginalEmbeds(embed.build()).queue();
        });
    }

    @SuppressWarnings("unchecked")
    public static String inactivityEntryConverter(InactivityEntry entry) {
        PageBuilder.PaginationState pageState = PageBuilder.PaginationManager.get(PaginationIds.CHECK_INACTIVITY.name());
        List<String> membersNotInDiscord = (List<String>) pageState.customData;

        StringBuilder sb = new StringBuilder();
        sb.append("**").append(Utils.escapeDiscordMarkdown(entry.username)).append("** (");
        sb.append(entry.rank).append(") joined ").append(entry.joinedDaysAgo()).append(" ago");

        PlaytimeHistoryList playtimeHistory = PlaytimeHistory.getPlaytimeHistory(UuidMap.getMinecraftIdByUsername(entry.username()));

        String playtime = entry.averagePlaytime > 0 ? Utils.formatNumber(entry.averagePlaytime) : "No access";

        sb.append("\nplaytime ").append(playtime).append(" / ")
                .append(Utils.formatNumber(entry.averagePlaytimeReq))
                .append(" (").append(Utils.formatNumber(playtimeHistory.getAverage(1))).append(" since ")
                .append(Utils.getDiscordTimestamp(playtimeHistory.getAverageTimeSpan(1).getKey(), true)).append(")\n");
        sb.append("last online ").append(Utils.getDiscordTimestamp(entry.lastOnline(), true)).append(" / ")
                .append(Utils.formatNumber(entry.inactiveThreashhold)).append(" days\n");
        sb.append("data from ").append(Utils.getDiscordTimestamp(entry.timeSpan().getKey(), true)).append(" to ")
                .append(Utils.getDiscordTimestamp(entry.timeSpan().getValue(), true)).append("\n\n");

        return sb.toString();
    }

    public static void getAveragePlaytime(SlashCommandInteractionEvent event) {

        event.deferReply(false).addComponents(PageBuilder.getPaginationActionRow(PaginationIds.AVERAGE_PLAYTIME)).queue(hook -> {
            PageBuilder.PaginationState pageState = PageBuilder.PaginationManager.get(PaginationIds.AVERAGE_PLAYTIME.name());
            pageState.reset(PlaytimeHistory.getSortedPlaytimeReport());
            EmbedBuilder embed = pageState.buildEmbedPage();

            if (embed == null) {
                event.reply("no ppl").setEphemeral(true).queue();
                return;
            }

            hook.editOriginalEmbeds(embed.build()).queue();
        });
    }

    public static void addException(SlashCommandInteractionEvent event) {
        User user = event.getOption("user").getAsUser();
        int days = event.getOption("length").getAsInt();

        long timestampExp = TimeUnit.DAYS.toMillis(days) + System.currentTimeMillis();

        String uuid = UuidMap.getMinecraftIdByUsername(event.getGuild().getMember(user).getEffectiveName().toLowerCase());
        if (uuid == null) {
            event.reply("user not found in database").setEphemeral(true).queue();
            return;
        }
        InactivityExceptions.add(uuid, timestampExp);

        event.reply("added an inactivity exception to \n" + uuid + "\n expires " + Utils.getDiscordTimestamp(timestampExp, true)).setEphemeral(true).queue();
    }

    public static void getExceptions(SlashCommandInteractionEvent event) {
        event.deferReply(true).queue(hook -> {
            Map<String, Long> inactiveExc = InactivityExceptions.getAll();
            Map<String, Long> promotionExc = PromotionExceptions.getAll();
            long currentTime = System.currentTimeMillis();
            Guild guild = event.getGuild();

            StringBuilder sb = new StringBuilder();

            sb.append("Inactivity Exceptions\n");
            for (Map.Entry<String, Long> entry : inactiveExc.entrySet()) {
                if (entry.getValue() > currentTime) {
                    String discordId = UuidMap.getDiscordIdByMinecraftId(entry.getKey());
                    sb.append(guild.getMemberById(discordId).getAsMention()).append(" expires ").append(Utils.getDiscordTimestamp(entry.getValue(), true)).append("\n");
                }
            }
            sb.append("\nPromotion Exceptions\n");
            for (Map.Entry<String, Long> entry : promotionExc.entrySet()) {
                if (entry.getValue() > currentTime) {
                    String discordId = UuidMap.getDiscordIdByMinecraftId(entry.getKey());
                    sb.append(guild.getMemberById(discordId).getAsMention()).append(" expires ").append(Utils.getDiscordTimestamp(entry.getValue(), true)).append("\n");
                }
            }

            hook.editOriginalEmbeds(Utils.getEmbed("", sb.toString())).queue();
        });
    }

    public record InactivityEntry(
            String username,
            double averagePlaytime,
            double averagePlaytimeReq,
            long lastOnline,
            double inactiveThreashhold,
            AbstractMap.SimpleEntry<Long, Long> timeSpan,
            Utils.RankList rank,
            String joinedDaysAgo)
    {}
}
