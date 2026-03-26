package sidly.discord_bot.v2.botapi;

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ButtonManager extends ListenerAdapter {
    // TODO buttons should use related command name as the first section, split the string by a symbol and move to custom class
    Map<String, Consumer<ButtonInteractionEvent>> buttons = new HashMap<>();
    Map<String, Long> expirations = new HashMap<>();

    // TODO before shutdown clear all buttons maybe?
    private String getNextId() {
        long now = System.currentTimeMillis();
        String id = null;
        for (Map.Entry<String, Long> entry : expirations.entrySet()) {
            if (now >= entry.getValue()) {
                // TODO make the real button not clickable (might require also storing message link or smth)
                id = entry.getKey();
            }
        }
        if (id != null) {
            buttons.remove(id);
            expirations.remove(id);
            return id;
        } else return String.valueOf(buttons.size()); // TODO make sure this cant create holes in the map and therefor overwrite and also make thread safe
    }

    public Button createButton(String display, Consumer<ButtonInteractionEvent> callback, Duration timeout) {
        long now = System.currentTimeMillis();
        String id = getNextId();
        buttons.put(id, callback);
        expirations.put(id, now + timeout.toMillis());
        return Button.primary(id ,display);
    }
}
