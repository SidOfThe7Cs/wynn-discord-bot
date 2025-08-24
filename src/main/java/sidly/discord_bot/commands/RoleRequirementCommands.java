package sidly.discord_bot.commands;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.RoleUtils;

public class RoleRequirementCommands {
    public static void setRoleRequirement(SlashCommandInteractionEvent event) {
        AllSlashCommands command = AllSlashCommands.valueOf(event.getOption("command").getAsString());
        Role mention  = event.getOption("role").getAsRole();
        String id = mention.getId();

        ConfigManager.getConfigInstance().roleRequirements.put(command, RoleUtils.getRoleEnumFromId(id));

        event.reply("success").setEphemeral(true).queue();
    }

    public static void removeRoleRequirement(SlashCommandInteractionEvent event) {
        AllSlashCommands command = AllSlashCommands.valueOf(event.getOption("command").getAsString());

        ConfigManager.getConfigInstance().roleRequirements.remove(command);

        event.reply("success").setEphemeral(true).queue();
    }

}
