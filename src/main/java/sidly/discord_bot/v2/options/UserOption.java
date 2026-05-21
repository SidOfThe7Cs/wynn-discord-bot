package sidly.discord_bot.v2.options;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Optional;

public class UserOption implements Option {
    private final static String NAME = "user";

    public static User from(SlashCommandInteractionEvent event) {
        return Optional.ofNullable(event.getOption(NAME))
                .map(OptionMapping::getAsUser)
                .orElse(event.getUser());
    }

    private final boolean required;

    public UserOption(boolean required) {
        this.required = required;
    }

    @Override
    public OptionData getData() {
        return new OptionData(OptionType.USER, NAME, "A user in the server.").setRequired(required);
    }
}
