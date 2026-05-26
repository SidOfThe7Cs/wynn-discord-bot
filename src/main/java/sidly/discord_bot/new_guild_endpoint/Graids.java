package sidly.discord_bot.new_guild_endpoint;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.api.sub.GuildRaids;
import sidly.discord_bot.api.sub.MemberInfo;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Graids {
    private static final Map<String, GraidTracker> trackers = new HashMap<>();
    private static Timer timer;
    private static final Map<String, GuildRaids> totalCounts = new HashMap<>();
    private static final Set<String> broken = new HashSet<>();

    public static class GraidTracker extends Tracker {
        private final Raid raid;

        private final boolean aspects;
        private final boolean positionNumbers;
        private final boolean total;

        public GraidTracker(String name, Raid raid, Long channelId, boolean stickied, boolean aspects, boolean positionNumbers, boolean total) {
            super(channelId, name, stickied);
            this.raid = raid;
            this.aspects = aspects;
            this.positionNumbers = positionNumbers;
            this.total = total;
            trackers.put(trackerName, this);
        }

        private Integer getCount(GuildRaids counts) {
            if (counts == null || counts.list == null) return null;
            return raid == Raid.ALL ? counts.total : counts.list.get(raid.apiName());
        }

        private void updateCount(String username, GuildRaids oldCounts) {
            GuildRaids newCounts = totalCounts.get(username);

            Integer oldTotal = getCount(oldCounts);
            Integer newTotal = getCount(newCounts);

            if (oldTotal != null && newTotal != null) {
                if (oldCounts.total == 0 && newCounts.total > 2) {
                    return; // we started the tracker when api showed 0 and it shouldn't have
                }
                increaseCounts.merge(username, newTotal - oldTotal, Integer::sum);
            }
        }

        public void aspectsGiven(boolean roundUp) {
            if (roundUp) {
                increaseCounts.clear();
            } else {
                increaseCounts.replaceAll((username, oldCount) -> oldCount % 2);
            }
            updateDisplay();
        }

        @Override
        protected MessageEmbed getEmbed() {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.CYAN);

            StringBuilder footer = new StringBuilder("id: " + trackerName + "\n");
            if (!broken.isEmpty()) footer.append("broken in api:\n");
            broken.forEach(name -> footer.append("   ").append(name).append("\n"));
            embed.setFooter(footer.toString());

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

            int lastRank = -1;
            int lastScore = 0;
            for (int i = 0; i < sortedEntries.size(); i++) {
                Map.Entry<String, Integer> entry = sortedEntries.get(i);
                String playerLine = "";
                if (this.positionNumbers) {
                    int score = entry.getValue();
                    int rank = (score == lastScore) ? lastRank : i + 1;

                    lastScore = score;
                    lastRank = rank;
                    playerLine += rank + "\\. ";
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

            if (this.total) {
                int total = increaseCounts.values().stream().reduce(Integer::sum).orElse(0);
                embed.setDescription("Total Completed: " + total / 4);
            }
            if (this.aspects) {
                int aspectsDown = increaseCounts.values().stream().mapToInt(v -> (int) Math.floor(v / 2.f)).sum();
                int aspectsUp = increaseCounts.values().stream().mapToInt(v -> (int) Math.ceil(v / 2.f)).sum();
                embed.setDescription("Total aspects: " + aspectsDown + " (" + aspectsUp + ")");
            }
            embed.addField("Players", playersBuilder.toString(), true);
            embed.addField(raid.name() + " Comps", countsBuilder.toString(), true);
            if (aspects) embed.addField("Aspects", aspectsBuilder.toString(), true);


            return embed.build();
        }

        @Override
        public ActionRow getButtons() {
            if (aspects) {
                Button down = Button.primary("aspects:" + trackerName + ":down", "i gave out aspects (round down)");
                Button up = Button.primary("aspects:" + trackerName + ":up", "i gave out aspects (round up)");
                return ActionRow.of(down, up);
            }
            return null;
        }
    }

    public static void startTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    Graids.updateAllCounts();
                } catch (Exception e) {
                    System.err.println("Error in GraidTrackerTimer");
                    e.printStackTrace();
                }
            }
        }, 9000, TimeUnit.MINUTES.toMillis(3));
    }

    static void updateAllCounts() {
        if (trackers.isEmpty()) return;

        GuildInfo guildInfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
        if (guildInfo == null || guildInfo.members == null) {
            System.err.println("null guildInfo");
            return;
        }

        Map<String, GuildRaids> oldCounts = new HashMap<>(totalCounts);
        Map<String, MemberInfo> allMembersByUsername = guildInfo.members.getAllMembersByUsername();

        for (Map.Entry<String, MemberInfo> entry : allMembersByUsername.entrySet()) {
            MemberInfo memberInfo = entry.getValue();
            if (memberInfo == null) continue;
            String username = memberInfo.username;
            GuildRaids guildRaids = memberInfo.globalData.currentGuildRaids;
            GuildRaids oldValues = oldCounts.get(username);
            if (oldValues != null && guildRaids.total < oldValues.total) {
                if (broken.add(username)) {
                    System.err.println("found a decrease in total graids comps? " + username + " old: " + oldValues.total + " new: " + guildRaids.total);
                }
            } else {
                if (broken.remove(username)) {
                    System.out.println(username + "'s graid count has returned to normal");
                }
                totalCounts.put(username, guildRaids);
                for (GraidTracker tracker : trackers.values()) {
                    tracker.updateCount(username, oldCounts.get(username));
                }
            }
        }

        for (GraidTracker tracker : trackers.values()) {
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
                trackers.get(name).stop(trackers);
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
        boolean total = Optional.ofNullable(event.getOption("total"))
                .map(OptionMapping::getAsBoolean)
                .orElse(false);


        GraidTracker tracker = new GraidTracker(name, raid, channel.getIdLong(), stickied, aspects, positionNumbers, total);
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
