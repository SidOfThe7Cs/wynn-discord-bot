package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.ConfigManager;

import java.awt.*;

public class HelpCommands {
    public static void listCommands(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("List of All Bot Commands");
        embed.setColor(Color.CYAN);

        StringBuilder sb = new StringBuilder();

        for (AllSlashCommands command : AllSlashCommands.values()){
            sb.append("**").append(command.name()).append("**").append("\n");
            sb.append(command.getDescription()).append("\n");
            if (command.getRequiredRole() != null) {
                sb.append("requires <@&").append(ConfigManager.getConfigInstance().roles.get(command.getRequiredRole())).append(">\n");
            }
            if (command.getAction() == null) sb.append("this command currently does nothing\n");
        }
        embed.setDescription(sb.toString());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}
