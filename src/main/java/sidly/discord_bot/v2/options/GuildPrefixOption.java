package sidly.discord_bot.v2.options;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import sidly.discord_bot.v2.botapi.ConfigManager;
import sidly.discord_bot.v2.config.Config;

import java.util.Optional;

import static sidly.discord_bot.v2.config.Config.StringOption.GUILD_PREFIX;

public class GuildPrefixOption implements Option {
    private final static String NAME = "guild_prefix";

    public static String from(SlashCommandInteractionEvent event) {
        Config config = ConfigManager.SERVERS_INSTANCE.getConfig(event);
        return Optional.ofNullable(event.getOption(NAME))
                .map(OptionMapping::getAsString)
                .orElse(config == null ? null : config.getAsString(GUILD_PREFIX));
    }

    private final boolean required;

    public GuildPrefixOption(boolean required) {
        this.required = required;
    }

    @Override
    public OptionData getData() {
        return new OptionData(OptionType.STRING, NAME, "The prefix of a guild.").setRequired(required);
    }
}
