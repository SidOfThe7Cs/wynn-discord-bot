package sidly.discord_bot.commands.demotion_promotion;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.database.PlaytimeHistoryList;

import java.awt.*;
import java.util.Map;

public class PlaytimeCommands {
    public static void getAllPlayersPlaytime(SlashCommandInteractionEvent event) {
        int weeks = event.getOption("weeks").getAsInt();

        GuildInfo guild = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
        Map<String, PlaytimeHistoryList> playtimeHistory = ConfigManager.getDatabaseInstance().playtimeHistory;
        EmbedBuilder embed = new EmbedBuilder();
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, GuildInfo.MemberInfo> entry : guild.members.getAllMembers().entrySet()){
            PlaytimeHistoryList playtimeHistoryList = playtimeHistory.get(entry.getValue().username);
            if (playtimeHistoryList != null) {
                sb.append(entry.getValue().username).append(" ");
                sb.append(playtimeHistoryList.getPlaytimeHistory());
            }
        }

        embed.setTitle("Playtimes");
        embed.setColor(Color.CYAN);
        embed.setDescription(sb.toString());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }
}
