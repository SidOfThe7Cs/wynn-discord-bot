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
        for (AllSlashCommands command : AllSlashCommands.values()){
            String info = command.getDescription() + '\n';
            if (command.getRequiredRole() != null) {
                info += "requires " + "<@&" + ConfigManager.getSetting(command.getRequiredRole()) + ">\n";
            }
            if (command.getAction() == null) info += "this command currently does nothing";

            embed.addField(command.name(), info, false);
        }

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}
