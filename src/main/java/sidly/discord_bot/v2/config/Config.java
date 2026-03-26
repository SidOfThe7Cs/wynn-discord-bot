package sidly.discord_bot.v2.config;

import java.util.EnumMap;
import java.util.Map;

public class Config {

    private final Map<StringOption, String> strings = new EnumMap<>(StringOption.class);

    public enum StringOption {
        GUILD_PREFIX
    }

    public String getAsString(StringOption stringOption) {
        return this.strings.get(stringOption);
    }

    public Config() {
        for (StringOption option : StringOption.values()) {
            if (strings.containsKey(option)) continue;
            strings.put(option, "");
        }
    }
}
