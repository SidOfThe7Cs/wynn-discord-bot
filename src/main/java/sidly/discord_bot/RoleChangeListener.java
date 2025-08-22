package sidly.discord_bot;

import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.*;
import java.util.concurrent.*;

public class RoleChangeListener extends ListenerAdapter {

    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final Map<String, PendingChange> pendingChanges = new ConcurrentHashMap<>();
    private static ScheduledFuture<?> future;

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        handleChange(event.getMember(), event.getRoles(), true);
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        handleChange(event.getMember(), event.getRoles(), false);
    }

    private void handleChange(Member member, List<Role> roles, boolean added) {
        String memberId = member.getId();

        // Merge into existing pending change, or create new
        PendingChange change = pendingChanges.computeIfAbsent(memberId, id -> new PendingChange(member));

        for (Role role : roles) {
            if (added) {
                change.added.add(role);
                change.removed.remove(role);
            } else {
                change.removed.add(role);
                change.added.remove(role);
            }
        }

        // Reset timer for this member
        if (future != null && !future.isDone()) {
            future.cancel(false);
        }
        future = scheduler.schedule(this::flushChanges, 10, TimeUnit.SECONDS);
    }

    private void flushChanges() {
        StringBuilder sb = new StringBuilder();
        for (PendingChange entry : pendingChanges.values()) {
            if (entry == null) return;

            sb.append(entry.member.getAsMention()).append("\n");

            for (Role role : entry.added) {
                sb.append("added ").append(role.getAsMention()).append("\n");
            }
            for (Role role : entry.removed) {
                sb.append("removed ").append(role.getAsMention()).append("\n");
            }

            sb.append("\n");
        }
        Utils.sendToModChannel("Roles changed", sb.toString(), false);
    }

    private static class PendingChange {
        final Member member;
        final Set<Role> added = new HashSet<>();
        final Set<Role> removed = new HashSet<>();

        PendingChange(Member member) {
            this.member = member;
        }
    }
}

