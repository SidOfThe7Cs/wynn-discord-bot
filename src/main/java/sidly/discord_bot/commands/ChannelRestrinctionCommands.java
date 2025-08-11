package sidly.discord_bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.ConfigManager;

public class ChannelRestrinctionCommands {
    public static void addRestriction(SlashCommandInteractionEvent event) {
        String channelId = event.getOption("channel_id").getAsString();
        String allowed = event.getOption("allowed").getAsString();
        String channelMention = event.getGuild().getGuildChannelById(channelId).getAsMention();
        Boolean ehh = null;

        switch (allowed.toLowerCase()){
            case "true":
                ehh = true;
                event.reply("added " + channelMention + " to whitelist").setEphemeral(true).queue();
                break;
            case "false":
                ehh = false;
                event.reply("added " + channelMention + " to blacklist").setEphemeral(true).queue();
                break;
            case "null":
                event.reply("set " + channelMention + " to default").setEphemeral(true).queue();
                break;
        }

        ConfigManager.getConfigInstance().allowedChannels.put(channelId, ehh);
        ConfigManager.save();
    }
}
