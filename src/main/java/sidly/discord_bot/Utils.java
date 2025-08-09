package sidly.discord_bot;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.List;

public class Utils {

    public static String getDiscordTimestamp(long epoch, boolean relative){
        String type = "t";
        if (relative) type = "R";
        return "<t:" + epoch + ":" + type + ">";
    }

    public static boolean hasRole(Member user, String userId){
        return user != null && user.getRoles().stream().anyMatch(role -> role.getId().equals(userId));
    }

    public static boolean hasAtLeastRank(Member user, String roleId) {
        List<String> rankOrder = List.of(
                ConfigManager.getSetting(Config.Settings.RecruitRole),     // lowest
                ConfigManager.getSetting(Config.Settings.RecruiterRole),
                ConfigManager.getSetting(Config.Settings.CaptainRole),
                ConfigManager.getSetting(Config.Settings.StrategistRole),
                ConfigManager.getSetting(Config.Settings.ChiefRole),
                ConfigManager.getSetting(Config.Settings.OwnerRole)        // highest
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

    public enum RankList{
        Owner,
        Chief,
        Strategist,
        Captain,
        Recruiter,
        Recruit
    }
}
