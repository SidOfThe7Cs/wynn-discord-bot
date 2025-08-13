package sidly.discord_bot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public class Utils {

    public static String getDiscordTimestamp(long epoch, boolean relative){
        String type = "t";
        if (relative) type = "R";
        return "<t:" + epoch/1000 + ":" + type + ">";
    }

    public static boolean hasRole(Member user, String roleId){
        return user != null && user.getRoles().stream().anyMatch(role -> role.getId().equals(roleId));
    }

    public static boolean hasAtLeastRank(Member user, String roleId) {
        List<String> rankOrder = List.of(
                ConfigManager.getConfigInstance().roles.get(Config.Roles.RecruitRole),
                ConfigManager.getConfigInstance().roles.get(Config.Roles.RecruiterRole),
                ConfigManager.getConfigInstance().roles.get(Config.Roles.CaptainRole),
                ConfigManager.getConfigInstance().roles.get(Config.Roles.StrategistRole),
                ConfigManager.getConfigInstance().roles.get(Config.Roles.ChiefRole),
                ConfigManager.getConfigInstance().roles.get(Config.Roles.OwnerRole)
                );


        if (user == null || roleId == null || roleId.isEmpty()) return false;
        int targetIndex = rankOrder.indexOf(roleId);

        if (targetIndex == -1) return false; // roleId not found in rankOrder

        // Check if user has any role with rank >= targetIndex
        for (Role role : user.getRoles()) {
            int userRoleIndex = rankOrder.indexOf(role.getId());
            if (userRoleIndex >= targetIndex) {
                return true;
            }
        }
        return false;
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

    public static Config.Channels getChannelEnumFromId(String channelId) {
        return ConfigManager.getConfigInstance().channels.entrySet().stream()
                .filter(entry -> channelId.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null); // or throw exception if you want
    }

    public static String addRole(Member member, String roleId){
        StringBuilder sb = new StringBuilder();
        Guild guild = member.getGuild();
        Role roleToAdd = getRoleFromGuild(guild, roleId);
        if (roleToAdd != null) {
            if (!Utils.hasRole(member, roleId)) {
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
            if (Utils.hasRole(member, roleId)) {
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

    public enum RankList{
        Owner,
        Chief,
        Strategist,
        Captain,
        Recruiter,
        Recruit
    }

    public static long daysSinceIso(String isoTimestamp) {
        try {
            // Parse the ISO-8601 timestamp string into an Instant
            Instant past = Instant.parse(isoTimestamp);
            Instant now = Instant.ofEpochMilli(System.currentTimeMillis());

            // Calculate days between past and now
            return ChronoUnit.DAYS.between(past, now);
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // Return -1 if parsing failed
        }
    }

    public static Role getRoleFromGuild(Guild guild, String id){
        if (id == null || id.isEmpty()){
            return null;
        }else{
            return guild.getRoleById(id);
        }
    }

    public static GuildChannel getChannelFromGuild(Guild guild, String id){
        if (id == null || id.isEmpty()){
            return null;
        }else{
            return guild.getGuildChannelById(id);
        }
    }

    public static String escapeDiscordMarkdown(String input) {
        if (input == null) return null;
        // Characters to escape in Discord Markdown
        String specialChars = "[*_~`>|]";
        return input.replaceAll("([" + specialChars + "])", "\\\\$1");
    }

}
