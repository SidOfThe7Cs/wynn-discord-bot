package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.PlayerProfile;

import java.awt.Color;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class VerificationCommands {

    public static void verify(SlashCommandInteractionEvent event) {
        String username = event.getOption("username").getAsString();

        Member member = event.getMember();
        if (member == null){
            event.reply("member was null").setEphemeral(true).queue();
            return;
        }
        // remove there previous verification
        if (ConfigManager.getDatabaseInstance().verifiedMembersByDiscordId.containsKey(member.getId())) {
            ConfigManager.getDatabaseInstance().removeVerification(member.getId());
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

        event.deferReply(true).queue(hook -> {
            if (!requireConfirmation) {
                CompletableFuture<String> stringCompletableFuture = completeVerification(member, username, member.getGuild());
                stringCompletableFuture.thenAccept(result -> {
                    // result is the String returned from completeVerification
                    embed.setDescription("Verification complete: \n" + result);
                    hook.editOriginalEmbeds(embed.build()).queue();
                });
            } else{
                embed.setDescription("Waiting for a moderator to accept your verification \n");
                hook.editOriginalEmbeds(embed.build()).queue();
            }
        });


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

        }

        if (event.getGuild().getOwnerIdLong() == member.getIdLong()){
            embed.addField("","You are the server owner so I cant change your nickname please make sure it matches your ign",false);
        }

    }

    public static CompletableFuture<String> completeVerification(Member member, String username, Guild guild) {
        CompletableFuture<String> future = new CompletableFuture<>();
        Role verifiedRole = Utils.getRoleFromGuild(guild, ConfigManager.getConfigInstance().roles.get(Config.Roles.VerifiedRole));
        String prefix = ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix);

        if (verifiedRole == null) {
            future.complete("verified role is null");
            return future;
        }

        ConfigManager.getDatabaseInstance().verifiedMembersByIgn.put(username, member.getId());
        ConfigManager.getDatabaseInstance().verifiedMembersByDiscordId.put(member.getId(), username);
        ConfigManager.save();

        guild.addRoleToMember(member, verifiedRole).queue(success -> {
            Runnable runUpdate = () ->
                    guild.retrieveMember(UserSnowflake.fromId(member.getId())).queue(m ->
                            future.complete(VerificationCommands.updatePlayer(m))
                    );

            // is there owner dont change nick
            if (guild.getOwnerIdLong() == member.getIdLong()) {
                runUpdate.run();
                return;
            }

            String newNick = prefix.equals(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix))
                    ? username // if in your guild
                    : username + " [" + prefix + "]"; // if different guild
            guild.modifyNickname(member, newNick).queue(nickSuccess -> runUpdate.run());
        });

        return future;
    }




    public static String updatePlayer(Member member) {
        // check if there verified
        String verifiedRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.VerifiedRole);
        if (!Utils.hasRole(member, verifiedRoleId)) {
            return member.getAsMention() + " is not verified";
        }

        String nickname = member.getEffectiveName().split("\\[")[0].trim();
        PlayerProfile playerData = ApiUtils.getPlayerData(nickname);
        if (playerData == null) return "api failed";

        StringBuilder sb = new StringBuilder();
        sb.append("**Updates Roles For** ").append(member.getAsMention()).append("\n");

        boolean isOwner = member.getGuild().getOwnerIdLong() == member.getIdLong();
        boolean isMember;
        if (playerData.guild == null){
            isMember = false;
        } else isMember = playerData.guild.prefix.equals(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
        // add / remove the member role and set nickname
        String memberRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.MemberRole);
        if (memberRoleId == null || memberRoleId.isEmpty()) {
            sb.append("No member role ID configured.\n");
        } else {
            Role memberRole = Utils.getRoleFromGuild(member.getGuild(), memberRoleId);
            if (memberRole != null) {
                boolean hasMemberRole = Utils.hasRole(member, memberRole.getId());
                if (isMember) {
                    // they are in your guild
                    if (!hasMemberRole) {
                        member.getGuild().addRoleToMember(member, memberRole).queue();
                        sb.append("Added the member role ").append(memberRole.getAsMention()).append('\n');
                    }
                    if (!isOwner)
                        member.modifyNickname(nickname).queue(); // make sure there nick doesnt have a guild tag after it
                } else {
                    // in a different guild
                    sb.append(removeRolesIfNotMember(member));
                    // make sure there nick has a guild tag after it
                    if (!isOwner) {
                        String guildPrefix;
                        if (playerData.guild == null){
                            guildPrefix = "NONE";
                        } else {
                            guildPrefix = playerData.guild.prefix;
                        }
                        member.getGuild().modifyNickname(member, nickname + " [" + guildPrefix + "]").queue();
                    }
                }
            } else {
                sb.append("Member role not found in guild for ID: ").append(memberRoleId).append('\n');
            }
        }


        // add their in-game guild rank
        if (isMember) {
            Utils.RankList rankOfMember = playerData.getRank();
            // Determine the correct rank role ID for the member
            String rankRoleId = null;
            switch (rankOfMember) {
                case Owner -> rankRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.OwnerRole);
                case Chief -> rankRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.ChiefRole);
                case Strategist ->
                        rankRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.StrategistRole);
                case Captain -> rankRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.CaptainRole);
                case Recruiter -> rankRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.RecruiterRole);
                case Recruit -> rankRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.RecruitRole);
                case null -> {
                }
            }
            if (rankRoleId != null && rankRoleId.isEmpty()) {
                sb.append("Failed to get role ID for rank ").append(rankOfMember).append("\n"); // unset in config
            }
            // if null there are not in your guild and it will remove all roles otherwise remove all and add the correct one
            sb.append(removeRankRolesExcept(member, rankRoleId));
        }

        // add their support rank
        String supportRoleId = null;
        if (playerData.supportRank != null) {
            // Determine the correct support role ID for the member
            supportRoleId = switch (playerData.supportRank.toLowerCase()) {
                case "vip" -> ConfigManager.getConfigInstance().roles.get(Config.Roles.VipRole);
                case "vipplus" -> ConfigManager.getConfigInstance().roles.get(Config.Roles.VipPlusRole);
                case "hero" -> ConfigManager.getConfigInstance().roles.get(Config.Roles.HeroRole);
                case "heroplus" -> ConfigManager.getConfigInstance().roles.get(Config.Roles.HeroPlusRole);
                case "champion" -> ConfigManager.getConfigInstance().roles.get(Config.Roles.ChampionRole);
                default -> null;
            };
        }
        if (supportRoleId != null && supportRoleId.isEmpty()) {
            sb.append("Failed to get role ID for support rank ").append(playerData.supportRank).append("\n"); // unset in config
        }
        sb.append(removeSupportRankRolesExcept(member, supportRoleId));


        // check for 100% content completion
        int MAX_CONTENT_COMPLETION = Integer.parseInt(ConfigManager.getConfigInstance().other.get(Config.Settings.MaxContentCompletion));
        String contentCompletionRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.OneHundredPercentContentCompletionRole);
        if (contentCompletionRoleId == null || contentCompletionRoleId.isEmpty()) {
            sb.append("No 100% completion role ID configured.\n");
        } else {
            Role OneHundredPercentContentCompletionRole = Utils.getRoleFromGuild(member.getGuild(), contentCompletionRoleId);
            if (OneHundredPercentContentCompletionRole != null) {
                if (playerData.getHighestContentCompletion() >= MAX_CONTENT_COMPLETION) {
                    if (!Utils.hasRole(member, OneHundredPercentContentCompletionRole.getId())) {
                        member.getGuild().addRoleToMember(member, OneHundredPercentContentCompletionRole).queue();
                        sb.append("Added the 100% completion role ").append(OneHundredPercentContentCompletionRole.getAsMention()).append('\n');
                    }
                }
            } else {
                sb.append("100% completion role not found in guild for ID: ").append(contentCompletionRoleId).append('\n');
            }
        }


        // add their highest lvl role
        int highestLvl = playerData.getHighestLevel();
        Config.LvlRoles matchedRole;
        if (highestLvl >= 106) {
            matchedRole = Config.LvlRoles.Lvl106Role;
        } else {
            int roundedLvl = (highestLvl <= 1) ? 1 : ((highestLvl + 4) / 5) * 5;
            String enumName = "Lvl" + roundedLvl + "Role";
            matchedRole = Config.LvlRoles.valueOf(enumName);
        }
        String lvlRoleId = ConfigManager.getConfigInstance().lvlRoles.get(matchedRole);
        if (lvlRoleId != null && lvlRoleId.isEmpty()) {
            sb.append("Failed to get role ID for lvl role ").append(lvlRoleId).append("\n"); // unset in config
        }
        sb.append(removeLvlRolesExcept(member, lvlRoleId));


        // their wynncraft server rank so admin/content team
        String wynnRankRoleId = switch (playerData.rank) {
            case "Administrator", "WebDev"-> ConfigManager.getConfigInstance().roles.get(Config.Roles.WynnAdminRole);
            case "Music", "Hybrid", "Game Master" -> ConfigManager.getConfigInstance().roles.get(Config.Roles.WynnContentTeamRole);
            case "Moderator" -> ConfigManager.getConfigInstance().roles.get(Config.Roles.WynnModeratorRole);
            default -> null;
        };
        sb.append(removeWynnRankRolesExcept(member, wynnRankRoleId));

        //veteran?
        if (playerData.veteran) {
            sb.append(Utils.addRole(member, Config.Roles.WynnVetRole));
        } else {
            sb.append(Utils.removeRole(member, Config.Roles.WynnVetRole));
        }

        // Send to mod channel
        TextChannel modChannel = member.getGuild().getTextChannelById(
                ConfigManager.getConfigInstance().channels.get(Config.Channels.ModerationChannel)
        );
        if (modChannel != null && !sb.toString().isEmpty()) {
            EmbedBuilder modEmbed = new EmbedBuilder()
                    .setColor(Color.ORANGE)
                    .setDescription(sb.toString());

            modChannel.sendMessageEmbeds(modEmbed.build()).queue();
        }
        return sb.toString();
    }

    public static void removeVerification(SlashCommandInteractionEvent event) {
        String userId = event.getOption("user_id").getAsString();
        ConfigManager.getDatabaseInstance().removeVerification(userId);
        event.reply("removed verification for " + userId).setEphemeral(true).queue();
    }

    public static void updateRoles(SlashCommandInteractionEvent event) {
        User user = event.getOption("user").getAsUser();
        event.getGuild().retrieveMemberById(user.getId()).queue(member -> {
            String changes = updatePlayer(member);
            event.reply("updated " + member.getAsMention() + "\n" + changes).setEphemeral(true).queue();
        }, failure -> {
            event.reply("User not found in this guild!").setEphemeral(true).queue();
        });
    }

    public static String removeRolesIfNotMember(Member member){

        return removeRankRolesExcept(member, null) +
                removeWarRolesExcept(member, null) +
                Utils.removeRole(member, Config.Roles.MemberRole) +
                Utils.removeRole(member, Config.Roles.GuildRaidsRole) +
                Utils.removeRole(member, Config.Roles.GiveawayRole);
    }

    public static String removeWarRolesExcept(Member member, String trialRoleId){
        Set<String> allTrialRoleIds = Stream.of(
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialEcoRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialTankRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialDpsRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialHealerRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialSoloRole)
                )
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());

        return removeRolesExcept(member, allTrialRoleIds, trialRoleId);
    }

    public static String removeRankRolesExcept(Member member, String rankRoleId) {
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

        return removeRolesExcept(member, allRankRoleIds, rankRoleId);
    }

    public static String removeSupportRankRolesExcept(Member member, String supportRankRoleId){
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

        return removeRolesExcept(member, allSupportRoleIds, supportRankRoleId);
    }

    public static String removeWynnRankRolesExcept(Member member, String wynnRankRoleId){
        // All support rank role IDs from config
        Set<String> allWynnRankRoleIds = Stream.of(
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.WynnModeratorRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.WynnAdminRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.WynnContentTeamRole)
                )
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());

        return removeRolesExcept(member, allWynnRankRoleIds, wynnRankRoleId);
    }

    public static String removeLvlRolesExcept(Member member, String lvlRoleId){
        // All support rank role IDs from config
        Set<String> allLvlRoleIds = Arrays.stream(Config.LvlRoles.values())
                .map(lvlRole -> ConfigManager.getConfigInstance().lvlRoles.get(lvlRole))
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());

        return removeRolesExcept(member, allLvlRoleIds, lvlRoleId);
    }

    public static String removeRolesExcept(Member member, Set<String> allIds, String idToKeep){
        StringBuilder sb = new StringBuilder();

        Guild guild = member.getGuild();
        // Remove all other roles from member
        for (String roleId : allIds) {
            if (!roleId.equals(idToKeep) && Utils.hasRole(member, roleId)) {
                Role roleToRemove = Utils.getRoleFromGuild(guild, roleId);
                if (roleToRemove != null && Utils.hasRole(member, roleId)) {
                    guild.removeRoleFromMember(member, roleToRemove).queue();
                    sb.append("Removed role ").append(roleToRemove.getAsMention()).append('\n');
                }
            }
        }
        // Add the correct role if not already present
        if (idToKeep != null) {
            if (!Utils.hasRole(member, idToKeep)) {
                Role rankRole = Utils.getRoleFromGuild(guild, idToKeep);
                if (rankRole != null && !Utils.hasRole(member, idToKeep)) {
                    guild.addRoleToMember(member, rankRole).queue();
                    sb.append("Added the role ").append(rankRole.getAsMention()).append('\n');
                } else {
                    sb.append("Role not found in guild for ID: ").append(idToKeep).append('\n');
                }
            }
        }

        return sb.toString();
    }

}
