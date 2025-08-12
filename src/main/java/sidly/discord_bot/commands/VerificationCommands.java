package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.api.PlayerProfile;

import java.awt.Color;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VerificationCommands {
    public final static int MAX_CONTENT_COMPLETION = 1128;

    public static void verify(SlashCommandInteractionEvent event) {
        String username = event.getOption("username").getAsString();

        Member member = event.getMember();
        if (member == null){
            event.reply("member was null").setEphemeral(true).queue();
            return;
        }
        // only allow one verification per discord account
        if (ConfigManager.getDatabaseInstance().verifiedMembersByDiscordId.containsKey(member.getId())) {
            event.reply("you are already verified please ask a moderator to remove it if you need to change it").setEphemeral(true).queue();
            return;
        }
        // only allow one discord account per mc account
        if (ConfigManager.getDatabaseInstance().verifiedMembersByIgn.containsKey(username)) {
            event.reply("someone is already verified as that user").setEphemeral(true).queue();
            return;
        }
        PlayerProfile playerData = ApiUtils.getPlayerData(username);
        if (playerData == null){
            event.reply("playerData was null").setEphemeral(true).queue();
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

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.BLUE);
        embed.setTitle("Verification");

        if (requireConfirmation) {
            embed.setDescription("Waiting for a moderator to confirm your verification request.");

            // Send to mod channel
            TextChannel modChannel = event.getGuild().getTextChannelById(
                    ConfigManager.getConfigInstance().channels.get(Config.Channels.ModerationChannel)
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
            embed.setDescription("Verification Complete");
            completeVerification(member, username, member.getGuild());
        }

        if (event.getGuild().getOwnerIdLong() == member.getIdLong()){
            embed.addField("","You are the server owner so I cant change your nickname please make sure it matches your ign",false);
        }
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    public static void completeVerification(Member member, String username, Guild guild) {
        Role verifiedRole = guild.getRoleById(ConfigManager.getConfigInstance().roles.get(Config.Roles.VerifiedRole));
        String prefix = ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix);

        if (verifiedRole != null) {
            // Put in the maps first
            ConfigManager.getDatabaseInstance().verifiedMembersByIgn.put(username, member.getId());
            ConfigManager.getDatabaseInstance().verifiedMembersByDiscordId.put(member.getId(), username);
            ConfigManager.save();

            guild.addRoleToMember(member, verifiedRole).queue(success -> {

                // Don't change nickname if they are owner
                if (guild.getOwnerIdLong() == member.getIdLong()) {
                    guild.retrieveMember(UserSnowflake.fromId(member.getId())).queue(VerificationCommands::updatePlayer);
                    return;
                }

                // if they are in your guild set there nickname
                if (prefix.equals(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix))) {
                    guild.modifyNickname(member, username).queue(
                            nickSuccess -> guild.retrieveMember(UserSnowflake.fromId(member.getId())).queue(VerificationCommands::updatePlayer)
                    );
                } else { // if not set nickname to name + prefix
                    guild.modifyNickname(member, username + "[" + prefix + "]").queue(
                            nickSuccess -> guild.retrieveMember(UserSnowflake.fromId(member.getId())).queue(VerificationCommands::updatePlayer)
                    );
                }
            });
        }
    }


    public static void updatePlayer(Member member){
        //check if there verified
        String verifiedRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.VerifiedRole);
        if (!Utils.hasRole(member, verifiedRoleId)){
            System.out.println("user is not verified");
            return;
        }

        String nickname = member.getEffectiveName().split("\\[")[0];
        PlayerProfile playerData = ApiUtils.getPlayerData(nickname);
        if (playerData == null) return;

        StringBuilder sb = new StringBuilder();
        sb.append("**").append(member.getAsMention()).append("**\n");
        int changedCounter = 0;

        // add their in-game guild rank
        Utils.RankList rankOfMember = playerData.getRank();
        // All rank role IDs from config
        Set<String> allRankRoleIds = Stream.of(
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.OwnerRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.ChiefRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.StrategistRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.CaptainRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.RecruiterRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.RecruitRole)
                )
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());


        // Determine the correct rank role ID for the member
        String rankRoleId = null;
        switch (rankOfMember) {
            case Owner -> rankRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.OwnerRole);
            case Chief -> rankRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.ChiefRole);
            case Strategist -> rankRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.StrategistRole);
            case Captain -> rankRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.CaptainRole);
            case Recruiter -> rankRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.RecruiterRole);
            case Recruit -> rankRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.RecruitRole);
        }
        if (rankRoleId == null || rankRoleId.isEmpty()) {
            sb.append("Failed to get role ID for rank ").append(rankOfMember).append("\n");
        } else {
            Guild guild = member.getGuild();
            // Remove all other rank roles from member
            for (String roleId : allRankRoleIds) {
                if (!roleId.equals(rankRoleId) && Utils.hasRole(member, roleId)) {
                    Role roleToRemove = guild.getRoleById(roleId);
                    if (roleToRemove != null) {
                        guild.removeRoleFromMember(member, roleToRemove).queue();
                        changedCounter++;
                        sb.append("Removed rank role ").append(roleToRemove.getAsMention()).append('\n');
                    }
                }
            }
            // Add the correct rank role if not already present
            if (!Utils.hasRole(member, rankRoleId)) {
                Role rankRole = guild.getRoleById(rankRoleId);
                if (rankRole != null) {
                    guild.addRoleToMember(member, rankRole).queue();
                    changedCounter++;
                    sb.append("Added the rank role ").append(rankRole.getAsMention()).append('\n');
                } else {
                    sb.append("Role not found in guild for ID: ").append(rankRoleId).append('\n');
                }
            }
        }



        // add their support rank
        // All support rank role IDs from config
        Set<String> allSupportRoleIds = Stream.of(
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.VipRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.VipPlusRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.HeroRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.HeroPlusRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.ChampionRole)
                )
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());

        if (playerData.supportRank != null) {
            // Determine the correct support role ID for the member
            String supportRoleId = switch (playerData.supportRank.toLowerCase()) {
                case "vip" -> ConfigManager.getConfigInstance().roles.get(Config.Roles.VipRole);
                case "vipplus" -> ConfigManager.getConfigInstance().roles.get(Config.Roles.VipPlusRole);
                case "hero" -> ConfigManager.getConfigInstance().roles.get(Config.Roles.HeroRole);
                case "heroplus" -> ConfigManager.getConfigInstance().roles.get(Config.Roles.HeroPlusRole);
                case "champion" -> ConfigManager.getConfigInstance().roles.get(Config.Roles.ChampionRole);
                default -> null;
            };
            if (supportRoleId == null || supportRoleId.isEmpty()) {
                sb.append("No support rank role found for support rank '").append(playerData.supportRank).append("'.\n");
            } else {
                Guild guild = member.getGuild();
                // Remove all other support rank roles from member
                for (String roleId : allSupportRoleIds) {
                    if (!roleId.equals(supportRoleId) && Utils.hasRole(member, roleId)) {
                        Role roleToRemove = guild.getRoleById(roleId);
                        if (roleToRemove != null) {
                            guild.removeRoleFromMember(member, roleToRemove).queue();
                            changedCounter++;
                            sb.append("Removed support rank role ").append(roleToRemove.getAsMention()).append('\n');
                        }
                    }
                }
                // Add the correct support rank role if not already present
                if (!Utils.hasRole(member, supportRoleId)) {
                    Role supportRank = guild.getRoleById(supportRoleId);
                    if (supportRank != null) {
                        guild.addRoleToMember(member, supportRank).queue();
                        changedCounter++;
                        sb.append("Added the support rank role ").append(supportRank.getAsMention()).append('\n');
                    } else {
                        sb.append("Support rank role not found in guild for ID: ").append(supportRoleId).append('\n');
                    }
                }
            }
        }
        boolean isOwner = member.getGuild().getOwnerIdLong() == member.getIdLong();

        // add / remove the member role and set nickname
        String memberRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.MemberRole);
        if (memberRoleId == null || memberRoleId.isEmpty()) {
            sb.append("No member role ID configured.\n");
        } else {
            Role memberRole = member.getGuild().getRoleById(memberRoleId);
            if (memberRole != null) {
                boolean hasMemberRole = Utils.hasRole(member, memberRole.getId());
                if (playerData.guild.prefix.equals(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix))){
                    // they are in your guild
                    if (!hasMemberRole){
                        changedCounter++;
                        member.getGuild().addRoleToMember(member, memberRole).queue();
                        sb.append("Added the member role ").append(memberRole.getAsMention()).append('\n');
                    }
                    if(!isOwner) member.modifyNickname(nickname).queue(); // make sure there nick doesnt have a guild tag after it
                }else {
                    // in a different guild
                    if (hasMemberRole){
                        changedCounter++;
                        member.getGuild().removeRoleFromMember(member, memberRole).queue();
                        sb.append("removed the member role ").append(memberRole.getAsMention()).append('\n');
                    }
                    if(!isOwner) member.getGuild().modifyNickname(member, nickname + "[" + playerData.guild.prefix + "]").queue(); // make sure there nick has a guild tag after it
                }
            } else {
                sb.append("Member role not found in guild for ID: ").append(memberRoleId).append('\n');
            }
        }


        // check for 100% content comp
        String contentCompletionRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.OneHundredPercentContentCompletionRole);
        if (contentCompletionRoleId == null || contentCompletionRoleId.isEmpty()) {
            sb.append("No 100% completion role ID configured.\n");
        } else {
            Role OneHundredPercentContentCompletionRole = member.getGuild().getRoleById(contentCompletionRoleId);
            if (OneHundredPercentContentCompletionRole != null) {
                if (playerData.getHighestContentCompletion() >= MAX_CONTENT_COMPLETION) {
                    if (!Utils.hasRole(member, OneHundredPercentContentCompletionRole.getId())) {
                        member.getGuild().addRoleToMember(member, OneHundredPercentContentCompletionRole).queue();
                        changedCounter++;
                        sb.append("Added the 100% completion role ").append(OneHundredPercentContentCompletionRole.getAsMention()).append('\n');
                    }
                }
            } else {
                sb.append("100% completion role not found in guild for ID: ").append(contentCompletionRoleId).append('\n');
            }
        }


        // add their highest lvl role
        int highestLvl = playerData.getHighestLevel();
        Config.LvlRoles matchedRole = Config.LvlRoles.Lvl1Role; // default
        if (highestLvl >= 106) {
            matchedRole = Config.LvlRoles.Lvl106Role;
        } else {
            int roundedLvl = (highestLvl <= 1) ? 1 : ((highestLvl + 4) / 5) * 5;
            String enumName = "Lvl" + roundedLvl + "Role";
            matchedRole = Config.LvlRoles.valueOf(enumName);
        }

        String lvlRoleId = ConfigManager.getConfigInstance().lvlRoles.get(matchedRole);
        if (lvlRoleId == null || lvlRoleId.isEmpty()) {
            sb.append("Failed to get role ID for ").append(matchedRole).append("\n");
        } else {
            Role lvlRole = member.getGuild().getRoleById(lvlRoleId);
            if (lvlRole == null) {
                sb.append("Role not found in guild for ID: ").append(lvlRoleId).append('\n');
            } else {
                // Remove all other lvl roles
                for (Config.LvlRoles lvl : Config.LvlRoles.values()) {
                    if (lvl != matchedRole) {
                        String otherRoleId = ConfigManager.getConfigInstance().lvlRoles.get(lvl);
                        if (otherRoleId != null && !otherRoleId.isEmpty() && Utils.hasRole(member, otherRoleId)) {
                            Role otherRole = member.getGuild().getRoleById(otherRoleId);
                            if (otherRole != null) {
                                member.getGuild().removeRoleFromMember(member, otherRole).queue();
                                changedCounter++;
                                sb.append("Removed lvlRole role ").append(otherRole.getAsMention()).append('\n');
                            }
                        }
                    }
                }

                // Add the correct lvl role if they don't have it yet
                if (!Utils.hasRole(member, lvlRole.getId())) {
                    member.getGuild().addRoleToMember(member, lvlRole).queue();
                    changedCounter++;
                    sb.append("Added the lvlRole role ").append(lvlRole.getAsMention()).append('\n');
                }
            }
        }




        // Send to mod channel
        TextChannel modChannel = member.getGuild().getTextChannelById(
                ConfigManager.getConfigInstance().channels.get(Config.Channels.ModerationChannel)
        );
        if (modChannel != null && changedCounter > 0) {
            EmbedBuilder modEmbed = new EmbedBuilder()
                    .setColor(Color.ORANGE)
                    .setTitle("Updates Roles For")
                    .setDescription(sb.toString());

            modChannel.sendMessageEmbeds(modEmbed.build()).queue();
        }
    }

    public static void removeVerification(SlashCommandInteractionEvent event) {
        String userId = event.getOption("user_id").getAsString();
        ConfigManager.getDatabaseInstance().removeVerification(userId);

        // remove verified role
        Role verifiedRole = event.getGuild().getRoleById(ConfigManager.getConfigInstance().roles.get(Config.Roles.VerifiedRole));
        event.getGuild().removeRoleFromMember(UserSnowflake.fromId(userId), verifiedRole).queue();
        ConfigManager.save();

        event.reply("removed verification for " + userId).setEphemeral(true).queue();
    }

    public static void updateRoles(SlashCommandInteractionEvent event) {
        String id = event.getOption("user_id").getAsString();
        event.getGuild().retrieveMemberById(id).queue(member -> {
            updatePlayer(member);
            event.reply("updated " + member.getAsMention()).setEphemeral(true).queue();
        }, failure -> {
            event.reply("User not found in this guild!").setEphemeral(true).queue();
        });
    }

}
