package sidly.discord_bot.v2;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import sidly.discord_bot.v2.botapi.ButtonManager;
import sidly.discord_bot.v2.botapi.Command;
import sidly.discord_bot.v2.botapi.CommandManager;
import sidly.discord_bot.v2.botapi.ConfigManager;
import sidly.discord_bot.v2.config.GlobalConfig;
import sidly.discord_bot.v2.event.CommandRegistrationEvent;
import sidly.discord_bot.v2.event.Event;

import java.util.EnumSet;

public class Launch {
    public static JDA jda;

    private static boolean shuttingDown = false;
    public static void shutdown() {
        if (shuttingDown) return;
        shuttingDown = true;

        System.out.println("Shutting down");
        ConfigManager.save();
        System.exit(0);
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(Launch::shutdown));

        try {
            ConfigManager.load();
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        CommandManager.loadCommands();
        Event.register(CommandRegistrationEvent.class, CommandManager::onRegister);

        String token = ConfigManager.GLOBAL_INSTANCE.getAsString(GlobalConfig.StringOption.TOKEN);
        if (token == null || token.isEmpty()){
            System.err.println("please assign your bot token in the config file located at " + ConfigManager.GLOBAL_CONFIG_FILE.getAbsolutePath());
            shutdown();
        }

        JDA jda = JDABuilder.createLight(token, EnumSet.noneOf(GatewayIntent.class))
                .addEventListeners(
                        new ButtonManager(),
                        new CommandManager()
                ) // TODO add here as needed (must extend listener adapter)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();

        CommandListUpdateAction commands = jda.updateCommands();
        new CommandRegistrationEvent(commands).post();
        commands.queue();
    }
}
