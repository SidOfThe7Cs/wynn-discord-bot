package sidly.discord_bot;

import net.dv8tion.jda.api.entities.Member;

public class Utils {
    public static String getDiscordTimestamp(long epoch, boolean relative){
        String type = "t";
        if (relative) type = "R";
        return "<t: " + epoch + ":" + type + ">";
    }

    public static boolean hasRole(Member user, String userId){
        return user != null && user.getRoles().stream().anyMatch(role -> role.getId().equals(userId));
    }

    public static void checkForUpdates(){

    }
}
