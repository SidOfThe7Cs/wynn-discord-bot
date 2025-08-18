package sidly.discord_bot.commands.demotion_promotion;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.MainEntrypoint;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.database.GuildDataActivity;
import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.PlaytimeHistoryList;
import sidly.discord_bot.database.tables.Players;
import sidly.discord_bot.database.tables.PlaytimeHistory;
import sidly.discord_bot.page.PageBuilder;
import sidly.discord_bot.page.PaginationIds;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class InactivityCommands {
    public static void checkForInactivity(SlashCommandInteractionEvent event) {
        int days = Optional.ofNullable(event.getOption("inactive_threshold"))
                .map(OptionMapping::getAsInt)
                .orElse(-1);

        GuildInfo guildinfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
        if (guildinfo == null || guildinfo.members == null) return;

        Map<String, Integer> lastJoinMap = new HashMap<>();
        Map<String, Long> lastUpdatedMap = new HashMap<>();

        Map<String, GuildInfo.MemberInfo> allMembers = guildinfo.members.getAllMembers();
        for (Map.Entry<String, GuildInfo.MemberInfo> entry : allMembers.entrySet()) {
            String username = entry.getValue().username;
            PlayerDataShortened playerDataShortened = Players.get(entry.getKey());

            if (playerDataShortened != null && playerDataShortened.lastJoined != null) {
                long lastJoin = Utils.daysSinceIso(playerDataShortened.lastJoined);
                lastJoinMap.put(username, (int) lastJoin);

                lastUpdatedMap.put(username, playerDataShortened.lastModified);
            }
        }

        List<Map.Entry<String, Integer>> result;

        if (days == -1) {
            // Top 10 entries
            result = lastJoinMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(10)
                    .toList();
        } else {
            // All entries with value >= days
            result = lastJoinMap.entrySet().stream()
                    .filter(entry -> entry.getValue() >= days)
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .toList();
        }


        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.CYAN);
        embed.setTitle("Inactive Players");
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, Integer> entry : result) {
            Long l = lastUpdatedMap.get(entry.getKey());
            sb.append("**").append(Utils.escapeDiscordMarkdown(entry.getKey()));
            sb.append("** last joined ").append(entry.getValue()).append(" days ago • last updated ")
                    .append(Utils.getDiscordTimestamp(l, true)).append("\n");
        }

        String description = sb.toString();
        if (description.length() > 4096) {
            description = description.substring(0, 4096);
            embed.setFooter("Character limit hit");
        }
        embed.setDescription(description);
        event.replyEmbeds(embed.build()).queue();
    }

    public static void getAveragePlaytime(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = buildAveragePlaytimePage();

        if (embed == null) {
            event.reply("no ppl").setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(embed.build())
                .addComponents(Utils.getPaginationActionRow(PaginationIds.AVERAGE_PLAYTIME))
                .queue();

    }

    public static EmbedBuilder buildAveragePlaytimePage() {
        PageBuilder.PaginationState paginationState = PageBuilder.PaginationManager.get(PaginationIds.AVERAGE_PLAYTIME.name());

        List<String> entries = PlaytimeHistory.getSortedPlaytimeReport();

        return PageBuilder.buildEmbedPage(entries, paginationState.currentPage, 30, "Player, 10weeklinearavg, 1weekavg, 5weekavg, 20weekavg, alltimeavg");
    }
}
