package sidly.discord_bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ConsoleInterceptor extends PrintStream {
    private final PrintStream originalErr;
    private final JDA jda;

    // Shared buffer
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final Object lock = new Object();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public ConsoleInterceptor(PrintStream originalErr, JDA jda) {
        super(originalErr);
        this.originalErr = originalErr;
        this.jda = jda;

        // Schedule flush every 5 seconds
        scheduler.scheduleAtFixedRate(this::flushToDiscord, 5, 5, TimeUnit.SECONDS);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        super.write(b, off, len); // still print to console

        synchronized (lock) {
            buffer.write(b, off, len);

            // If buffer is too big, flush immediately
            if (buffer.size() >= 1500) {
                flushToDiscord();
            }
        }
    }

    private void flushToDiscord() {
        String logChannelId = ConfigManager.getConfigInstance().channels.get(Config.Channels.ConsoleLogChannel);
        TextChannel logChannel = jda.getTextChannelById(logChannelId);

        if (logChannel != null) {
            String msg;
            synchronized (lock) {
                if (buffer.size() == 0) return; // nothing to send
                msg = buffer.toString(StandardCharsets.UTF_8);
                buffer.reset();
            }

            // Truncate if still too long (Discord max ~2000 chars)
            String toSend = msg.length() > 1900 ? msg.substring(0, 1900) + "..." : msg;

            logChannel.sendMessage("```Ansi\n" + toSend + "```").queue();
        }
    }

    // Call this when shutting down the bot
    public void shutdown() {
        scheduler.shutdownNow();
        flushToDiscord(); // flush whatever is left
    }
}
