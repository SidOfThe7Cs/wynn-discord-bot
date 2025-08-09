package sidly.discord_bot;

import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.api.PlayerProfile;

import java.util.HashMap;
import java.util.Map;

public class Database {
    public static Map<String, PlayerProfile> allPlayers = new HashMap<>();
    public static Map<String, GuildInfo> allGuilds = new HashMap<>();
}
