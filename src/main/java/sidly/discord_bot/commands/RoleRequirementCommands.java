package sidly.discord_bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;

import java.util.Map;

public class RoleRequirementCommands {
    public static void setRoleRequirement(SlashCommandInteractionEvent event) {
        AllSlashCommands command = AllSlashCommands.valueOf(event.getOption("command").getAsString());
        String mention  = event.getOption("role").getAsString();
        String id = mention.replaceAll("\\D+", "");

        ConfigManager.getConfigInstance().roleRequirements.put(command, getRoleEnumFromId(id));

        event.reply("success").setEphemeral(true).queue();
    }

    public static void removeRoleRequirement(SlashCommandInteractionEvent event) {
        AllSlashCommands command = AllSlashCommands.valueOf(event.getOption("command").getAsString());

        ConfigManager.getConfigInstance().roleRequirements.remove(command);

        event.reply("success").setEphemeral(true).queue();
    }

    public static Config.Roles getRoleEnumFromId(String roleId) {
        return ConfigManager.getConfigInstance().roles.entrySet().stream()
                .filter(entry -> roleId.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null); // or throw exception if you want
    }

}
