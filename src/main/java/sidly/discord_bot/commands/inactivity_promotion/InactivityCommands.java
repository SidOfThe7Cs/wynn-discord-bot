package sidly.discord_bot.commands.inactivity_promotion;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
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
    public static void checkForInactivity(SlashCommandInteractionEvent event) {

        event.deferReply(false).addComponents(PageBuilder.getPaginationActionRow(PaginationIds.CHECK_INACTIVITY)).queue(hook -> {

            GuildInfo guildinfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
            if (guildinfo == null || guildinfo.members == null) return;

            List<InactivityEntry> inactiveMembers = new ArrayList<>();

            Map<String, GuildInfo.MemberInfo> allMembers = guildinfo.members.getAllMembers();
            for (Map.Entry<String, GuildInfo.MemberInfo> entry : allMembers.entrySet()) {
                String username = entry.getValue().username;

                Long inactiveException = InactivityExceptions.get(UuidMap.getMinecraftIdByUsername(username.toLowerCase()));
                if (inactiveException != null && inactiveException > System.currentTimeMillis()) continue;


                PlayerDataShortened playerDataShortened = Players.get(entry.getKey());
                Utils.RankList rankOfMember = guildinfo.members.getRankOfMember(entry.getKey());

                if (playerDataShortened != null && playerDataShortened.lastJoined != null) {

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

                    if (Utils.timeSinceIso(entry.getValue().joined, ChronoUnit.DAYS) < 8) {
                        inactiveThreashhold = 5;
                        averagePlaytimeReq = 0;
                    }

                    double lastOnline = Utils.timeSinceIso(playerDataShortened.lastJoined, ChronoUnit.DAYS);

                    if (averagePlaytime < averagePlaytimeReq || lastOnline > inactiveThreashhold) {
                        inactiveMembers.add(new InactivityEntry(username, averagePlaytime, averagePlaytimeReq, Utils.timeSinceIso(playerDataShortened.lastJoined, ChronoUnit.MILLIS), inactiveThreashhold, playtimeHistory.getAverageTimeSpan(averageWeeks)));
                    }
                }
            }

            PageBuilder.PaginationState pageState = PageBuilder.PaginationManager.get(PaginationIds.CHECK_INACTIVITY.name());
            pageState.reset(inactiveMembers);

            EmbedBuilder embed = pageState.buildEmbedPage();

            if (embed == null) {
                hook.editOriginalEmbeds(Utils.getEmbed("well this is awkward", "something went wrong")).queue();
                return;
            }

            hook.editOriginalEmbeds(embed.build()).queue();
        });
    }

    public static String inactiveityEntryConverter(InactivityEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(Utils.escapeDiscordMarkdown(entry.username)).append("**\n");
        sb.append("playtime ").append(Utils.formatNumber(entry.averagePlaytime)).append(" / ").append(Utils.formatNumber(entry.averagePlaytimeReq)).append("\n");
        sb.append("data from ").append(Utils.getDiscordTimestamp(entry.timeSpan().getValue(), true)).append(" to ").append(Utils.getDiscordTimestamp(entry.timeSpan().getValue(), true)).append("\n");
        sb.append("last online ").append(Utils.getDiscordTimestamp(entry.lastOnline(), true)).append(" / ").append(Utils.formatNumber(entry.inactiveThreashhold)).append(" days\n\n");
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

    public record InactivityEntry(String username, double averagePlaytime, double averagePlaytimeReq, long lastOnline, double inactiveThreashhold, AbstractMap.SimpleEntry<Long, Long> timeSpan){}
}
