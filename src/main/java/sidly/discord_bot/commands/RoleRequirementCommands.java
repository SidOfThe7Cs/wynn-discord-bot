package sidly.discord_bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;

public class RoleRequirementCommands {
    public static void setRoleRequirement(SlashCommandInteractionEvent event) {
        AllSlashCommands command = AllSlashCommands.valueOf(event.getOption("command").getAsString());
        Config.Settings role = Config.Settings.valueOf(event.getOption("required_role").getAsString());

        ConfigManager.getConfigInstance().roleRequirements.put(command, role);

        event.reply("success").setEphemeral(true).queue();
    }

    public static void removeRoleRequirement(SlashCommandInteractionEvent event) {
        AllSlashCommands command = AllSlashCommands.valueOf(event.getOption("command").getAsString());

        ConfigManager.getConfigInstance().roleRequirements.remove(command);

        event.reply("success").setEphemeral(true).queue();
    }
}
