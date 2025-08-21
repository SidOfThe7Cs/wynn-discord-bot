package sidly.discord_bot.page;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import sidly.discord_bot.Utils;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

        EmbedBuilder embed = buildEmbedPage(state);

        Button leftButton = Button.primary("pagination:" + key + ":left", "◀️");
        Button rightButton = Button.primary("pagination:" + key + ":right", "▶️");

        if (embed == null) embed = new EmbedBuilder().setTitle("nothing");

        event.editMessageEmbeds(embed.build())
                .setComponents(ActionRow.of(leftButton, rightButton))
                .queue();
    }

    public static EmbedBuilder buildEmbedPage(PaginationState state) {
        if (state.sortedEntries.isEmpty()) return null;

        EmbedBuilder embed = new EmbedBuilder();
        embed.setColor(Color.CYAN);

        int maxPages = (int) Math.ceil((double) state.sortedEntries.size() / (double) state.entriesPerPage);

        state.currentPage = (state.currentPage >= maxPages) ? 0 : state.currentPage;
        embed.setTitle(state.title + " (Page " + (state.currentPage + 1) + "/" + (maxPages) + ")");

        StringBuilder sb = new StringBuilder();

        if (state.equals(PaginationManager.get(PaginationIds.GUILD_STATS.name()))) {
            sb.append(state.customData);
        }


        int start = state.currentPage * state.entriesPerPage;
        int end = Math.min(start + state.entriesPerPage, state.sortedEntries.size());


        for (int i = start; i < end; i++) {
            String entry;
            if (state.converter != null) {
                entry = state.converter.convert(state.sortedEntries.get(i));
            } else entry = (String) state.sortedEntries.get(i);
            sb.append(entry);
        }

        embed.setDescription(sb.toString());

        return embed;
    }

    public static ActionRow getPaginationActionRow(PaginationIds id){
        Button leftButton = Button.primary("pagination:" + id.name() + ":left", "◀️");
        Button rightButton = Button.primary("pagination:" + id.name() + ":right", "▶️");
        return ActionRow.of(leftButton, rightButton);
    }

    public static class PaginationState {
        public int currentPage;
        public long lastUpdated;
        public String title;
        public int entriesPerPage;
        public pageConverterFunction converter;
        public Object customData;

        public List<?> sortedEntries;

        public PaginationState(pageConverterFunction converter, int currentPage, String title, int entriesPerPage) {
            this.converter = converter;
            this.currentPage = currentPage;
            this.title = title;
            this.entriesPerPage = entriesPerPage;
            this.lastUpdated = System.currentTimeMillis();
        }

        public void reset(List<?> sortedEntries) {
            lastUpdated = System.currentTimeMillis();
            currentPage = 0;
            this.sortedEntries = sortedEntries;
        }
    }

    public static class PaginationManager {
        private static final Map<String, PaginationState> states = new HashMap<>();

        public static void register(String name, pageConverterFunction converter, String title, int entriesPerPage) {
            states.put(name, new PaginationState(converter, 0, title, entriesPerPage));
        }

        public static void register(String name, String title, int entriesPerPage) {
            states.put(name, new PaginationState(null, 0, title, entriesPerPage));
        }

        public static PaginationState get(String name) {
            return states.get(name);
        }
    }
}
