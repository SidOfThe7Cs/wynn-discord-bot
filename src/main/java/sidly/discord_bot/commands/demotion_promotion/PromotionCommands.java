package sidly.discord_bot.commands.demotion_promotion;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.api.PlayerProfile;

import java.awt.*;
import java.util.Iterator;
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
        ConfigManager.save();


        event.reply("added " + requirement + " to " + rank.name()).setEphemeral(true).queue();
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
            reply = removedAny ? "Removed all matching requirements." : "Requirement not found";
        }
        ConfigManager.save();

        event.reply(reply).setEphemeral(true).queue();
    }

    public static void setPromotionOptionalRequirement(SlashCommandInteractionEvent event) {
        Utils.RankList rank = Utils.RankList.valueOf(event.getOption("rank").getAsString());
        int value = event.getOption("value").getAsInt();
        ConfigManager.getDatabaseInstance().promotionRequirements.get(rank).setOptionalQuantityRequired(value);
        ConfigManager.save();

        event.reply("set " + rank + " to " + value).setEphemeral(true).queue();
    }

    public static void checkPromotionProgress(SlashCommandInteractionEvent event) {
        String username = event.getOption("username").getAsString();

        PlayerProfile playerData = ApiUtils.getPlayerData(username);
        GuildInfo guildInfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
        if (playerData == null) return;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Promotion Progress for " + username)
                .setColor(Color.CYAN);


        String uuid = playerData.uuid;
        Utils.RankList playerRank = guildInfo.members.getRankOfMember(uuid);
        if (playerRank == Utils.RankList.Owner || playerRank == Utils.RankList.Chief) {
            event.reply(username + " cant be promoted").setEphemeral(true).queue();
            return;
        }

        GuildInfo.MemberInfo guildMemberInfo = guildInfo.members.getMemberInfo(uuid);

        // And get promotion requirements map
        Map<Utils.RankList, RequirementList> promotionRequirements = ConfigManager.getDatabaseInstance().promotionRequirements;
        RequirementList requirementList = null;
        int currentIndex = playerRank.ordinal();
        if (currentIndex > 0) { // Make sure there is a rank above
            Utils.RankList rankAbove = Utils.RankList.values()[currentIndex - 1];
            requirementList = promotionRequirements.get(rankAbove);
        }


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
                    } else sb.append("❌");
                    sb.append(" XPContributed: " + playerXPContribution + " / " + value + '\n');
                    break;
                case TopXpContributor:
                    int playerXPContributionRank = guildMemberInfo.contributionRank;
                    if (playerXPContributionRank <= value) {
                        sb.append("✅ ");
                        if (!req.isRequired()){
                            optionalCounter++;
                        }
                    } else sb.append("❌");
                    sb.append(" TopXpContributor: " + playerXPContributionRank + " / " + value + '\n');
                    break;
                case Level:
                    int playerLevel = playerData.getHighestLevel();
                    if (playerLevel >= value) {
                        sb.append("✅ ");
                        if (!req.isRequired()){
                            optionalCounter++;
                        }
                    } else sb.append("❌");
                    sb.append(" Level: " + playerLevel + " / " + value + '\n');
                    break;
                case DaysInGuild:
                    long daysInGuild = Utils.daysSinceIso(guildMemberInfo.joined);
                    if (daysInGuild >= value) {
                        sb.append("✅ ");
                        if (!req.isRequired()){
                            optionalCounter++;
                        }
                    } else sb.append("❌");
                    sb.append(" DaysInGuild: " + daysInGuild + " / " + value + '\n');
                    break;
                case GuildWars:
                    int guildWars = playerData.globalData.wars;
                    if (guildWars >= value) {
                        sb.append("✅ ");
                        if (!req.isRequired()){
                            optionalCounter++;
                        }
                    } else sb.append("❌");
                    sb.append(" GuildWars: " + guildWars + " / " + value + '\n');
                    break;
                case WarBuild:
                    sb.append(" WarBuild: " + "not implemented yet" + " / " + value + '\n');
                    break;
                case WeeklyPlaytime:
                    sb.append(" WeeklyPlaytime: " + "not implemented yet" + " / " + value + '\n');
                    break;
                case Eco:
                    sb.append(" Eco: " + "not implemented yet" + " / " + value + '\n');
                    break;
                case Verified:
                    sb.append(" Verified: " + "not implemented yet" + " / " + value + '\n');
                    break;
                default:
                    break;
            }
        }

        if (optionalCounter >= requiredOptionalRequirements){
            sb.append("✅ ");
        }else sb.append("❌");
        sb.append(optionalCounter + " of " + requiredOptionalRequirements + " optional requirements are met" + '\n');

        embed.setDescription(sb.toString());
        event.replyEmbeds(embed.build()).queue();
    }
}
