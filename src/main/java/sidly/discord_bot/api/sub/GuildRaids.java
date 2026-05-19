package sidly.discord_bot.api.sub;

import java.util.Map;

public class GuildRaids {
    public int total;
    public Map<String, Integer> list;

    @Override
    public String toString() {
        return list.toString();
    }
}
