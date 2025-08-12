package sidly.discord_bot.commands;

import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.ConfigManager;

public class ChannelRestrinctionCommands {
    public static void addRestriction(SlashCommandInteractionEvent event) {
        Channel channel = event.getOption("channel").getAsChannel();
        String allowed = event.getOption("allowed").getAsString();
        Boolean ehh = null;

        switch (allowed.toLowerCase()){
            case "true":
                ehh = true;
                event.reply("added " + channel.getAsMention() + " to whitelist").setEphemeral(true).queue();
                break;
            case "false":
                ehh = false;
                event.reply("added " + channel.getAsMention() + " to blacklist").setEphemeral(true).queue();
                break;
            case "null":
                event.reply("set " + channel.getAsMention() + " to default").setEphemeral(true).queue();
                break;
        }

        ConfigManager.getConfigInstance().allowedChannels.put(channel.getId(), ehh);
        ConfigManager.save();
    }
}
