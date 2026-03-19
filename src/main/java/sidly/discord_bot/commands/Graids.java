package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.MainEntrypoint;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class Graids {
    private static final Map<String, Integer> totalCounts = new HashMap<>();
    private static final Map<String, Integer> increaseCounts = new HashMap<>();
    private static Long currentMessageId;
    private static Long currentChannelId;
    private static Raid trackedRaid;
    private static Long lastStartTime;

    private static Timer timer;

    public static void stopTracker() {
        increaseCounts.clear();
        currentMessageId = null;
        currentChannelId = null;
        lastStartTime = null;
        timer = new Timer();
    }

    public static void start() {
        stopTracker();
        lastStartTime = System.currentTimeMillis();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    updateAllCounts();
                } catch (Exception e) {
                    System.err.println("Error in GraidTrackerTimer" + e.getMessage());
                }
            }
        },  9000, TimeUnit.MINUTES.toMillis(3));
    }

    private static void updateAllCounts() {
        Raid raid = trackedRaid;
        if (raid == Raid.NONE) return;
        GuildInfo guildInfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
        if (guildInfo == null || guildInfo.members == null) {
            System.err.println("null guildInfo");
            return;
        }

        Map<String, GuildInfo.MemberInfo> allMembersByUsername = guildInfo.members.getAllMembersByUsername();
        for (Map.Entry<String, GuildInfo.MemberInfo> entry : allMembersByUsername.entrySet()) {
            GuildInfo.GuildRaids guildRaids = entry.getValue().guildRaids;
            Integer count = raid == Raid.ALL ? guildRaids.total : guildRaids.list.get(raid.apiName());
            updateCount(entry.getValue().username, count);
        }

        updateGraidTracker();
    }

    private static void updateCount(String player, Integer newTotal) {
        Integer oldTotal = totalCounts.get(player);

        if (oldTotal != null) {
            increaseCounts.merge(player, newTotal - oldTotal, Integer::sum);
        }

        totalCounts.put(player, newTotal);
    }

    private static void updateGraidTracker() {

        if (currentChannelId != null && currentMessageId != null) {
            TextChannel channel = MainEntrypoint.jda.getTextChannelById(currentChannelId);
            if (channel != null) {
                channel.retrieveMessageById(currentMessageId).queue(message ->
                        message.editMessageEmbeds(getMessage().build()).queue());
            }
        }
    }

    public static void startGraidTracker(SlashCommandInteractionEvent event) {
        TextChannel channel = (TextChannel) event.getOption("channel").getAsChannel();
        String raid = event.getOption("raid").getAsString();
        trackedRaid = Raid.from(raid);
        if (trackedRaid == Raid.NONE) {
            stopTracker();
            event.reply("stopping trackers").setEphemeral(true).queue();
            return;
        }

        start();

        channel.sendMessageEmbeds(getMessage().build()).queue(message -> {
            currentMessageId = message.getIdLong();
            currentChannelId = channel.getIdLong();
        });

        event.reply("tracker started for " + raid + " in " + channel.getAsMention()).setEphemeral(true).queue();
    }

    private static EmbedBuilder getMessage() {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.CYAN);
        String currentTime = Utils.getDiscordTimestamp(System.currentTimeMillis(), true);
        embed.setTitle("**Graid Tracker** started " + Utils.getDiscordTimestamp(lastStartTime, true) + "\nlast updated " + currentTime);

        List<Map.Entry<String, Integer>> sortedEntries = increaseCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.comparingByValue())
                .toList();

        StringBuilder playersBuilder = new StringBuilder();
        StringBuilder countsBuilder = new StringBuilder();

        for (Map.Entry<String, Integer> entry : sortedEntries) {
            String playerLine = Utils.escapeDiscordMarkdown(entry.getKey()) + "\n";
            String countLine = entry.getValue() + "\n";

            // Check if adding this line would exceed the limit
            if (playersBuilder.length() + playerLine.length() <= 1024 &&
                    countsBuilder.length() + countLine.length() <= 1024) {
                playersBuilder.append(playerLine);
                countsBuilder.append(countLine);
            } else {
                // Add a truncation message
                playersBuilder.append("...");
                countsBuilder.append("...");
                break;
            }
        }

        String players = !playersBuilder.isEmpty() ? playersBuilder.toString() : "None";
        String graidComps = !countsBuilder.isEmpty() ? countsBuilder.toString() : "None";

        embed.addField("Players", players, true);
        embed.addField(trackedRaid.name() + " Comps", graidComps, true);

        return embed;
    }

    public enum Raid {
        NOTG("Nest of the Grootslangs"),
        NOL("Orphion's Nexus of Light"),
        TCC("The Canyon Colossus"),
        TNA("The Nameless Anomaly"),
        ALL("All"),
        NONE("None");

        private final String apiName;

        public String apiName() {
            return apiName;
        }

        Raid(String apiName) {
            this.apiName = apiName;
        }

        public static Raid from(String name) {
            try {
                return Raid.valueOf(name);
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid Raid name: " + name);
                return NONE;
            }
        }
    }

}
