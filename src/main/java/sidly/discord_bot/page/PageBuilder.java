package sidly.discord_bot.page;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

        EmbedBuilder embed = state.buildEmbedPage();

        Button leftButton = Button.primary("pagination:" + key + ":left", "◀️");
        Button rightButton = Button.primary("pagination:" + key + ":right", "▶️");

        if (embed == null) embed = new EmbedBuilder().setTitle("nothing");

        event.editMessageEmbeds(embed.build())
                .setComponents(ActionRow.of(leftButton, rightButton))
                .queue();
    }

    public static ActionRow getPaginationActionRow(PaginationIds id){
        Button leftButton = Button.primary("pagination:" + id.name() + ":left", "◀️");
        Button rightButton = Button.primary("pagination:" + id.name() + ":right", "▶️");
        return ActionRow.of(leftButton, rightButton);
    }

    public static class PaginationState {
        public int currentPage;
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
        }

        public void reset(List<?> sortedEntries) {
            currentPage = 0;
            this.sortedEntries = sortedEntries;
        }

        public EmbedBuilder buildEmbedPage() {
            if (this.sortedEntries.isEmpty()) return null;

            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.CYAN);

            int maxPages = (int) Math.ceil((double) this.sortedEntries.size() / (double) this.entriesPerPage);

            this.currentPage = (this.currentPage >= maxPages) ? 0 : this.currentPage;
            embed.setTitle(this.title + " (Page " + (this.currentPage + 1) + "/" + (maxPages) + ")");

            StringBuilder sb = new StringBuilder();


            //custom data handling
            if (this.equals(PaginationManager.get(PaginationIds.GUILD_STATS.name())) || this.equals(PaginationManager.get(PaginationIds.GUILD.name()))) {
                sb.append(this.customData);
            }


            int start = this.currentPage * this.entriesPerPage;
            int end = Math.min(start + this.entriesPerPage, this.sortedEntries.size());


            for (int i = start; i < end; i++) {
                String entry;
                if (this.converter != null) {
                    entry = this.converter.convert(this.sortedEntries.get(i));
                } else entry = (String) this.sortedEntries.get(i);
                sb.append(entry);
            }

            embed.setDescription(sb.toString());

            return embed;
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
