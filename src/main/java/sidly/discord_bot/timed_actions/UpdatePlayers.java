package sidly.discord_bot.timed_actions;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.MainEntrypoint;
import sidly.discord_bot.commands.VerificationCommands;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UpdatePlayers {
    private static final Queue<Member> updateQueue = new ConcurrentLinkedQueue<>();
    private static int serverMemberCount = 0;
    private static Timer timer;
    private static volatile boolean loadingMembers = false;

    public static boolean isRunning() {
        return isRunning;
    }

    private static boolean isRunning = false;

    public static void updateNext(){
        if (updateQueue.size() < serverMemberCount / 4 || updateQueue.isEmpty()){
            if (!loadingMembers) {
                loadingMembers = true;
                getAllMembers();
            }
        }
        Member next = updateQueue.poll();
        if (next != null) {
            // Refresh member from API to ensure all roles are loaded
            Guild guild = next.getGuild();
            guild.retrieveMemberById(next.getId()).queue(freshMember -> {
                VerificationCommands.updatePlayer(freshMember);
            }, failure -> {
                System.err.println("Failed to retrieve member " + next.getId());
            });
        }
    }

    public static void getAllMembers(){
        String serverId = ConfigManager.getConfigInstance().other.get(Config.Settings.YourDiscordServerId);
        if (serverId == null || serverId.isEmpty()) {
            System.err.println("please assign server id with /editconfigother");
            return;
        }

        Guild guild = MainEntrypoint.jda.getGuildById(serverId);
        if (guild != null) {
            guild.loadMembers().onSuccess(members -> {
                serverMemberCount = members.size();
                updateQueue.addAll(members);
                loadingMembers = false;
            }).onError(e -> {
                e.printStackTrace();
                loadingMembers = false;
            });
        }else {
            System.out.println("failed to get guild UpdatePlayers.GetAllMembers()");
            loadingMembers = false;
        }
    }

    public static void init(){
        if (isRunning) {
            return;
        }
        isRunning = true;
        timer = new Timer();
        getAllMembers();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    updateNext();
                } catch (Exception e) {
                    e.printStackTrace(); // Log and keep going
                }
            }
        },  4 * 1000, 2000); // 2 seconds
    }

    public static void shutdown() {
        isRunning = false;
        timer.cancel();
    }
}
