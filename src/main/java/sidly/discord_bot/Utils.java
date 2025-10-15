package sidly.discord_bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    public static String getDiscordTimestamp(long epoch, boolean relative){
        String type = "t";
        if (relative) type = "R";
        return "<t:" + epoch/1000 + ":" + type + ">";
    }

    public static boolean hasAtLeastRank(Member user, String roleId) {
        List<String> rankOrder = List.of(
                ConfigManager.getConfigInstance().roles.get(Config.Roles.RecruitRole),
                ConfigManager.getConfigInstance().roles.get(Config.Roles.RecruiterRole),
                ConfigManager.getConfigInstance().roles.get(Config.Roles.CaptainRole),
                ConfigManager.getConfigInstance().roles.get(Config.Roles.StrategistRole),
                ConfigManager.getConfigInstance().roles.get(Config.Roles.ChiefRole),
                ConfigManager.getConfigInstance().roles.get(Config.Roles.OwnerRole)
                );


        if (user == null || roleId == null || roleId.isEmpty()) return false;
        int targetIndex = rankOrder.indexOf(roleId);

        if (targetIndex == -1) return false; // roleId not found in rankOrder

        // Check if user has any role with rank >= targetIndex
        for (Role role : user.getRoles()) {
            int userRoleIndex = rankOrder.indexOf(role.getId());
            if (userRoleIndex >= targetIndex) {
                return true;
            }
        }
        return false;
    }

    public static Config.Channels getChannelEnumFromId(String channelId) {
        return ConfigManager.getConfigInstance().channels.entrySet().stream()
                .filter(entry -> channelId.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null); // or throw exception if you want
    }

    public enum RankList{
        Owner,
        Chief,
        Strategist,
        Captain,
        Recruiter,
        Recruit
    }

    public static long timeSinceIso(String isoTimestamp, ChronoUnit timeunit) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) return Long.MAX_VALUE;
        try {
            // Parse the ISO-8601 timestamp string into an Instant
            Instant past = Instant.parse(isoTimestamp);
            Instant now = Instant.ofEpochMilli(System.currentTimeMillis());

            if (timeunit == ChronoUnit.WEEKS) {
                return ChronoUnit.DAYS.between(past, now) / 7;
            }
            // Calculate days between past and now
            return timeunit.between(past, now);
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // Return -1 if parsing failed
        }
    }

    public static long getEpochTimeFromIso(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) return -1;
        try {
            // Parse the ISO-8601 timestamp string into an Instant
            Instant instant = Instant.parse(isoTimestamp);
            return instant.toEpochMilli();
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // Return -1 if parsing failed
        }
    }


    public static GuildChannel getChannelFromGuild(Guild guild, String id){
        if (id == null || id.isEmpty()){
            return null;
        }else{
            return guild.getGuildChannelById(id);
        }
    }

    public static String escapeDiscordMarkdown(String input) {
        if (input == null) return null;
        // Characters to escape in Discord Markdown
        String specialChars = "[*_~`>|]";
        return input.replaceAll("([" + specialChars + "])", "\\\\$1");
    }

    public static int getHoursSinceDayStarted(long millis){
        long millisInDay = millis % (24 * 60 * 60 * 1000);

        long hours = millisInDay / (60 * 60 * 1000);

        return (int)((hours));
    }

    public static String getTimestampFromInt(int hour){
        long epochSeconds = LocalDate.now().atTime(hour, 0).toEpochSecond(ZoneOffset.UTC);
        return getDiscordTimestamp(epochSeconds * 1000, false);
    }

    public static List<String> padToLongest(List<String> input) {
        if (input == null || input.isEmpty()) return new ArrayList<>();

        // Find the maximum length
        int maxLength = input.stream().mapToInt(String::length).max().orElse(0);

        List<String> paddedList = new ArrayList<>();
        for (String str : input) {
            if (str == null) str = "";
            int padding = maxLength - str.length();
            paddedList.add(str + "\u2005".repeat(Math.max(0, padding))
            );
        }

        return paddedList;
    }

    public static MessageEmbed getEmbed(String title, String description){
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.CYAN);
        if (!title.isEmpty()) embed.setTitle(title);

        if (description.length() > 4096) {
            description = description.substring(0, 4095);
            embed.setFooter("Character limit hit");
        }
        embed.setDescription(description);

        return embed.build();
    }

    public static void sendToModChannel(String title, String text, boolean fakeTitle) {
        Guild guild = MainEntrypoint.jda.getGuildById(ConfigManager.getConfigInstance().other.get(Config.Settings.YourDiscordServerId));
        TextChannel modChannel = guild.getTextChannelById(
                ConfigManager.getConfigInstance().channels.get(Config.Channels.ModerationChannel)
        );

        if (text == null || text.isEmpty()) return;

        if (fakeTitle) text = title + "\n" + text;

        if (modChannel != null) {
            EmbedBuilder modEmbed = new EmbedBuilder()
                    .setColor(Color.ORANGE)
                    .setDescription(text);
            if (!fakeTitle) modEmbed.setTitle(title);

            modChannel.sendMessageEmbeds(modEmbed.build()).queue();
        }
    }

    public static String abbreviate(String input) {
        if (input == null || input.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        String[] parts = input.split("\\s+"); // split by any whitespace

        for (String part : parts) {
            if (!part.isEmpty()) {
                sb.append(part.charAt(0)); // take the first character
            }
        }

        return sb.toString();
    }

    public static String formatNumbersInString(String input) {
        if (input == null || input.isEmpty()) return "";

        Pattern pattern = Pattern.compile("\\d+(?:,?\\d+)*(?:\\.\\d+)?"); // matches integers and decimals
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String numStr = matcher.group().replaceAll(",", ""); // remove commas
            double number;
            try {
                number = Double.parseDouble(numStr);
            } catch (NumberFormatException e) {
                matcher.appendReplacement(sb, matcher.group()); // keep original if parse fails
                continue;
            }

            // Format the number with suffixes
            String formatted = formatNumber(number);
            matcher.appendReplacement(sb, formatted);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public static String formatTime(long time, ChronoUnit unit) {
        boolean negative = time < 0;

        double seconds = unit.getDuration().toMillis() / 1000.0 * Math.abs(time);
        String base;

        if (seconds < 60) {
            base = String.format("%.1fs", seconds);
        } else if ((seconds /= 60.0) < 60) {
            base = String.format("%.1fm", seconds);
        } else if ((seconds /= 60.0) < 24) {
            base = String.format("%.1fh", seconds);
        } else if ((seconds /= 24.0) < 7) {
            base = String.format("%.1fd", seconds);
        } else if ((seconds /= 7.0) < 4.345) {
            base = String.format("%.1fw", seconds);
        } else if ((seconds *= 7.0 / 30.4375) < 12) {
            base = String.format("%.1fmo", seconds);
        } else {
            base = String.format("%.1fy", seconds / 12.0);
        }

        return negative ? "in " + base : base + " ago";
    }


    // Reuse the formatNumber(double) function from before
    public static String formatNumber(double number) {
        String[] suffixes = {"", "K", "M", "B", "T"};
        int suffixIndex = 0;

        while (Math.abs(number) >= 1000 && suffixIndex < suffixes.length - 1) {
            number /= 1000.0;
            suffixIndex++;
        }

        if (number == Math.floor(number)) {
            return String.format("%,d%s", (long) number, suffixes[suffixIndex]);
        } else {
            return String.format("%,.2f%s", number, suffixes[suffixIndex]);
        }
    }

    public static double getTotalGuildExperience(int level, int percentToNextLevel) {
        if (level < 1) return 0;

        int base = 20000;
        double totalExp = 0;

        // Sum scaled XP up to level 130
        int cappedLevel = Math.min(level, 130);
        for (int n = 1; n <= cappedLevel; n++) {
            totalExp += base * Math.pow(1.15, n - 1);
        }

        // If level is beyond 130, add flat XP for the extra levels
        if (level > 130) {
            double cappedXp = base * Math.pow(1.15, 129); // XP at level 130
            totalExp += (level - 130) * cappedXp;
        }

        // Add fractional progress to next level
        double nextXp;
        if (level < 130) {
            nextXp = base * Math.pow(1.15, level);
        } else {
            nextXp = base * Math.pow(1.15, 129); // flat XP after 130
        }

        totalExp += nextXp * (percentToNextLevel / 100.0);

        return totalExp;
    }

    // Generic method to clean a collection based on a reference set
    public static <T> List<T> removeIfNotIn(Collection<T> collection, Set<T> allowed) {
        List<T> removed = new ArrayList<>();
        collection.removeIf(item -> {
            if (!allowed.contains(item)) {
                removed.add(item);
                return true;
            }
            return false;
        });
        return removed;
    }


}
