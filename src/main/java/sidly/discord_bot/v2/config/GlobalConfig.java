package sidly.discord_bot.v2.config;

import net.dv8tion.jda.api.entities.channel.Channel;
import sidly.discord_bot.v2.Launch;

import java.util.EnumMap;
import java.util.Map;

public class GlobalConfig {
    private final Map<StringOption, String> strings = new EnumMap<>(StringOption.class);
    private final Map<ChannelOption, Long> channels = new EnumMap<>(ChannelOption.class);

    public enum StringOption {
        TOKEN
    }

    public enum ChannelOption {
        CONSOLE
    }

    public String getAsString(StringOption s) {
        return this.strings.get(s);
    }

    public Channel getAsChannel(ChannelOption id) {
        return Launch.jda.getChannelById(Channel.class, this.channels.get(id));
    }

    public GlobalConfig() {
        for (StringOption option : StringOption.values()) {
            if (strings.containsKey(option)) continue;
            strings.put(option, "");
        }

        for (ChannelOption option : ChannelOption.values()) {
            if (channels.containsKey(option)) continue;
            channels.put(option, -1L);
        }
    }
}
