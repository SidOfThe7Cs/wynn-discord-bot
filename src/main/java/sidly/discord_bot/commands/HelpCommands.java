package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.ConfigManager;

import java.awt.*;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;

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

    public static void getSystemInfo(SlashCommandInteractionEvent event){
        try {
            InetAddress localHost = InetAddress.getLocalHost();

            File root = new File("/");
            String sb = "**Hostname:** " + localHost.getHostName() + "\n" +
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

        } catch (UnknownHostException e) {
            event.reply("Failed to retrieve server info: " + e.getMessage()).setEphemeral(true).queue();
        }
    }
}
