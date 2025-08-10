package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.api.PlayerProfile;

import java.awt.Color;
import java.util.concurrent.atomic.AtomicReference;

public class VerificationCommands {
    public static void verify(SlashCommandInteractionEvent event) {
        String username = event.getOption("username").getAsString();

        // only allow one discord account per mc account
        if (ConfigManager.getDatabaseInstance().verifiedMembers.containsKey(username)) {
            event.reply("someone is already verified as that user").setEphemeral(true).queue();
            return;
        }

        PlayerProfile playerData = ApiUtils.getPlayerData(username);
        if (playerData == null){
            event.reply("playerData was null").setEphemeral(true).queue();
            return;
        }
        GuildInfo guildInfo = ApiUtils.getGuildInfo(playerData.guild.prefix);
        Member member = event.getMember();
        if (member == null){
            event.reply("member was null").setEphemeral(true).queue();
            return;
        }
        Utils.RankList rankOfMember = playerData.getRank();
        boolean requireConfirmation = switch (rankOfMember) {
            case Utils.RankList.Owner -> true;
            case Utils.RankList.Chief -> true;
            case Utils.RankList.Strategist -> true;
            case Utils.RankList.Captain -> true;
            default -> false;
        };

        StringBuilder sb = new StringBuilder();
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.BLUE);
        embed.setTitle("verification");

