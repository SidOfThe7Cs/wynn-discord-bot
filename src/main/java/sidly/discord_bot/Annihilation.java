package sidly.discord_bot;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Annihilation {
    public static void sendAnniPing(SlashCommandInteractionEvent event) {
        String time = event.getOption("time_till_start").getAsString();
        String discordTimestamp = Utils.getDiscordTimestamp(parseTimeToMilliseconds(time), true);


    }

    public static void createAnniParties(SlashCommandInteractionEvent event) {
    }

    /**
     * Parses a time string in the format "xh ym" and returns milliseconds into the future.
     * @param timeString String in format like "10h 14m", "5h 0m", "0h 30m"
     * @return milliseconds representing the time duration
     * @throws IllegalArgumentException if the format is invalid
     */
    public static long parseTimeToMilliseconds(String timeString) {
        // Regex pattern to match "xh ym" format
        Pattern pattern = Pattern.compile("(\\d+)h\\s+(\\d+)m");
        Matcher matcher = pattern.matcher(timeString);

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time format. Expected format: 'xh ym' (e.g., '10h 14m')");
        }

        // Extract hours and minutes
        int hours = Integer.parseInt(matcher.group(1));
        int minutes = Integer.parseInt(matcher.group(2));

        // Convert to milliseconds
        return (hours * 60L * 60L * 1000L) + (minutes * 60L * 1000L);
    }

    private record AnniPlayerData(ServerOption serverOption, ClassOption classOption, boolean canLead, boolean hasScrolls) {}

    private enum ServerOption {
        NA,
        EU,
        AS,
        NA_EU,
        NA_AS,
        EU_AS,
        ANY;
    }

    private enum ClassOption {
        Tank,
        Healer,
        Dps,

    }
}
