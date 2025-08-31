package sidly.discord_bot.commands.inactivity_promotion;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.*;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.PlaytimeHistoryList;
import sidly.discord_bot.database.tables.*;
import sidly.discord_bot.page.PageBuilder;
import sidly.discord_bot.page.PaginationIds;

import java.awt.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PromotionCommands {

    public static void addRequirement(SlashCommandInteractionEvent event) {
        Utils.RankList rank = Utils.RankList.valueOf(event.getOption("rank").getAsString());
        RequirementType type = RequirementType.valueOf(event.getOption("requirement").getAsString());
        int value = event.getOption("value").getAsInt();
        boolean required = event.getOption("required").getAsBoolean();

        Requirement requirement = new Requirement(type, value, required);

        RequirementList reqList = ConfigManager.getConfigInstance().promotionRequirements.computeIfAbsent(rank, k -> new RequirementList());
        reqList.addRequirement(requirement);


        event.reply("added " + requirement + " to " + rank.name()).queue();
    }

    public static void getRequirements(SlashCommandInteractionEvent event) {

        if (ConfigManager.getConfigInstance().promotionRequirements == null || ConfigManager.getConfigInstance().promotionRequirements.isEmpty()) {
            event.reply("No promotion requirements found.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Promotion Requirements")
                .setColor(Color.CYAN);

        for (Map.Entry<Utils.RankList, RequirementList> entry : ConfigManager.getConfigInstance().promotionRequirements.entrySet()) {
            Utils.RankList rank = entry.getKey();
            RequirementList requirements = entry.getValue();

            StringBuilder reqText = new StringBuilder();
            if (requirements == null || requirements.isEmpty()) {
                reqText.append("No requirements set.");
            } else {
                for (Requirement req : requirements.getRequirements()) {
                    reqText
                            .append("- ")
                            .append(req.getType().name())
                            .append(": ")
                            .append(req.getValue())
                            .append(req.isRequired() ? "" : " (Optional)")
                            .append("\n");

                }
                if (requirements.getOptionalQuantityRequired() > 0) {
                    reqText.append("- Optional Required: ");
                    reqText.append(requirements.getOptionalQuantityRequired());
                }
            }

            embed.addField(rank.name(), reqText.toString(), false);
        }

        event.replyEmbeds(embed.build()).queue();
    }

    public static void removeRequirement(SlashCommandInteractionEvent event) {
        Utils.RankList rank = Utils.RankList.valueOf(event.getOption("rank").getAsString());
        RequirementType type = RequirementType.valueOf(event.getOption("requirement").getAsString());
        String reply;

        RequirementList requirements = ConfigManager.getConfigInstance().promotionRequirements.get(rank);
        if (requirements == null || requirements.isEmpty()) {
            reply = "no requirements were found";
        } else {
            Iterator<Requirement> iterator = requirements.getRequirements().iterator();
            boolean removedAny = false;
            while (iterator.hasNext()) {
                Requirement req = iterator.next();
                if (req.getType() == type) {
                    iterator.remove();
                    removedAny = true;
                }
            }
            reply = removedAny ? "Removed all matching requirements: " : "Requirement not found: ";
            reply += rank + " " + type;
        }

        event.reply(reply).queue();
    }

    public static void setPromotionOptionalRequirement(SlashCommandInteractionEvent event) {
        Utils.RankList rank = Utils.RankList.valueOf(event.getOption("rank").getAsString());
        int value = event.getOption("value").getAsInt();
        ConfigManager.getConfigInstance().promotionRequirements.get(rank).setOptionalQuantityRequired(value);

        event.reply("set " + rank + " to " + value).queue();
    }

    public static void checkPromotionProgress(SlashCommandInteractionEvent event) {
        User user = event.getOption("user").getAsUser();
        String username = event.getGuild().getMember(user).getEffectiveName();

        GuildInfo guildInfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
        String s = checkPromotionProgress(username, guildInfo);

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Promotion Progress for " + username)
                .setColor(Color.CYAN)
                .setDescription(s);

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();

    }

    public static String checkPromotionProgress(String username, GuildInfo guildInfo) {

        PlayerDataShortened playerDataShortened = Players.get(UuidMap.getMinecraftIdByUsername(username.toLowerCase()));

        if (playerDataShortened == null || guildInfo == null || guildInfo.members == null) return "null error ❌";

        String uuid = playerDataShortened.uuid;

        Long exception = PromotionExceptions.get(uuid);
        if (exception != null && exception > System.currentTimeMillis()) {
            return username + "❌ has a promotions exception that expires " + Utils.getDiscordTimestamp(exception, true);
        }

        Utils.RankList playerRank = guildInfo.members.getRankOfMember(uuid);
        if (playerRank == Utils.RankList.Owner || playerRank == Utils.RankList.Chief) {
            return username + " cant be promoted error ❌";
        }

        GuildInfo.MemberInfo guildMemberInfo = guildInfo.members.getMemberInfo(uuid);

        if (guildMemberInfo == null) return "guild member info is null error ❌";

        // get promotion requirements map
        Map<Utils.RankList, RequirementList> promotionRequirements = ConfigManager.getConfigInstance().promotionRequirements;
        RequirementList requirementList = null;
        int currentIndex = playerRank.ordinal();
        if (currentIndex > 0) { // Make sure there is a rank above
            Utils.RankList rankAbove = Utils.RankList.values()[currentIndex - 1];
            requirementList = promotionRequirements.get(rankAbove);
        }


        Guild yourDiscordServer = MainEntrypoint.jda.getGuildById(ConfigManager.getConfigInstance().other.get(Config.Settings.YourDiscordServerId));
        Member member = null;
        if (yourDiscordServer != null) {
            List<Member> membersByEffectiveName = yourDiscordServer.getMembersByEffectiveName(username, true);
            if (membersByEffectiveName.size() == 1){
                member = membersByEffectiveName.getFirst();
            }
        }

        if (member == null) return "username not in discord error ❌";

        // Loop through all RequirementTypes to check progress:

        StringBuilder sb = new StringBuilder();
        int requiredOptionalRequirements = requirementList.getOptionalQuantityRequired();

        Map<Boolean, List<Requirement>> partitioned = requirementList.getRequirements().stream()
                .collect(Collectors.partitioningBy(Requirement::isRequired));

        List<Requirement> required = partitioned.get(true).stream()
                .sorted(Comparator.comparingInt(r -> r.getType().ordinal()))
                .toList();

        List<Requirement> optional = partitioned.get(false).stream()
                .sorted(Comparator.comparingInt(r -> r.getType().ordinal()))
                .toList();


        sb.append("Required:\n");
        for (Requirement req : required) {
            sb.append(checkRequirement(req, guildMemberInfo, playerDataShortened, member));
        }

        StringBuilder optionalOutputB = new StringBuilder();
        for (Requirement req : optional) {
            optionalOutputB.append(checkRequirement(req, guildMemberInfo, playerDataShortened, member));
        }
        String optionalOutput = optionalOutputB.toString();
        long optionalCounter = optionalOutput.chars().filter(ch -> ch == '✅').count();

        if (optionalCounter >= requiredOptionalRequirements){
            sb.append("✅ ");
        }else sb.append("❌");
        sb.append("Optional Requirements: ").append(optionalCounter).append(" / ").append(requiredOptionalRequirements).append('\n');

        if (!optional.isEmpty()) {
            sb.append("-# Optional: (at least ").append(requiredOptionalRequirements).append(" must be met)\n");
        }
        sb.append(optionalOutput);

        return sb.toString();
    }

    public static String checkRequirement(Requirement req, GuildInfo.MemberInfo guildMemberInfo, PlayerDataShortened playerDataShortened, Member member){
        StringBuilder sb = new StringBuilder();
        Integer requirementCount = req.getValue();
        if (!req.isRequired()) sb.append("-# ");
        switch (req.getType()) {
            case XPContributed:
                long playerXPContribution = guildMemberInfo.contributed;
                sb.append(getSymbol(playerXPContribution, requirementCount));
                sb.append(" XPContributed: ").append(Utils.formatNumber(playerXPContribution)).append(" / ").append(requirementCount).append('\n');
                break;
            case TopXpContributor:
                int playerXPContributionRank = guildMemberInfo.contributionRank;
                sb.append(getSymbol(requirementCount, playerXPContributionRank));
                sb.append(" TopXpContributor: ").append(playerXPContributionRank).append(" / ").append(requirementCount).append('\n');
                break;
            case Level:
                int playerLevel = playerDataShortened.highestLvl;
                sb.append(getSymbol(playerLevel, requirementCount));
                sb.append(" Level: ").append(playerLevel).append(" / ").append(requirementCount).append('\n');
                break;
            case DaysInGuild:
                long daysInGuild = Utils.timeSinceIso(guildMemberInfo.joined, ChronoUnit.DAYS);
                sb.append(getSymbol((int) daysInGuild, requirementCount));
                sb.append(" DaysInGuild: ").append(daysInGuild).append(" / ").append(requirementCount).append('\n');
                break;
            case GuildWars:
                int guildWars = playerDataShortened.wars;
                sb.append(getSymbol(guildWars, requirementCount));
                sb.append(" GuildWars: ").append(Utils.formatNumber(guildWars)).append(" / ").append(requirementCount).append('\n');
                break;
            case WarBuild:
                boolean dps = RoleUtils.hasRole(member, ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialDpsRole));
                boolean tank = RoleUtils.hasRole(member, ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialTankRole));
                boolean healer = RoleUtils.hasRole(member, ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialHealerRole));
                boolean solo = RoleUtils.hasRole(member, ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialSoloRole));

                int buildCount = 0;
                if (dps) buildCount++;
                if (tank) buildCount++;
                if (healer) buildCount++;
                if (solo) buildCount++;

                sb.append(getSymbol(buildCount, requirementCount));
                sb.append(" WarBuild: ").append(buildCount).append(" / ").append(requirementCount).append('\n');
                break;
            case WeeklyPlaytime:
                double average = 0;
                PlaytimeHistoryList playtimeHistoryList = PlaytimeHistory.getPlaytimeHistory(playerDataShortened.uuid);
                if (playtimeHistoryList.getAverage(10) > requirementCount) {
                    average = playtimeHistoryList.getAverage(10);
                    sb.append("✅ ");
                } else if (req.isRequired()){
                    sb.append("❌");
                } else sb.append(":no_entry_sign:");
                sb.append(" WeeklyPlaytime: ").append(Utils.formatNumber(average)).append(" / ").append(requirementCount).append('\n');
                break;
            case Eco:
                boolean eco = RoleUtils.hasRole(member, ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialEcoRole));
                int ecoInt = eco ? 1 : 0;
                sb.append(getSymbol(ecoInt, requirementCount));
                sb.append(" Eco: ").append(ecoInt).append(" / ").append(requirementCount).append('\n');
                break;
            case Verified:
                boolean verified = RoleUtils.hasRole(member, ConfigManager.getConfigInstance().roles.get(Config.Roles.VerifiedRole));
                int verifiedInt = verified ? 1 : 0;
                sb.append(getSymbol(verifiedInt, requirementCount));
                sb.append(" Verified: ").append(verifiedInt).append(" / ").append(requirementCount).append('\n');
                break;
            default:
                break;
        }
        return sb.toString();
    }

    public static String getSymbol(int value, int requirement) {
        if (value >= requirement) {
            return "✅ ";
        } else return "❌";
    }

    public static String getSymbol(long value, int requirement) {
        if (value >= requirement) {
            return "✅ ";
        } else return "❌";
    }

    public static void checkForPromotions(SlashCommandInteractionEvent event) {
        PageBuilder.PaginationState pageState = PageBuilder.PaginationManager.get(PaginationIds.PROMOTIONS.name());

        event.deferReply(false).addComponents(PageBuilder.getPaginationActionRow(PaginationIds.PROMOTIONS)).queue(hook -> {
            GuildInfo guildInfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
            if (guildInfo == null) return;
            List<PromotionEntry> entries = guildInfo.members.getAllMembers().entrySet().stream()
                    .map(entry -> {
                        String progress = checkPromotionProgress(entry.getValue().username, guildInfo);
                        return new AbstractMap.SimpleEntry<>(entry, progress);
                    })
                    .filter(e -> !e.getValue().split("-#")[0].contains("❌"))
                    .map(e -> new PromotionEntry(e.getKey().getKey(), e.getKey().getValue().username, guildInfo, e.getValue()))
                    .toList();

            pageState.reset(entries);


            EmbedBuilder embed = pageState.buildEmbedPage();

            if (embed == null) {
                hook.editOriginalEmbeds(Utils.getEmbed("No Promotions", "there is no-one good enough for the next rank")).queue();
                return;
            }

            hook.editOriginalEmbeds(embed.build()).queue();
        });
    }

    public static String promotionConverter(PromotionEntry info) {
        StringBuilder sb = new StringBuilder();
        Utils.RankList rank = info.guildInfo.members.getRankOfMember(info.uuid);
        Utils.RankList promoteTo = Utils.RankList.values()[rank.ordinal() - 1];
        sb.append("**");
        sb.append(info.username).append("** is eligible for **").append(promoteTo).append("** rank\n");
        sb.append(info.progress).append("\n");
        return sb.toString();
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
        PromotionExceptions.add(uuid, timestampExp);

        event.reply("added a promotion exception to \n" + uuid + "\n expires " + Utils.getDiscordTimestamp(timestampExp, true)).setEphemeral(true).queue();
    }

    public record PromotionEntry (String uuid, String username, GuildInfo guildInfo, String progress){}
}