        if (requireConfirmation) {
            embed.setDescription("waiting for a human to confirm you are who you say you are");
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();

            // Send to mod channel
            TextChannel modChannel = event.getGuild().getTextChannelById(
                    ConfigManager.getSetting(Config.Settings.ModerationChannel)
            );

            if (modChannel != null) {
                String baseId = event.getUser().getId() + "|" + username;

                Button confirmButton = Button.success("verify_confirm_" + baseId, "Confirm");
                Button denyButton = Button.danger("verify_deny_" + baseId, "Deny");

                EmbedBuilder modEmbed = new EmbedBuilder()
                        .setColor(Color.ORANGE)
                        .setTitle("Verification Request")
                        .setDescription(event.getUser().getAsMention() + " claims to be **" + username + "**");

                modChannel.sendMessageEmbeds(modEmbed.build())
                        .setActionRow(confirmButton, denyButton)
                        .queue();
            }
        } else {
            embed.setDescription("done");
            event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            completeVerification(member, username, member.getGuild());
        }
    }

    public static void completeVerification(Member member, String username, Guild guild) {
        Role verifiedRole = guild.getRoleById(ConfigManager.getSetting(Config.Settings.VerifiedRole));
        String prefix = ConfigManager.getSetting(Config.Settings.YourGuildPrefix);

        // add the verified role
        if (verifiedRole != null) {
            ConfigManager.getDatabaseInstance().verifiedMembers.put(username, member);
            guild.addRoleToMember(member, verifiedRole).queue();
        }

        if (prefix.equals(ConfigManager.getSetting(Config.Settings.YourGuildPrefix))) {

            // set there nick to their ign
            guild.modifyNickname(member, username).queue(
                    success -> {
                        guild.retrieveMember(UserSnowflake.fromId(member.getId())).queue(VerificationCommands::updatePlayer);
                    });

        } else {
            // there a member of a different guild

            // set there nick to their ign + guild prefix
            guild.modifyNickname(member, username + "[" + prefix + "]").queue(
                    success -> {
                        guild.retrieveMember(UserSnowflake.fromId(member.getId())).queue(VerificationCommands::updatePlayer);
                    });
        }

    }

    public static void updatePlayer(Member member){
        String nickname = member.getNickname();
        if (nickname == null || nickname.endsWith("]") || !Utils.hasRole(member, ConfigManager.getSetting(Config.Settings.VerifiedRole))){
            // not in guild
            return; // return without ever making an api request
        }
        PlayerProfile playerData = ApiUtils.getPlayerData(nickname);
        if (playerData == null) return;
        GuildInfo guildInfo = ApiUtils.getGuildInfo(playerData.guild.prefix);
        updatePlayer(member, playerData, guildInfo);
    }
    public static void updatePlayer(Member member, PlayerProfile playerData, GuildInfo guildInfo){
        StringBuilder sb = new StringBuilder();

        // add there in game guild rank
        Utils.RankList rankOfMember = playerData.getRank();
        Role rankRole = switch (rankOfMember) {
            case Utils.RankList.Owner -> member.getGuild().getRoleById(ConfigManager.getSetting(Config.Settings.OwnerRole));
            case Utils.RankList.Chief -> member.getGuild().getRoleById(ConfigManager.getSetting(Config.Settings.ChiefRole));
            case Utils.RankList.Strategist -> member.getGuild().getRoleById(ConfigManager.getSetting(Config.Settings.StrategistRole));
            case Utils.RankList.Captain -> member.getGuild().getRoleById(ConfigManager.getSetting(Config.Settings.CaptainRole));
            case Utils.RankList.Recruiter -> member.getGuild().getRoleById(ConfigManager.getSetting(Config.Settings.RecruiterRole));
            case Utils.RankList.Recruit -> member.getGuild().getRoleById(ConfigManager.getSetting(Config.Settings.RecruitRole));
        };
        if (rankRole != null) {
            if (!Utils.hasRole(member, rankRole.getId())) {
                member.getGuild().addRoleToMember(member, rankRole).queue();
                sb.append("added the rank role ").append(rankRole.getAsMention()).append('\n');
            }
        }

        // add their support rank
        Role supportRank = switch (playerData.supportRank) {
            case "vip" -> member.getGuild().getRoleById(ConfigManager.getSetting(Config.Settings.VipRole));
            case "vipplus" -> member.getGuild().getRoleById(ConfigManager.getSetting(Config.Settings.VipPlusRole));
            case "hero" -> member.getGuild().getRoleById(ConfigManager.getSetting(Config.Settings.HeroRole));
            case "heroplus" -> member.getGuild().getRoleById(ConfigManager.getSetting(Config.Settings.HeroPlusRole));
            case "champion" -> member.getGuild().getRoleById(ConfigManager.getSetting(Config.Settings.ChampionRole));
            default -> null;
        };
        if (supportRank != null) {
            if (!Utils.hasRole(member, supportRank.getId())) {
                member.getGuild().addRoleToMember(member, supportRank).queue();
                sb.append("added the support rank role ").append(supportRank.getAsMention()).append('\n');
            }
        }

        // add the member role
        Role memberRole = member.getGuild().getRoleById(ConfigManager.getSetting(Config.Settings.MemberRole));
        if (memberRole != null) {
            if (!Utils.hasRole(member, memberRole.getId())) {
                member.getGuild().addRoleToMember(member, memberRole).queue();
                sb.append("added the member role ").append(memberRole.getAsMention()).append('\n');
            }
        }

        // check for 100% content comp
        Role OneHundredPercentContentCompletionRole = member.getGuild().getRoleById(ConfigManager.getSetting(Config.Settings.OneHundredPercentContentCompletionRole));
        if (playerData.getHighestContentCompletion() >= 1128 && OneHundredPercentContentCompletionRole != null) {
            if (!Utils.hasRole(member, OneHundredPercentContentCompletionRole.getId())) {
                member.getGuild().addRoleToMember(member, OneHundredPercentContentCompletionRole).queue();
                sb.append("added the 100% completion role ").append(OneHundredPercentContentCompletionRole.getAsMention()).append('\n');
            }
        }

        // Send to mod channel
        TextChannel modChannel = member.getGuild().getTextChannelById(
                ConfigManager.getSetting(Config.Settings.ModerationChannel)
        );
        if (modChannel != null) {
            EmbedBuilder modEmbed = new EmbedBuilder()
                    .setColor(Color.ORANGE)
                    .setTitle("Updates Roles For " + member.getUser().getAsMention())
                    .setDescription(sb.toString());

            modChannel.sendMessageEmbeds(modEmbed.build()).queue();
        }
    }
}
