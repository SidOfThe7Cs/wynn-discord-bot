package sidly.discord_bot.commands;

import kotlin.Pair;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.MainEntrypoint;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Graids {
    private static final Map<String, Tracker> trackers = new HashMap<>();
    private static final Map<String, GuildInfo.GuildRaids> totalCounts = new HashMap<>();
    private static Timer timer;

    public static class Tracker {
        private final ConcurrentHashMap<String, Integer> increaseCounts = new ConcurrentHashMap<>();
        private Long messageId;
        private final Long channelId;
        private final Raid raid;
        private final Long startTime;
        private final String trackerName;

        private final boolean stickied;
        private final boolean aspects;
        private final boolean positionNumbers;

        public Tracker(String name, Raid raid, Long channelId, boolean stickied, boolean aspects, boolean positionNumbers) {
            this.trackerName = name;
            this.raid = raid;
            this.channelId = channelId;
            this.stickied = stickied;
            this.aspects = aspects;
            this.positionNumbers = positionNumbers;
            this.startTime = System.currentTimeMillis();
            trackers.put(name, this);
        }

        public void updateMessageId(Long messageId) {
            this.messageId = messageId;
        }

        private Integer getCount(GuildInfo.GuildRaids counts) {
            return raid == Raid.ALL ? counts.total : counts.list.get(raid.apiName());
        }

        private void updateCount(String username, GuildInfo.GuildRaids oldCounts) {
            GuildInfo.GuildRaids newCounts = totalCounts.get(username);

            Integer oldTotal = getCount(oldCounts);
            Integer newTotal = getCount(newCounts);

            if (oldTotal != null) {
                increaseCounts.merge(username, newTotal - oldTotal, Integer::sum);
            }
        }

        public void aspectsGiven(boolean roundUp) {
            int threshold = roundUp ? 1 : 2;
            increaseCounts.replaceAll((k, v) -> v >= threshold ? 0 : v);
        }

        public void stop() {
            if (channelId != null && messageId != null) {
                TextChannel channel = MainEntrypoint.jda.getTextChannelById(channelId);
                if (channel != null) {
                    channel.retrieveMessageById(messageId).queue(message -> message.editMessageComponents().queue());
                }
            }
            trackers.remove(this.trackerName);
        }

        private MessageEmbed getEmbed() {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.CYAN);
            embed.setFooter("id: " + trackerName);
            String currentTime = Utils.getDiscordTimestamp(System.currentTimeMillis(), true);
            embed.setTitle("**Graid Tracker** started " + Utils.getDiscordTimestamp(startTime, true) + "\nlast updated " + currentTime);

            List<Map.Entry<String, Integer>> sortedEntries = increaseCounts.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .sorted(Map.Entry.comparingByValue())
                    .toList()
                    .reversed();

            StringBuilder playersBuilder = new StringBuilder();
            StringBuilder countsBuilder = new StringBuilder();
            StringBuilder aspectsBuilder = new StringBuilder();

            Pair<Integer, Integer> lastEntry = new Pair<>(0, 0);
            for (int i = 0; i < sortedEntries.size(); i++) {
                Map.Entry<String, Integer> entry = sortedEntries.get(i);
                String playerLine = "";
                if (this.positionNumbers) {
                    int completed = entry.getValue();
                    int rank = completed < lastEntry.component1() ? i : lastEntry.component2();
                    lastEntry = new Pair<>(completed, i);
                    playerLine += rank + ". ";
                }
                playerLine += Utils.escapeDiscordMarkdown(entry.getKey()) + "\n";
                String countLine = entry.getValue() + "\n";
                String aspectsLine = "";
                if (aspects) aspectsLine += entry.getValue() / 2f + "\n";

                // Check if adding this line would exceed the limit
                if (playersBuilder.length() + playerLine.length() <= 1021 &&
                        countsBuilder.length() + countLine.length() <= 1021 &&
                        aspectsBuilder.length() + aspectsLine.length() <= 1021
                ) {
                    playersBuilder.append(playerLine);
                    countsBuilder.append(countLine);
                    aspectsBuilder.append(aspectsLine);
                } else {
                    // Add a truncation message
                    playersBuilder.append("...");
                    countsBuilder.append("...");
                    aspectsBuilder.append("...");
                    break;
                }
            }

            embed.addField("Players", playersBuilder.toString(), true);
            embed.addField(raid.name() + " Comps", countsBuilder.toString(), true);
            if (aspects) embed.addField( "Aspects", aspectsBuilder.toString(), true);


            return embed.build();
        }

        public ActionRow getButtons() {
            if (aspects) {
                Button down = Button.primary("aspects:" + trackerName + ":down", "i gave out aspects (round down)");
                Button up = Button.primary("aspects:" + trackerName + ":up", "i gave out aspects (round up)");
                return ActionRow.of(down, up);
            }
            return null;
        }

        public void updateDisplay() {
            if (channelId != null && messageId != null) {
                TextChannel channel = MainEntrypoint.jda.getTextChannelById(channelId);
                if (channel != null) {
                    channel.retrieveMessageById(messageId).queue(message -> {
                        MessageEmbed newMessage = getEmbed();

                        channel.getHistory().retrievePast(1).queue(history -> {
                            Message latest = history.getFirst();

                            if (stickied && message.getIdLong() != latest.getIdLong()) {
                                message.delete().queue();

                                MessageCreateAction messageCreateAction = channel.sendMessageEmbeds(newMessage);
                                ActionRow buttons = getButtons();
                                if (buttons != null) {
                                    messageCreateAction.setComponents(buttons);
                                }
                                messageCreateAction.queue(
                                        editedTo -> this.messageId = editedTo.getIdLong()
                                );

                            } else {
                                MessageEditAction messageAction = message.editMessageEmbeds(newMessage);
                                ActionRow buttons = getButtons();
                                if (buttons != null) {
                                    messageAction.setComponents(buttons);
                                }
                                messageAction.queue();
                            }
                        });
                    });
                }
            }
        }
    }

    public static void startTimer() {
        timer = new Timer();
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
        GuildInfo guildInfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
        if (guildInfo == null || guildInfo.members == null) {
            System.err.println("null guildInfo");
            return;
        }

        Map<String, GuildInfo.GuildRaids> oldCounts = new HashMap<>(totalCounts);
        Map<String, GuildInfo.MemberInfo> allMembersByUsername = guildInfo.members.getAllMembersByUsername();

        for (Map.Entry<String, GuildInfo.MemberInfo> entry : allMembersByUsername.entrySet()) {
            String username = entry.getValue().username;
            GuildInfo.GuildRaids guildRaids = entry.getValue().guildRaids;
            totalCounts.put(username, guildRaids);

            for (Tracker tracker : trackers.values()) {
                tracker.updateCount(username, oldCounts.get(username));
            }
        }

        for (Tracker tracker : trackers.values()) {
            tracker.updateDisplay();
        }
    }

    public static void startGraidTracker(SlashCommandInteractionEvent event) {
        TextChannel channel = (TextChannel) event.getOption("channel").getAsChannel();
        Raid raid = Raid.from(event.getOption("raid").getAsString());
        String name = event.getOption("name").getAsString();

        if (name.equals("get")) {
            Set<String> strings = trackers.keySet();
            StringBuilder sb = new StringBuilder();
            for (String string : strings) {
                sb.append(string).append("\n");
            }
            event.reply("Current Trackers:\n" + sb).setEphemeral(true).queue();
            return;
        }

        if (trackers.containsKey(name)) {
            if (raid == Raid.NONE) {
                event.reply("stopped tracker " + name).setEphemeral(true).queue();
                trackers.get(name).stop();
                return;
            }
            event.reply("cannot create tracker " + name + " as it already exists").setEphemeral(true).queue();
            return;
        }

        boolean stickied = Optional.ofNullable(event.getOption("stickied"))
                .map(OptionMapping::getAsBoolean)
                .orElse(false);
        boolean aspects = Optional.ofNullable(event.getOption("aspects"))
                .map(OptionMapping::getAsBoolean)
                .orElse(false);
        boolean positionNumbers = Optional.ofNullable(event.getOption("ranks"))
                .map(OptionMapping::getAsBoolean)
                .orElse(false);


        Tracker tracker = new Tracker(name, raid, channel.getIdLong(), stickied, aspects, positionNumbers);
        MessageCreateAction messageAction = channel.sendMessageEmbeds(tracker.getEmbed());
        ActionRow buttons = tracker.getButtons();
        if (buttons != null) {
            messageAction.setComponents(buttons);
        }
        messageAction.queue(
                message -> tracker.updateMessageId(message.getIdLong())
        );
        event.reply("tracker " + name + " started for " + raid + " in " + channel.getAsMention()).setEphemeral(true).queue();
    }

    public static void buttonClicked(ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        if (parts.length == 3) {
            String name = parts[1];
            String bool = parts[2];
            trackers.get(name).aspectsGiven(bool.equals("up"));
            event.reply("thanks!").setEphemeral(true).queue();
            return;
        }
        event.reply("failed :(").setEphemeral(true).queue();
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
