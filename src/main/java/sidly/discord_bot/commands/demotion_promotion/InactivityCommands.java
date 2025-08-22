package sidly.discord_bot.commands.demotion_promotion;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.PlaytimeHistoryList;
import sidly.discord_bot.database.records.GuildAverages;
import sidly.discord_bot.database.tables.GuildActivity;
import sidly.discord_bot.database.tables.Players;
import sidly.discord_bot.database.tables.PlaytimeHistory;
import sidly.discord_bot.page.PageBuilder;
import sidly.discord_bot.page.PaginationIds;

import java.awt.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;

public class InactivityCommands {
    public static void checkForInactivity(SlashCommandInteractionEvent event) {

        GuildInfo guildinfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
        if (guildinfo == null || guildinfo.members == null) return;

        List<InactivityEntry> inactiveMembers = new ArrayList<>();

        Map<String, GuildInfo.MemberInfo> allMembers = guildinfo.members.getAllMembers();
        for (Map.Entry<String, GuildInfo.MemberInfo> entry : allMembers.entrySet()) {
            String username = entry.getValue().username;
            PlayerDataShortened playerDataShortened = Players.get(entry.getKey());
            Utils.RankList rankOfMember = guildinfo.members.getRankOfMember(entry.getKey());

            if (playerDataShortened != null && playerDataShortened.lastJoined != null) {
                long lastJoin = Utils.timeSinceIso(playerDataShortened.lastJoined, ChronoUnit.DAYS);

                double inactiveThreashhold;
                double averagePlaytimeReq = 4;
                int averageWeeks = switch (rankOfMember) {
                    case Utils.RankList.Recruit -> {
                        inactiveThreashhold = 10;
                        yield 2;
                    }
                    case Utils.RankList.Recruiter -> {
                        inactiveThreashhold = 10;
                        yield 4;
                    }
                    case Utils.RankList.Captain -> {
                        inactiveThreashhold = 16;
                        yield 8;
                    }
                    case Utils.RankList.Strategist -> {
                        inactiveThreashhold = 30;
                        yield 20;
                    }
                    case Utils.RankList.Chief -> {
                        inactiveThreashhold = 365;
                        yield 100;
                    }
                    case Utils.RankList.Owner -> {
                        inactiveThreashhold = Long.MAX_VALUE;
                        yield 4;
                    }
                };

                PlaytimeHistoryList playtimeHistory = PlaytimeHistory.getPlaytimeHistory(playerDataShortened.uuid);
                double averagePlaytime = playtimeHistory.getAverage(averageWeeks);

                double averageOnline = GuildActivity.getAverageOnline(guildinfo.uuid, 28, false);
                double multiplier = (averageOnline > 5) ? 1.2 : 0.8;

                int totalGuildMembers = guildinfo.members.total;
                int currentGuildMembers = guildinfo.members.getAllMembers().keySet().size();

                double usedPercent = (double) currentGuildMembers / (double) totalGuildMembers;

                multiplier = usedPercent > 0.9 ? multiplier + 0.3 : multiplier - 0.3;

                averagePlaytimeReq *= multiplier;
                inactiveThreashhold /= multiplier;

                if (Utils.timeSinceIso(entry.getValue().joined, ChronoUnit.DAYS) < 8) inactiveThreashhold = 5;

                double lastOnline = Utils.timeSinceIso(playerDataShortened.lastJoined, ChronoUnit.DAYS);

                if (averagePlaytime < averagePlaytimeReq || lastOnline > inactiveThreashhold) {
                    inactiveMembers.add(new InactivityEntry(username, averagePlaytime, averagePlaytimeReq, lastOnline, inactiveThreashhold));
                }
            }
        }

        StringBuilder sb = new StringBuilder();
        for (InactivityEntry entry : inactiveMembers) {
            sb.append("**").append(entry.username).append("**\n");
            sb.append("playtime ").append(Utils.formatNumber(entry.averagePlaytime)).append(" / ").append(Utils.formatNumber(entry.averagePlaytimeReq)).append("\n");
            sb.append("last online ").append(Utils.formatNumber(entry.lastOnline)).append(" / ").append(Utils.formatNumber(entry.inactiveThreashhold)).append("\n\n");
        }

        MessageEmbed embed = Utils.getEmbed("Inactive Players", sb.toString());
        event.replyEmbeds(embed).queue();
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

    private record InactivityEntry(String username, double averagePlaytime, double averagePlaytimeReq, double lastOnline, double inactiveThreashhold){}
}
