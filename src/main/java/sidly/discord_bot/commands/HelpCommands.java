package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.page.PageBuilder;
import sidly.discord_bot.page.PaginationIds;

import java.awt.*;
import java.io.File;
import java.util.List;

public class HelpCommands {
    public static void listCommands(SlashCommandInteractionEvent event) {

        event.deferReply(false).addComponents(PageBuilder.getPaginationActionRow(PaginationIds.COMMAND_LIST)).queue(hook -> {
            PageBuilder.PaginationState pageState = PageBuilder.PaginationManager.get(PaginationIds.COMMAND_LIST.name());
            pageState.reset(List.of(AllSlashCommands.values()));

            EmbedBuilder embed = pageState.buildEmbedPage();

            if (embed == null) {
                event.reply("no guilds").setEphemeral(true).queue();
                return;
            }

            hook.editOriginalEmbeds(embed.build()).queue();
        });
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

    public static String commandListConverter(AllSlashCommands command) {
        StringBuilder sb = new StringBuilder();
        sb.append("**").append(command.name()).append("**").append(" ");
        if (command.getRequiredRole() != null) {
            sb.append("requires <@&").append(ConfigManager.getConfigInstance().roles.get(command.getRequiredRole())).append(">\n");
        } else sb.append("\n");
        sb.append(command.getDescription()).append("\n");

        if (command.getAction() == null) sb.append("this command currently does nothing\n");
        return sb.toString();
    }

    public static void sendMessage(SlashCommandInteractionEvent event) {
        GuildChannelUnion channel = event.getOption("channel").getAsChannel();
        String message = event.getOption("message").getAsString();

        channel.asTextChannel().sendMessage(message);
        event.reply("done").setEphemeral(true).queue();
    }
}
