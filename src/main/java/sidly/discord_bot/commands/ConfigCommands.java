package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;

import java.awt.*;
import java.util.stream.Collectors;

public class ConfigCommands {
    public static void editConfigOption(SlashCommandInteractionEvent event) {
        String setting = event.getOption("setting").getAsString();
        String newValue = event.getOption("new_value").getAsString();

        Config.Settings option;
        try {
            option = Config.Settings.valueOf(setting);
        } catch (IllegalArgumentException e) {
            event.reply("Invalid setting: " + setting).setEphemeral(true).queue();
            return;
        }

        if (option == Config.Settings.Token) {
            event.reply("If this is working the token is already correct and you don't want to change it").setEphemeral(true).queue();
            return;
        } else {
            System.out.println(option);
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

        result += "\n";
        result += ConfigManager.getConfigInstance().lvlRoles.entrySet().stream()
                .map(entry -> entry.getKey() + " : " + entry.getValue())
                .collect(Collectors.joining(System.lineSeparator()));

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Current Config:")
                .setDescription(result)
                .setColor(Color.CYAN)
                .setFooter("Requested by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl())
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    public static void editConfigLvlRoleOption(SlashCommandInteractionEvent event) {
        String setting = event.getOption("role").getAsString();
        String newValue = event.getOption("role_id").getAsString();
        Config.LvlRoles option = Config.LvlRoles.valueOf(setting);

        ConfigManager.getConfigInstance().lvlRoles.put(option, newValue);
        ConfigManager.save();

        event.reply(event.getUser().getName() + " has changed the " + setting + " to " + newValue).setEphemeral(false).queue();
    }
}
