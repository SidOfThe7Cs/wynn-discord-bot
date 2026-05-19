package sidly.discord_bot.new_guild_endpoint;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
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
import sidly.discord_bot.RoleUtils;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.GuildInfo;
import sidly.discord_bot.api.sub.MemberInfo;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WarTracker extends Tracker {
    private static final Map<String, WarTracker> trackers = new HashMap<>();
    private static final Map<String, Integer> totalCounts = new HashMap<>();
    private static Timer timer;
    private static final Set<String> broken = new HashSet<>();
    private final boolean positionNumbers;

    public WarTracker(String trackerName, Long channelId, boolean stickied, boolean positionNumbers) {
        super(channelId, trackerName, stickied);
        this.positionNumbers = positionNumbers;
        trackers.put(trackerName, this);
    }

    public static void startTimer() {
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    updateAll();
                } catch (Exception e) {
                    System.err.println("Error in WarTrackerTimer" + e.getMessage());
                }
            }
        }, 19000, TimeUnit.MINUTES.toMillis(3));
    }

    private static void updateAll() {
        if (trackers.isEmpty()) return;

        GuildInfo guildInfo = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix));
        if (guildInfo == null || guildInfo.members == null) {
            System.err.println("null guildInfo");
            return;
        }

        Map<String, Integer> oldCounts = new HashMap<>(totalCounts);
        Map<String, MemberInfo> allMembersByUsername = guildInfo.members.getAllMembersByUsername();

        for (Map.Entry<String, MemberInfo> entry : allMembersByUsername.entrySet()) {
            String username = entry.getValue().username;
            int wars = entry.getValue().globalData.wars;
            Integer oldCount = oldCounts.get(username);
            if (oldCount != null && wars < oldCount) {
                if (broken.add(username)) {
                    System.err.println("found a decrease in total war comps? " + username + " old: " + oldCounts + " new: " + wars);
                }
            } else {
                if (broken.remove(username)) {
                    System.out.println(username + "'s war count has returned to normal");
                }
                totalCounts.put(username, wars);
                for (WarTracker tracker : trackers.values()) {
                    tracker.updateCount(username, oldCounts.get(username));
                }
            }
        }

        for (WarTracker tracker : trackers.values()) {
            tracker.updateDisplay();
        }
    }

    private void updateCount(String username, Integer oldCount) {
        Integer newCount = totalCounts.get(username);
        if (oldCount == null || newCount == null || newCount == 0) return;
        increaseCounts.merge(username, newCount - oldCount, Integer::sum);
    }

    private void restart() {
        startTime = System.currentTimeMillis();
        increaseCounts.clear();
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
        embed.setTitle("**War Tracker** started " + Utils.getDiscordTimestamp(startTime, true) + "\nlast updated " + currentTime);

        List<Map.Entry<String, Integer>> sortedEntries = increaseCounts.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .sorted(Map.Entry.comparingByValue())
                .toList()
                .reversed();

        StringBuilder playersBuilder = new StringBuilder();
        StringBuilder countsBuilder = new StringBuilder();

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

            // Check if adding this line would exceed the limit
            if (playersBuilder.length() + playerLine.length() <= 1021 &&
                    countsBuilder.length() + countLine.length() <= 1021
            ) {
                playersBuilder.append(playerLine);
                countsBuilder.append(countLine);
            } else {
                // Add a truncation message
                playersBuilder.append("...");
                countsBuilder.append("...");
                break;
            }
        }
        embed.addField("Players", playersBuilder.toString(), true);
        embed.addField("Wars", countsBuilder.toString(), true);

        return embed.build();
    }

    @Override
    public ActionRow getButtons() {
        Button button = Button.primary("warTracker:" + trackerName + ":reset", "reset");
        return ActionRow.of(button);
    }

    public static void startWarTracker(SlashCommandInteractionEvent event) {
        TextChannel channel = (TextChannel) event.getOption("channel").getAsChannel();
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
            event.reply("cannot create tracker " + name + " as it already exists").setEphemeral(true).queue();
            return;
        }

        boolean stickied = Optional.ofNullable(event.getOption("stickied"))
                .map(OptionMapping::getAsBoolean)
                .orElse(false);
        boolean positionNumbers = Optional.ofNullable(event.getOption("ranks"))
                .map(OptionMapping::getAsBoolean)
                .orElse(false);


        WarTracker tracker = new WarTracker(name, channel.getIdLong(), stickied, positionNumbers);
        MessageCreateAction messageAction = channel.sendMessageEmbeds(tracker.getEmbed());
        ActionRow buttons = tracker.getButtons();
        if (buttons != null) {
            messageAction.setComponents(buttons);
        }
        messageAction.queue(
                message -> tracker.updateMessageId(message.getIdLong())
        );
        event.reply("war tracker " + name + " started in " + channel.getAsMention()).setEphemeral(true).queue();
    }

    public static void buttonClicked(ButtonInteractionEvent event) {
        Member member = event.getMember();
        if (member == null) {
            event.reply("cmd must be used in a server").setEphemeral(true).queue();
            return;
        }
        boolean isChief = RoleUtils.hasRole(member, Config.Roles.ChiefRole);
        boolean isOwner = RoleUtils.hasRole(member, Config.Roles.OwnerRole);
        if (!isChief && !isOwner) {
            event.reply("you do not have perms to do this").setEphemeral(true).queue();
        }


        String[] parts = event.getComponentId().split(":");
        if (parts.length == 3) {
            String name = parts[1];
            String action = parts[2];
            trackers.get(name).restart();
            event.reply("restarted tracker").setEphemeral(true).queue();
            return;
        }
        event.reply("failed :(").setEphemeral(true).queue();
    }

}
