package sidly.discord_bot.v2.options;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Optional;

public class UsernameOption implements Option {
    private final static String NAME = "username";

    public static String from(SlashCommandInteractionEvent event) {
        return Optional.ofNullable(event.getOption(NAME))
                .map(OptionMapping::getAsString)
                .orElse(event.getUser().getEffectiveName());
    }

    public static String fromUserOrName(SlashCommandInteractionEvent event) {
        User user = UserOption.from(event);
        String username = Optional.ofNullable(event.getOption(NAME))
                .map(OptionMapping::getAsString)
                .orElse(null);
        return user == event.getUser() ? username : user.getEffectiveName(); // TODO dont use the users username store verification separately and or make work with [tag]
    }

    private final boolean required;

    public UsernameOption(boolean required) {
        this.required = required;
    }

    @Override
    public OptionData getData() {
        return new OptionData(OptionType.STRING, NAME, "A username of a player.").setRequired(required);
    }
}
