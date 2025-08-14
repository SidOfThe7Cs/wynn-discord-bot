package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;

import java.awt.*;

public class RateLimitCommands {
    public static void getRateLimitInfo(SlashCommandInteractionEvent event) {

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.CYAN);
        embed.setTitle("Rate Limit Info");
        StringBuilder sb = new StringBuilder();

        for (ApiUtils.RateLimitTypes type : ApiUtils.RateLimitTypes.values()) {
            ApiUtils.RateLimitInfo rateLimitInfo = ApiUtils.rateLimitInfoMap.get(type);
            if (rateLimitInfo == null){
                continue;
            }
            sb.append("**").append(type.name()).append("**\n");
            long resetTime = ApiUtils.getLastRateLimitUpdate() + (rateLimitInfo.rateLimitSecondsTillReset() * 1000L);
            // if reset time is in past set remaining to max
            String remaining = String.valueOf((resetTime <= System.currentTimeMillis()) ? rateLimitInfo.rateLimitMax() : rateLimitInfo.rateLimitRemaining());
            sb.append(remaining).append(" / ").append(rateLimitInfo.rateLimitMax()).append(" remaining\n").append("resets in ").append(Utils.getDiscordTimestamp(resetTime, true));
            sb.append("\n");
        }

        embed.setDescription(sb.toString());
        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}
