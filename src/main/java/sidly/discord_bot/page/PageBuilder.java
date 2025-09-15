package sidly.discord_bot.page;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class PageBuilder {
    public static void handlePagination(ButtonInteractionEvent event) {
        event.deferEdit().queue();

        String[] parts = event.getComponentId().split(":");
        String key = parts[1];
        String direction = parts[2];
        PaginationState state = PaginationManager.get(key);
        if (state == null) return;

        int page = state.currentPage;
        if ("left".equals(direction)) {
            page--;
        } else if ("right".equals(direction)) {
            page++;
        }
        state.currentPage = page;

        EmbedBuilder embed = state.buildEmbedPage();

        Button leftButton = Button.primary("pagination:" + key + ":left", "◀️");
        Button rightButton = Button.primary("pagination:" + key + ":right", "▶️");

        if (embed == null) embed = new EmbedBuilder().setTitle("nothing");

        event.getHook().editOriginalEmbeds(embed.build())
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
        private CompletableFuture<EmbedBuilder> nextPage;
        private CompletableFuture<EmbedBuilder> prevPage;
        private int maxPages;
        private int lastPage;

        public PaginationState(pageConverterFunction converter, int currentPage, String title, int entriesPerPage) {
            this.converter = converter;
            this.currentPage = currentPage;
            this.title = title;
            this.entriesPerPage = entriesPerPage;
        }

        public void reset(List<?> sortedEntries) {
            this.lastPage = -5;
            this.currentPage = 0;
            this.sortedEntries = sortedEntries;
            this.maxPages = ((int) Math.ceil((double) this.sortedEntries.size() / (double) this.entriesPerPage)) - 1;
        }

        // called to display the page after changing this.currentPage remotely
        public EmbedBuilder buildEmbedPage() {
            boolean forward;
            boolean backward;
            if (this.lastPage == -5) {
                // First run special case
                forward = false;
                backward = false;
            } else {
                forward = (this.currentPage - this.lastPage) == 1;
                backward = (this.lastPage - this.currentPage) == 1;
            }

            if (this.currentPage > this.maxPages) this.currentPage = 0;
            else if (this.currentPage < 0) this.currentPage = this.maxPages;

            CompletableFuture<EmbedBuilder> currentFuture;

            if (forward) {
                currentFuture = this.nextPage;
            } else if (backward) {
                currentFuture = this.prevPage;
            } else if (currentPage == 0){
                currentFuture = generateEmbedAsync(this.currentPage);
            } else {
                System.out.println("you have confused the page builder");
                return generateEmbedAsync(this.currentPage).join();
            }

            // Preload next/prev for future moves
            this.nextPage = generateEmbedAsync(this.currentPage + 1);
            this.prevPage = generateEmbedAsync(this.currentPage - 1);

            this.lastPage = this.currentPage;

            // Block here until current page is ready
            return currentFuture.join();
        }
        public CompletableFuture<EmbedBuilder> generateEmbedAsync(int page) {
            if (page > maxPages) page = 0;
            else if (page < 0) page = maxPages;

            int finalPage = page;
            return CompletableFuture.supplyAsync(() -> {

                if (this.sortedEntries.isEmpty()) return null;

                EmbedBuilder embed = new EmbedBuilder();
                embed.setColor(Color.CYAN);


                embed.setTitle(this.title + " (Page " + (finalPage + 1) + "/" + (maxPages + 1) + ")");

                StringBuilder sb = new StringBuilder();


                //custom data handling
                if (this.equals(PaginationManager.get(PaginationIds.GUILD_STATS.name())) || this.equals(PaginationManager.get(PaginationIds.GUILD.name()))) {
                    sb.append(this.customData);
                }

                int start = finalPage * this.entriesPerPage;
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
            });
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
