package sidly.discord_bot.page;

import kotlin.Function;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import sidly.discord_bot.commands.GuildCommands;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class PageBuilder {
    public static void handlePagination(ButtonInteractionEvent event) {
        String[] parts = event.getComponentId().split(":");
        String key = parts[1];
        String direction = parts[2];
        PaginationState state = PaginationManager.get(key);
        if (state == null) return;

        int page = state.currentPage;
        if ("left".equals(direction)) {
            page = Math.max(0, page - 1);
        } else if ("right".equals(direction)) {
            page++;
        }
        state.currentPage = page;

        EmbedBuilder embed = state.function.get();

        Button leftButton = Button.primary("pagination:" + key + ":left", "◀️");
        Button rightButton = Button.primary("pagination:" + key + ":right", "▶️");

        if (embed == null) embed = new EmbedBuilder().setTitle("nothing");

        event.editMessageEmbeds(embed.build())
                .setComponents(ActionRow.of(leftButton, rightButton))
                .queue();
    }

    public static EmbedBuilder buildEmbedPage(List<String> sortedEntries, int page, int entriesPerPage, String title) {
        if (sortedEntries.isEmpty()) return null;
        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.CYAN);

        embed.setTitle(title + " (Page " + (page + 1) + ")");

        StringBuilder sb = new StringBuilder();

        int start = page * entriesPerPage;
        int end = Math.min(start + entriesPerPage, sortedEntries.size());

        for (int i = start; i < end; i++) {
            String entry = sortedEntries.get(i);
            sb.append(entry);
        }

        embed.setDescription(sb.toString());
        return embed;
    }


    public static class PaginationState {
        public Supplier<EmbedBuilder> function;
        public int currentPage;

        public PaginationState(Supplier<EmbedBuilder> function, int currentPage) {
            this.function = function;
            this.currentPage = currentPage;
        }
    }

    public static class PaginationManager {
        private static final Map<String, PaginationState> functions =new HashMap<>();

        public static void register(String name, Supplier<EmbedBuilder> function) {
            functions.put(name, new PaginationState(function, 0));
        }

        public static PaginationState get(String name) {
            return functions.get(name);
        }

        public static void reset(String name) {
            functions.get(name).currentPage = 0;
        }

    }
}
