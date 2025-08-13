package sidly.discord_bot.timed_actions;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.commands.VerificationCommands;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class UpdatePlayers {
    private static final Queue<Member> updateQueue = new ConcurrentLinkedQueue<>();
    private static JDA jda;
    private static int serverMemberCount = 0;
    private static Timer timer = new Timer();
    private static volatile boolean loadingMembers = false;

    public static void updateNext(){
        if (updateQueue.size() < serverMemberCount / 4 || updateQueue.isEmpty()){
            if (!loadingMembers) {
                loadingMembers = true;
                getAllMembers();
            }
        }
        Member next = updateQueue.poll();
        if (next != null) {
            VerificationCommands.updatePlayer(next);
        }
    }

    public static void getAllMembers(){
        String serverId = ConfigManager.getConfigInstance().other.get(Config.Settings.YourDiscordServerId);
        if (serverId == null || serverId.isEmpty()) {
            System.err.println("please assign server id with /editconfigother");
            return;
        }

        Guild guild = jda.getGuildById(serverId);
        if (guild != null) {
            guild.loadMembers().onSuccess(members -> {
                serverMemberCount = members.size();
                updateQueue.addAll(members);
                loadingMembers = false;
            }).onError(e -> {
                e.printStackTrace();
                loadingMembers = false;
            });;
        }else {
            System.out.println("failed to get guild UpdatePlayers.GetAllMembers()");
            loadingMembers = false;
        }
    }

    public static void init(JDA jda){
        UpdatePlayers.jda = jda;
        getAllMembers();

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateNext();
            }
        },  10 * 1000, 90 * 1000); // 90 seconds
    }

    public static void shutdown() {
        timer.cancel();
    }
}
