package sidly.discord_bot.v2.event;

import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import sidly.discord_bot.v2.botapi.Command;

public class CommandRegistrationEvent extends Event {
    private CommandListUpdateAction commands;

    public CommandRegistrationEvent(CommandListUpdateAction commandListUpdateAction) {
        this.commands = commandListUpdateAction;
    }

    public void addCommand(Command cmd) {
        System.out.println("registering command: " + cmd.getName());
        commands = commands.addCommands(cmd.getAsData());
    }
}
