package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;

import java.awt.*;
import java.util.stream.Collectors;

public class ConfigCommands {
    public static void editConfigOption(SlashCommandInteractionEvent event){
        String setting = event.getOption("setting").getAsString();
        String newValue = event.getOption("new_value").getAsString();
        Config.Settings option = Config.Settings.valueOf(setting);

        if (option == Config.Settings.Token){
            event.reply("if this is working the token is already correct and you dont want to change it").setEphemeral(true).queue();
            return;
        }

        ConfigManager.getConfigInstance().settings.put(option, newValue);
        ConfigManager.save();

        event.reply(event.getUser().getName() + " has changed the " + setting + " to " + newValue).setEphemeral(false).queue();
    }


    public static void showConfigOptions(SlashCommandInteractionEvent event){
        String result = ConfigManager.getConfigInstance().settings.entrySet().stream()
                .filter(entry -> entry.getKey() != Config.Settings.Token)
                .map(entry -> entry.getKey() + " : " + entry.getValue())
                .collect(Collectors.joining(System.lineSeparator()));


        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("current config:")
                .setDescription(result)
                .setColor(Color.CYAN)
                .setFooter("Requested by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl())
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}
