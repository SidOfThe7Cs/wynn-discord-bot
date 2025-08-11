package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;

import java.awt.*;

public class RateLimitCommands {
    public static void getRateLimitInfo(SlashCommandInteractionEvent event) {
        long resetTime = ApiUtils.getLastRateLimitUpdate() + (ApiUtils.getRateLimitSecondsTillReset() * 1000L);
        // if reset time is in past set remaining to max
        String remaining = String.valueOf((resetTime <= System.currentTimeMillis()) ? ApiUtils.getRateLimitMax() : ApiUtils.getRateLimitRemaining());
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.CYAN);
        embed.setTitle("Rate Limit");
        embed.setDescription(remaining + " / " + ApiUtils.getRateLimitMax() + " remaining\n" + "resets in " + Utils.getDiscordTimestamp(resetTime, true));

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}
