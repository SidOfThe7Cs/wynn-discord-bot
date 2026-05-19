package sidly.discord_bot.new_guild_endpoint;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import sidly.discord_bot.*;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.api.sub.MemberInfo;
import sidly.discord_bot.page.PageBuilder;
import sidly.discord_bot.page.PaginationIds;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

public class Weekly {
    public static void getCommandResponse(SlashCommandInteractionEvent event) {

        event.deferReply(false).addComponents(PageBuilder.getPaginationActionRow(PaginationIds.WEEKLY_OBJ)).queue(hook -> {

            User user = Optional.ofNullable(event.getOption("user"))
                    .map(OptionMapping::getAsUser)
                    .orElse(null);
            String username;
            if (user != null) username = event.getGuild().getMember(user).getEffectiveName();
            else {
                username = Optional.ofNullable(event.getOption("username"))
                        .map(OptionMapping::getAsString)
                        .orElse(null);
            }

            GuildInfo guildInfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix), true);
            if (guildInfo == null) return;
            Map<String, MemberInfo> allMembersByUsername = guildInfo.members.getAllMembersByUsername();
            List<MemberInfo> sortedEntries = allMembersByUsername.values().stream()
                    .filter(m -> username == null || username.isEmpty() || username.equalsIgnoreCase(m.username))
                    .sorted(Comparator.comparingInt(m -> m.contributionRank))
                    .toList();


            PageBuilder.PaginationState pageState = PageBuilder.PaginationManager.get(PaginationIds.WEEKLY_OBJ.name());
            pageState.reset(sortedEntries);
            pageState.customData = "Resets in " + getTimeUntilReset() + "\n";

            EmbedBuilder embed = pageState.buildEmbedPage();

            if (embed == null) {
                hook.editOriginalEmbeds(Utils.getEmbed("well this is awkward", "something went wrong")).queue();
                return;
            }

            hook.editOriginalEmbeds(embed.build()).queue();
        });
    }

    public static String weeklyConverter(MemberInfo memberInfo) {
        String maybe = memberInfo.weekly.completed ? "" : " NOT";
        return getSymbol(memberInfo.weekly) + " " +
                memberInfo.username +
                " has" + maybe + " completed there guild obj" +
                " (Streak " + memberInfo.weekly.streak + ")\n";
    }

    public static String getSymbol(sidly.discord_bot.api.sub.Weekly weekly) {
        if (weekly.completed) {
            return "✅";
        } else return "❌";
    }

    public static void buttonClicked(ButtonInteractionEvent event) {
        Member member = event.getMember();
        if (member == null) {
            event.reply("cmd must be used in a server").setEphemeral(true).queue();
            return;
        }
        boolean isChief = RoleUtils.hasRole(member, Config.Roles.ChiefRole);
        boolean isOwner = RoleUtils.hasRole(member, Config.Roles.OwnerRole);
        if (!isChief && !isOwner) {
            event.reply("you do not have perms to do this").setEphemeral(true).queue();
        }

        GuildInfo guildInfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix), true);
        if (guildInfo != null) {
            List<Member> toPing = guildInfo.members.getAllMembers().values().stream()
                    .filter(m -> !m.weekly.completed)
                    .map(m -> {
                        Guild yourDiscordServer = MainEntrypoint.jda.getGuildById(ConfigManager.getConfigInstance().other.get(Config.Settings.YourDiscordServerId));
                        if (yourDiscordServer != null) {
                            List<Member> membersByEffectiveName = yourDiscordServer.getMembersByEffectiveName(m.username, true);
                            if (membersByEffectiveName.size() == 1) {
                                return membersByEffectiveName.getFirst();
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .toList();
            // TODO ping button
        }

        event.reply("failed :(").setEphemeral(true).queue();
    }

    public static String getTimeUntilReset() {
        ZoneId estZone = ZoneId.of("America/New_York");
        ZonedDateTime now = ZonedDateTime.now(estZone);

        // Calculate next Sunday at 11 PM (today if it's Sunday before 11 PM)
        ZonedDateTime nextReset = now.getDayOfWeek() == DayOfWeek.SUNDAY && now.getHour() < 23
                ? now.withHour(23).withMinute(0).withSecond(0).withNano(0)
                : now.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
                  .withHour(23).withMinute(0).withSecond(0).withNano(0);

        return Utils.getDiscordTimestamp(nextReset.toInstant().getEpochSecond() * 1000, true);
    }
}
