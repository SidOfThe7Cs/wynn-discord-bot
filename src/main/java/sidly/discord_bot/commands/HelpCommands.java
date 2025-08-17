package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;
import sidly.discord_bot.page.PageBuilder;
import sidly.discord_bot.page.PaginationIds;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HelpCommands {
    public static void listCommands(SlashCommandInteractionEvent event) {
        EmbedBuilder embed = buildCommandListPage();

        if (embed == null) {
            event.reply("no guilds").setEphemeral(true).queue();
            return;
        }

        event.replyEmbeds(embed.build())
                .addComponents(Utils.getPaginationActionRow(PaginationIds.COMMAND_LIST))
                .queue();
    }

    public static void getSystemInfo(SlashCommandInteractionEvent event){
        File root = new File("/");
        String sb =
                "**OS:** " + System.getProperty("os.name") + "\n" +
                "**OS Version:** " + System.getProperty("os.version") + "\n" +
                "**Architecture:** " + System.getProperty("os.arch") + "\n" +
                "**Java Version:** " + System.getProperty("java.version") + "\n" +
                "**Available Processors:** " + Runtime.getRuntime().availableProcessors() + "\n" +
                "**Total Memory On JVM:** " + Runtime.getRuntime().totalMemory() / 1024 / 1024 + " MB\n" +
                "**Free Memory On JVM:** " + Runtime.getRuntime().freeMemory() / 1024 / 1024 + " MB\n" +
                "**Total Storage:** " + root.getTotalSpace() / 1024 / 1024 / 1024 + " GB\n" +
                "**Free Storage:** " + root.getFreeSpace() / 1024 / 1024 / 1024 + " GB\n";


        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("Host Machine Info");
        embed.setColor(Color.CYAN);
        embed.setDescription(sb);

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();

    }

    public static EmbedBuilder buildCommandListPage() {
        List<String> entries = new ArrayList<>();
        PageBuilder.PaginationState paginationState = PageBuilder.PaginationManager.get(PaginationIds.COMMAND_LIST.name());

        for (AllSlashCommands command : AllSlashCommands.values()){
            StringBuilder sb = new StringBuilder();
            sb.append("**").append(command.name()).append("**").append("\n");
            sb.append(command.getDescription()).append("\n");
            if (command.getRequiredRole() != null) {
                sb.append("requires <@&").append(ConfigManager.getConfigInstance().roles.get(command.getRequiredRole())).append(">\n");
            }
            if (command.getAction() == null) sb.append("this command currently does nothing\n");
            entries.add(sb.toString());
        }
        return PageBuilder.buildEmbedPage(entries, paginationState.currentPage, 20, "List of All Bot Commands");
    }
}
