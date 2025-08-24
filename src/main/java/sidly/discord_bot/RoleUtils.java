package sidly.discord_bot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoleUtils {
    public static String removeRolesIfNotMember(Member member){

        return removeRankRolesExcept(member, null) +
                removeTrialWarRoles(member) +
                removeRole(member, Config.Roles.MemberRole) +
                removeRole(member, Config.Roles.GuildRaidsRole) +
                removeRole(member, Config.Roles.WarrerRole) +
                removeRole(member, Config.Roles.WarPingRole) +
                removeRole(member, Config.Roles.GiveawayRole);
    }

    public static String removeRolesUnverify(Member member){
        return removeRolesIfNotMember(member) +
                removeRole(member, Config.Roles.WynnVetRole) +
                removeRole(member, Config.Roles.OneHundredPercentContentCompletionRole) +
                removeRole(member, Config.Roles.VerifiedRole) +
                addRole(member, Config.Roles.UnVerifiedRole) +
                removeSupportRankRolesExcept(member, null) +
                removeLvlRolesExcept(member, null) +
                removeRole(member, Config.Roles.WynnAdminRole) +
                removeRole(member, Config.Roles.WynnModeratorRole) +
                removeRole(member, Config.Roles.WynnContentTeamRole);
    }

    public static String removeTrialWarRoles(Member member){
        Set<String> allTrialRoleIds = Stream.of(
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialEcoRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialTankRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialDpsRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialHealerRole),
                        ConfigManager.getConfigInstance().roles.get(Config.Roles.TrialSoloRole)
                )
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());

        return removeRolesExcept(member, allTrialRoleIds, null);
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

    public static String removeClassRolesExcept(Member member, Config.ClassRoles classRole){
        String classRoleId = ConfigManager.getConfigInstance().classRoles.get(classRole);
        // All support rank role IDs from config
        Set<String> allClassRoleIds = Stream.of(
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Warrior),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Archer),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Mage),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Shaman),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Assassin)
                )
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());

        return removeRolesExcept(member, allClassRoleIds, classRoleId);
    }

    public static String removeArchetypeRolesExcept(Member member, Config.ClassRoles ArchetypeRole){
        String ArchetypeRoleId = ConfigManager.getConfigInstance().classRoles.get(ArchetypeRole);
        // All support rank role IDs from config
        Set<String> allArchetypeRoleIds = Stream.of(
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Boltslinger),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Trapper),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Sharpshooter),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Fallen),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.BattleMonk),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Paladin),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.RiftWalker),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.LightBender),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Arcanist),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Trickster),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Shadestepper),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Acrobat),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Summoner),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Ritualist),
                        ConfigManager.getConfigInstance().classRoles.get(Config.ClassRoles.Acolyte)
                )
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toSet());

        return removeRolesExcept(member, allArchetypeRoleIds, ArchetypeRoleId);
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
            if (!roleId.equals(idToKeep) && hasRole(member, roleId)) {
                Role roleToRemove = getRoleFromGuild(guild, roleId);
                if (roleToRemove != null) {
                    if (hasRole(member, roleId)) {
                        guild.removeRoleFromMember(member, roleToRemove).queue();
                        sb.append("Removed role ").append(roleToRemove.getAsMention()).append('\n');
                    }
                } else {
                    sb.append("Role not found in guild for ID: ").append(idToKeep).append('\n');
                }
            }
        }
        // Add the correct role if not already present
        if (idToKeep != null) {
            if (!hasRole(member, idToKeep)) {
                Role rankRole = getRoleFromGuild(guild, idToKeep);
                if (rankRole != null) {
                    guild.addRoleToMember(member, rankRole).queue();
                    sb.append("Added the role ").append(rankRole.getAsMention()).append('\n');
                } else {
                    sb.append("Role not found in guild for ID: ").append(idToKeep).append('\n');
                }
            }
        }

        return sb.toString();
    }

    public static boolean hasRole(Member user, String roleId){
        return user != null && user.getRoles().stream().anyMatch(role -> role.getId().equals(roleId));
    }

    public static boolean hasRole(Member member, Config.Roles role) {
        String s = ConfigManager.getConfigInstance().roles.get(role);
        return hasRole(member, s);
    }

    public static boolean hasRole(Member member, Config.ClassRoles role) {
        String s = ConfigManager.getConfigInstance().classRoles.get(role);
        return hasRole(member, s);
    }

    public static Config.Roles getRoleEnumFromId(String roleId) {
        return ConfigManager.getConfigInstance().roles.entrySet().stream()
                .filter(entry -> roleId.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null); // or throw exception if you want
    }

    public static Config.LvlRoles getLvlRoleEnumFromId(String roleId) {
        return ConfigManager.getConfigInstance().lvlRoles.entrySet().stream()
                .filter(entry -> roleId.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null); // or throw exception if you want
    }

    public static String addRole(Member member, String roleId){
        StringBuilder sb = new StringBuilder();
        Guild guild = member.getGuild();
        Role roleToAdd = getRoleFromGuild(guild, roleId);
        if (roleToAdd != null) {
            if (!hasRole(member, roleId)) {
                guild.addRoleToMember(member, roleToAdd).queue();
                sb.append("Added role ").append(roleToAdd.getAsMention()).append('\n');
            }
        } else sb.append("failed to get role for ").append(roleId).append('\n');
        return sb.toString();
    }

    public static String addRole(Member member, Config.Roles role){
        String roleId = ConfigManager.getConfigInstance().roles.get(role);
        return addRole(member, roleId);
    }

    public static String removeRole(Member member, String roleId){
        StringBuilder sb = new StringBuilder();
        Guild guild = member.getGuild();
        Role roleToRemove = getRoleFromGuild(guild, roleId);
        if (roleToRemove != null) {
            if (hasRole(member, roleId)) {
                guild.removeRoleFromMember(member, roleToRemove).queue();
                sb.append("Removed role ").append(roleToRemove.getAsMention()).append('\n');
            }
        } else sb.append("failed to get role for ").append(roleId).append('\n');
        return sb.toString();
    }

    public static String removeRole(Member member, Config.Roles role){
        String roleId = ConfigManager.getConfigInstance().roles.get(role);
        return removeRole(member, roleId);
    }

    public static Role getRoleFromGuild(Guild guild, String id){
        if (id == null || id.isEmpty()){
            return null;
        }else{
            return guild.getRoleById(id);
        }
    }
}
