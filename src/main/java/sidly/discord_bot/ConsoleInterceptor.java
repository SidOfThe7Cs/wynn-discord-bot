package sidly.discord_bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class ConsoleInterceptor extends PrintStream {
    private final PrintStream originalErr;
    private final JDA jda;

    // Buffer to collect output before sending
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public ConsoleInterceptor(PrintStream originalErr, JDA jda) {
        super(originalErr);
        this.originalErr = originalErr;
        this.jda = jda;
    }

    @Override
    public void write(byte[] b, int off, int len) {
        super.write(b, off, len); // still print to console
        buffer.write(b, off, len);

        // Optional: send message if you see a newline to avoid flooding
        String currentContent = buffer.toString(StandardCharsets.UTF_8);
        if (currentContent.endsWith("\n")) {
            sendToDiscord(currentContent);
            buffer.reset();
        }
    }

    private void sendToDiscord(String msg) {
        String logChannelId = ConfigManager.getConfigInstance().channels.get(Config.Channels.ConsoleLogChannel);
        TextChannel logChannel = jda.getTextChannelById(logChannelId);
        if (logChannel != null) {
            // Discord has message length limits, so truncate if too long
            String toSend = msg.length() > 1900 ? msg.substring(0, 1900) + "..." : msg;
            logChannel.sendMessage("```Ansi\n" + toSend + "```").queue();
        }
    }
}
