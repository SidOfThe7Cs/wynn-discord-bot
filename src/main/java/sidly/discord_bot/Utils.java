package sidly.discord_bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import sidly.discord_bot.page.PaginationIds;

import java.awt.*;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Utils {

    public static String getDiscordTimestamp(long epoch, boolean relative){
        String type = "t";
        if (relative) type = "R";
        return "<t:" + epoch/1000 + ":" + type + ">";
    }

    public static boolean hasRole(Member user, String roleId){
        return user != null && user.getRoles().stream().anyMatch(role -> role.getId().equals(roleId));
    }

    public static boolean hasRole(Member member, Config.Roles role) {
        String s = ConfigManager.getConfigInstance().roles.get(role);
        return hasRole(member, s);
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

    public static Config.Roles getRoleEnumFromId(String roleId) {
        return ConfigManager.getConfigInstance().roles.entrySet().stream()
                .filter(entry -> roleId.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null); // or throw exception if you want
    }

    public static Config.LvlRoles getLvlRoleEnumFromId(String roleId) {
        return ConfigManager.getConfigInstance().lvlRoles.entrySet().stream()
                .filter(entry -> roleId.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null); // or throw exception if you want
    }

    public static Config.Channels getChannelEnumFromId(String channelId) {
        return ConfigManager.getConfigInstance().channels.entrySet().stream()
                .filter(entry -> channelId.equals(entry.getValue()))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null); // or throw exception if you want
    }

    public static String addRole(Member member, String roleId){
        StringBuilder sb = new StringBuilder();
        Guild guild = member.getGuild();
        Role roleToAdd = getRoleFromGuild(guild, roleId);
        if (roleToAdd != null) {
            if (!Utils.hasRole(member, roleId)) {
                guild.addRoleToMember(member, roleToAdd).queue();
                sb.append("Added role ").append(roleToAdd.getAsMention()).append('\n');
            }
        } else sb.append("failed to get role for ").append(roleId).append('\n');
        return sb.toString();
    }

    public static String addRole(Member member, Config.Roles role){
        String roleId = ConfigManager.getConfigInstance().roles.get(role);
        return addRole(member, roleId);
    }

    public static String removeRole(Member member, String roleId){
        StringBuilder sb = new StringBuilder();
        Guild guild = member.getGuild();
        Role roleToRemove = getRoleFromGuild(guild, roleId);
        if (roleToRemove != null) {
            if (Utils.hasRole(member, roleId)) {
                guild.removeRoleFromMember(member, roleToRemove).queue();
                sb.append("Removed role ").append(roleToRemove.getAsMention()).append('\n');
            }
        } else sb.append("failed to get role for ").append(roleId).append('\n');
        return sb.toString();
    }

    public static String removeRole(Member member, Config.Roles role){
        String roleId = ConfigManager.getConfigInstance().roles.get(role);
        return removeRole(member, roleId);
    }

    public enum RankList{
        Owner,
        Chief,
        Strategist,
        Captain,
        Recruiter,
        Recruit
    }

    public static long daysSinceIso(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.isEmpty()) return -1;
        try {
            // Parse the ISO-8601 timestamp string into an Instant
            Instant past = Instant.parse(isoTimestamp);
            Instant now = Instant.ofEpochMilli(System.currentTimeMillis());

            // Calculate days between past and now
            return ChronoUnit.DAYS.between(past, now);
        } catch (Exception e) {
            e.printStackTrace();
            return -1; // Return -1 if parsing failed
        }
    }

    public static Role getRoleFromGuild(Guild guild, String id){
        if (id == null || id.isEmpty()){
            return null;
        }else{
            return guild.getRoleById(id);
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
        title = title.isEmpty() ? "no title specified" : title;
        embed.setTitle(title);

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

    public static ActionRow getPaginationActionRow(PaginationIds id){
        Button leftButton = Button.primary("pagination:" + id.name() + ":left", "◀️");
        Button rightButton = Button.primary("pagination:" + id.name() + ":right", "▶️");
        return ActionRow.of(leftButton, rightButton);
    }
}
