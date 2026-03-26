package sidly.discord_bot.v2.botapi;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;

public class CommandResult {
    private final String title;
    private final String text;
    private final boolean ethereal;

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public boolean isEthereal() {
        return ethereal;
    }

    public CommandResult(Builder builder) {
        this.ethereal = builder.ethereal;
        this.text = builder.text;
        this.title = builder.title;
    }

    public MessageEmbed toEmbed() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(Color.CYAN);
        if (!getTitle().isEmpty()) embedBuilder.setTitle(getTitle());
        String text = getText();
        String truncated = text.length() <= 1024 ? text : text.substring(0, 1021) + "...";
        embedBuilder.setDescription(truncated);

        return embedBuilder.build();
    }

    public static class Builder {
        private final String text;
        private String title = "";
        private boolean ethereal = false;

        public Builder(String text) {
            this.text = text;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder ethereal(boolean ethereal) {
            this.ethereal = ethereal;
            return this;
        }

        public CommandResult build() {
            return new CommandResult(this);
        }
    }
}
