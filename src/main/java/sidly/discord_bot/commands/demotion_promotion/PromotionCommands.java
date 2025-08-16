package sidly.discord_bot.commands.demotion_promotion;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.MainEntrypoint;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.api.PlayerProfile;
import sidly.discord_bot.database.Database;
import sidly.discord_bot.database.GuildDataActivity;
import sidly.discord_bot.database.PlayerDataShortened;
import sidly.discord_bot.database.PlaytimeHistoryList;

import java.awt.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PromotionCommands {

    public static void addRequirement(SlashCommandInteractionEvent event) {
        Utils.RankList rank = Utils.RankList.valueOf(event.getOption("rank").getAsString());
        RequirementType type = RequirementType.valueOf(event.getOption("requirement").getAsString());
        int value = event.getOption("value").getAsInt();
        boolean required = event.getOption("required").getAsBoolean();

        Requirement requirement = new Requirement(type, value, required);

        RequirementList reqList = ConfigManager.getDatabaseInstance().promotionRequirements.computeIfAbsent(rank, k -> new RequirementList());
        reqList.addRequirement(requirement);
        ConfigManager.saveDatabase();


        event.reply("added " + requirement + " to " + rank.name()).queue();
    }

    public static void getRequirements(SlashCommandInteractionEvent event) {

        if (ConfigManager.getDatabaseInstance().promotionRequirements == null || ConfigManager.getDatabaseInstance().promotionRequirements.isEmpty()) {
            event.reply("No promotion requirements found.").setEphemeral(true).queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Promotion Requirements")
                .setColor(Color.CYAN);

        for (Map.Entry<Utils.RankList, RequirementList> entry : ConfigManager.getDatabaseInstance().promotionRequirements.entrySet()) {
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

        RequirementList requirements = ConfigManager.getDatabaseInstance().promotionRequirements.get(rank);
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
        ConfigManager.saveDatabase();

        event.reply(reply).queue();
    }

    public static void setPromotionOptionalRequirement(SlashCommandInteractionEvent event) {
        Utils.RankList rank = Utils.RankList.valueOf(event.getOption("rank").getAsString());
        int value = event.getOption("value").getAsInt();
        ConfigManager.getDatabaseInstance().promotionRequirements.get(rank).setOptionalQuantityRequired(value);
        ConfigManager.saveDatabase();

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

        PlayerDataShortened playerDataShortened = ConfigManager.getDatabaseInstance().allPlayers.get(username);

        if (playerDataShortened == null || guildInfo == null || guildInfo.members == null) return "null error ❌";


        String uuid = playerDataShortened.uuid;
        Utils.RankList playerRank = guildInfo.members.getRankOfMember(uuid);
        if (playerRank == Utils.RankList.Owner || playerRank == Utils.RankList.Chief) {
            return username + " cant be promoted error ❌";
        }

        GuildInfo.MemberInfo guildMemberInfo = guildInfo.members.getMemberInfo(uuid);

        if (guildMemberInfo == null) return "guild member info is null error ❌";

        // And get promotion requirements map
        Map<Utils.RankList, RequirementList> promotionRequirements = ConfigManager.getDatabaseInstance().promotionRequirements;
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
        int optionalCounter = 0;
        for (Requirement req : requirementList.getRequirements()) {
            Integer value = req.getValue();
            switch (req.getType()) {
                case XPContributed:
                    long playerXPContribution = guildMemberInfo.contributed;
                    if (playerXPContribution >= value) {
                        sb.append("✅ ");
                        if (!req.isRequired()){
                            optionalCounter++;
                        }
                    } else if (req.isRequired()){
                        sb.append("❌");
                    } else sb.append(":no_entry_sign:");
                    sb.append(" XPContributed: ").append(playerXPContribution).append(" / ").append(value).append('\n');
                    break;
                case TopXpContributor:
                    int playerXPContributionRank = guildMemberInfo.contributionRank;
                    if (playerXPContributionRank <= value) {
                        sb.append("✅ ");
                        if (!req.isRequired()){
                            optionalCounter++;
                        }
                    } else if (req.isRequired()){
                        sb.append("❌");
                    } else sb.append(":no_entry_sign:");
                    sb.append(" TopXpContributor: ").append(playerXPContributionRank).append(" / ").append(value).append('\n');
                    break;
                case Level:
                    int playerLevel = playerDataShortened.highestLvl;
                    if (playerLevel >= value) {
                        sb.append("✅ ");
                        if (!req.isRequired()){
                            optionalCounter++;
                        }
                    } else if (req.isRequired()){
                        sb.append("❌");
                    } else sb.append(":no_entry_sign:");
                    sb.append(" Level: ").append(playerLevel).append(" / ").append(value).append('\n');
                    break;
                case DaysInGuild:
                    long daysInGuild = Utils.daysSinceIso(guildMemberInfo.joined);
                    if (daysInGuild >= value) {
                        sb.append("✅ ");
                        if (!req.isRequired()){
                            optionalCounter++;
                        }
                    } else if (req.isRequired()){
                        sb.append("❌");
                    } else sb.append(":no_entry_sign:");
                    sb.append(" DaysInGuild: ").append(daysInGuild).append(" / ").append(value).append('\n');
                    break;
                case GuildWars:
                    int guildWars = playerDataShortened.wars;
                    if (guildWars >= value) {
                        sb.append("✅ ");
                        if (!req.isRequired()){
                            optionalCounter++;
                        }

                    } else if (req.isRequired()){
                        sb.append("❌");
                    } else sb.append(":no_entry_sign:");
                    sb.append(" GuildWars: ").append(guildWars).append(" / ").append(value).append('\n');
                    break;
                case WarBuild:
                    boolean dps = Utils.hasRole(member, ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialDpsRole));
                    boolean tank = Utils.hasRole(member, ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialTankRole));
                    boolean healer = Utils.hasRole(member, ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialHealerRole));
                    boolean solo = Utils.hasRole(member, ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialSoloRole));

                    int buildCount = 0;
                    if (dps) buildCount++;
                    if (tank) buildCount++;
                    if (healer) buildCount++;
                    if (solo) buildCount++;

                    if (buildCount >= value) {
                        sb.append("✅ ");
                        if (!req.isRequired()){
                            optionalCounter++;
                        }
                    } else if (req.isRequired()){
                        sb.append("❌");
                    } else sb.append(":no_entry_sign:");
                    sb.append(" WarBuild: ").append(buildCount).append(" / ").append(value).append('\n');
                    break;
                case WeeklyPlaytime:
                    double average = 0;
                    PlaytimeHistoryList playtimeHistoryList = ConfigManager.getDatabaseInstance().playtimeHistory.get(username);
                    if (playtimeHistoryList != null && playtimeHistoryList.getAverage(10) > value) {
                        average = playtimeHistoryList.getAverage(10);
                        sb.append("✅ ");
                        if (!req.isRequired()) {
                            optionalCounter++;
                        }
                    } else if (req.isRequired()){
                        sb.append("❌");
                    } else sb.append(":no_entry_sign:");
                    sb.append(" WeeklyPlaytime: ").append(average).append(" / ").append(value).append('\n');
                    break;
                case Eco:
                    boolean eco = Utils.hasRole(member, ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialEcoRole));
                    int ecoInt = eco ? 1 : 0;
                    if (ecoInt >= value) {
                        sb.append("✅ ");
                        if (!req.isRequired()) {
                            optionalCounter++;
                        }
                    } else if (req.isRequired()){
                        sb.append("❌");
                    } else sb.append(":no_entry_sign:");
                    sb.append(" Eco: ").append(ecoInt).append(" / ").append(value).append('\n');
                    break;
                case Verified:
                    boolean verified = Utils.hasRole(member, ConfigManager.getConfigInstance().roles.get(Config.Roles.VerifiedRole));
                    int verifiedInt = verified ? 1 : 0;
                    if (verifiedInt >= value) {
                        sb.append("✅ ");
                        if (!req.isRequired()) {
                            optionalCounter++;
                        }
                    } else if (req.isRequired()){
                        sb.append("❌");
                    } else sb.append(":no_entry_sign:");
                    sb.append(" Verified: ").append(verifiedInt).append(" / ").append(value).append('\n');
                    break;
                default:
                    break;
            }
        }

        if (optionalCounter >= requiredOptionalRequirements){
            sb.append("✅ ");
        }else sb.append("❌");
        sb.append(optionalCounter).append(" of ").append(requiredOptionalRequirements).append(" optional requirements are met").append('\n');

        return sb.toString();
    }

    public static void checkForPromotions(SlashCommandInteractionEvent event) {
        GuildInfo guildInfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));

        if (guildInfo == null || guildInfo.members == null) {
            event.reply("guild null").setEphemeral(true).queue();
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, GuildInfo. MemberInfo> entry : guildInfo.members.getAllMembers().entrySet()) {
            String progress = checkPromotionProgress(entry.getValue().username, guildInfo);
            if (!progress.contains("❌")) {
                Utils.RankList rank = guildInfo.members.getRankOfMember(entry.getKey());
                Object promoteTo = Utils.RankList.values()[rank.ordinal() - 1];
                sb.append("**");
                sb.append(entry.getValue().username).append(" from ").append(rank.name()).append(" to ").append(promoteTo).append("**\n");
                sb.append(progress).append("\n");
            }
        }

        MessageEmbed embed = Utils.getEmbed("Promotions", sb.toString());
        event.replyEmbeds(embed).queue();
    }
}
