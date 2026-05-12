package sidly.discord_bot.new_guild_endpoint;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.requests.restaction.MessageEditAction;
import sidly.discord_bot.MainEntrypoint;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Tracker {
    protected final ConcurrentHashMap<String, Integer> increaseCounts = new ConcurrentHashMap<>();
    protected Long messageId;
    protected final Long channelId;
    protected Long startTime;
    protected final String trackerName;
    protected final boolean stickied;

    public Tracker(Long channelId, String trackerName, boolean stickied) {
        this.channelId = channelId;
        this.trackerName = trackerName;
        this.stickied = stickied;
        this.startTime = System.currentTimeMillis();
    }

    public void updateMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public void stop(Map<String, ? extends Tracker> trackers) {
        if (channelId != null && messageId != null) {
            TextChannel channel = MainEntrypoint.jda.getTextChannelById(channelId);
            if (channel != null) {
                channel.retrieveMessageById(messageId).queue(message -> message.editMessageComponents().queue());
            }
        }
        trackers.remove(this.trackerName);
    }

    abstract MessageEmbed getEmbed();

    abstract ActionRow getButtons();

    protected void updateDisplay() {
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
