package sidly.discord_bot.v2.botapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.MainEntrypoint;
import sidly.discord_bot.v2.config.Config;
import sidly.discord_bot.v2.config.ConfigUtil;
import sidly.discord_bot.v2.config.GlobalConfig;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final File JAR_DIR;

    static {
        try {
            JAR_DIR = new File(MainEntrypoint.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static final File GLOBAL_CONFIG_FILE = new File(JAR_DIR, "config.json");
    public static final File SERVERS_CONFIG_FILE = new File(JAR_DIR, "server_configs.json");

    public static GlobalConfig GLOBAL_INSTANCE = new GlobalConfig();
    public static Server_Configs SERVERS_INSTANCE = new Server_Configs();

    public static void save() {
        ConfigUtil.saveConfig(SERVERS_INSTANCE, SERVERS_CONFIG_FILE);
        ConfigUtil.saveConfig(GLOBAL_INSTANCE, GLOBAL_CONFIG_FILE);
    }

    public static void load() {
        if (!Files.exists(GLOBAL_CONFIG_FILE.toPath())) {
            System.err.println("no config file found");
            GLOBAL_CONFIG_FILE.getParentFile().mkdirs();
            ConfigUtil.saveConfig(GLOBAL_INSTANCE, GLOBAL_CONFIG_FILE);
            ConfigUtil.saveConfig(SERVERS_INSTANCE, SERVERS_CONFIG_FILE);
            throw new RuntimeException("Global config has been created at " + GLOBAL_CONFIG_FILE.getAbsolutePath() + " please edit it with necessary info");
        }
        SERVERS_INSTANCE = ConfigUtil.loadConfig(Server_Configs.class, SERVERS_CONFIG_FILE);
        GLOBAL_INSTANCE = ConfigUtil.loadConfig(GlobalConfig.class, GLOBAL_CONFIG_FILE);
    }

    public static class Server_Configs {
        private final Map<Long, Config> configs = new HashMap<>();

        public Config getConfig(SlashCommandInteractionEvent event) {
            Guild guild = event.getGuild();
            if (guild == null) return null;
            return configs.get(guild.getIdLong());
        }
    }
}
