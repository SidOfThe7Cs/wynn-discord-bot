package sidly.discord_bot.commands.inactivity_promotion;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import sidly.discord_bot.Utils;
import sidly.discord_bot.database.PlaytimeHistoryList;
import sidly.discord_bot.database.tables.PlaytimeHistory;
import sidly.discord_bot.database.tables.UuidMap;

import java.util.Optional;

public class PlaytimeCommands {
    public static void getWarReport(SlashCommandInteractionEvent event) {
        event.deferReply(false).queue(hook -> {
            User user = Optional.ofNullable(event.getOption("user"))
                    .map(OptionMapping::getAsUser)
                    .orElse(null);
            String username;
            if (user != null) username = event.getGuild().getMember(user).getEffectiveName();
            else {
                username = Optional.ofNullable(event.getOption("username"))
                        .map(OptionMapping::getAsString)
                        .orElse(null);
            }
            if (username == null || username.isEmpty()) {
                username = event.getMember().getEffectiveName();
            }

            String minecraftIdByUsername = UuidMap.getMinecraftIdByUsername(username);
            if (minecraftIdByUsername == null) {
                hook.editOriginal("failed to get uuid for " + username).queue();
                return;
            }
            PlaytimeHistoryList playtimeHistory = PlaytimeHistory.getPlaytimeHistory(minecraftIdByUsername);
            String playtimeReport = playtimeHistory.getPlaytimeReport();

            hook.editOriginalEmbeds(Utils.getEmbed("Playtime Entries for " + username, playtimeReport)).queue();
        });
    }
}
