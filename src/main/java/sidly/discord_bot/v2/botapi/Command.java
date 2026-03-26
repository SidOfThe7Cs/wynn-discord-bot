package sidly.discord_bot.v2.botapi;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.LayoutComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import sidly.discord_bot.v2.ErrorManager;
import sidly.discord_bot.v2.options.Option;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Command {
    /* TODO support command requirments for discord rolls or and permissions
            support commands that are able to be used not in a guild
            every command should pull the relivant config if it is guild
     */

    private final String name;
    private final String description;
    private final Function<SlashCommandInteractionEvent, CommandResult>  action;
    private final boolean paginated;
    private final Duration cooldown;
    private final Duration deactivateAfter;
    private final List<OptionData> options;
    private final List<LayoutComponent> components;
    private final Integer id;

    private long lastRan;

    private Command(Builder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.action = builder.action;
        this.paginated = builder.paginated;
        this.cooldown = builder.cooldown;
        this.deactivateAfter = builder.deactivateAfter;
        this.options = builder.options;
        this.components = builder.components;
        this.id = builder.id;

        CommandManager.register(this);
    }

    public String getName() {
        return name;
    }

    public static class Builder {
        public Builder(String name) {
            this.name = name.toLowerCase();
        }

        private final String name;
        private String description = "default description";
        private Function<SlashCommandInteractionEvent, CommandResult> action = Command::voidRun;
        private boolean paginated = false;
        private Duration cooldown = Duration.ZERO;
        private Duration deactivateAfter = Duration.ofMinutes(1);
        private final List<OptionData> options = new ArrayList<>();
        private final List<LayoutComponent> components = new ArrayList<>();
        private Integer id = null;

        public Builder description(String description) { this.description = description; return this; }
        public Builder action(Function<SlashCommandInteractionEvent, CommandResult> action) { this.action = action; return this; }
        public Builder paginated(boolean paginated) { this.paginated = paginated; return this; }
        public Builder cooldown(Duration cooldown) { this.cooldown = cooldown; return this; }
        public Builder deactivateAfter(Duration deactivateAfter) { this.deactivateAfter = deactivateAfter; return this; }
        public Builder option(OptionData option) { this.options.add(option); return this; }
        public Builder option(Option option) { this.options.add(option.getData()); return this; }
        public Builder buttonRow(Button... buttons) { this.components.add(ActionRow.of(buttons)); return this; }
        public Builder id(int id) { this.id = id; return this; }

        public Command build() { return new Command(this); }
    }

    public SlashCommandData getAsData() {
        SlashCommandData data = Commands.slash(this.name, this.description);
        data.addOptions(this.options);

        return data;
    }

    public void run(SlashCommandInteractionEvent event) {
        // TODO custom reqs but also look ay data.setDefaultPermissions

        // TODO maybe just like chache api calls and make the cd there
        if (lastRan + cooldown.toMillis() > System.currentTimeMillis()) {
            event.reply("this command is on global cooldown").setEphemeral(true).queue();
            return;
        }
        lastRan = System.currentTimeMillis();
        try {
            // TODO action returns a string which is automatically checked for length and added buttons
            CommandResult result = action.apply(event);
            ReplyCallbackAction reply = event.replyEmbeds(result.toEmbed());
            if (result.isEthereal()) reply = reply.setEphemeral(true);
            for (LayoutComponent row : components) {
                reply = reply.addComponents(row);
            }

            reply.queue();

            if (!components.isEmpty()) {
                // TODO store somehow with ids so you can deactivate after an amount of time
            }

        } catch (Exception e) {
            ErrorManager.errorInCommand(e, this);
        }
    }

    public static CommandResult voidRun(SlashCommandInteractionEvent event) {
        return new CommandResult.Builder("This command has not been implemented yet").ethereal(true).build();
    }

}
