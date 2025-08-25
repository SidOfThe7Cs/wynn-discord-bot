package sidly.discord_bot.commands;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import sidly.discord_bot.Config;
import sidly.discord_bot.RoleUtils;
import sidly.discord_bot.Utils;

import java.util.*;

public class SelfAssignedRoles {
    private static Map<String, Map<StringSelectorIds, List<String>>> selections = new HashMap<>();

    public static void sendMessage(SlashCommandInteractionEvent event) {
        TextChannel channel = (TextChannel) event.getOption("channel").getAsChannel();

        Button button = Button.primary("configure_roles", "Configure Roles");

        channel.sendMessage("Click the button below to get roles for this discord!\n" +
                "Options:\n" +
                "• Class & Archetype roles\n" +
                "• Pronoun roles\n" +
                "• Guild Pingable roles").setComponents(ActionRow.of(button)).queue();

        event.reply("sent").setEphemeral(true).queue();
    }

    public static void sendWarMessage(SlashCommandInteractionEvent event) {
        TextChannel channel = (TextChannel) event.getOption("channel").getAsChannel();

        StringSelectMenu warMenu = StringSelectMenu.create(StringSelectorIds.WAR.name())
                .setPlaceholder("Select War Roles")
                .setMinValues(0)
                .setMaxValues(6)
                .addOptions(
                        SelectOption.of("Trial Solo", "trial_solo"),
                        SelectOption.of("Trial Dps", "trial_dps"),
                        SelectOption.of("Trial Tank", "trial_tank"),
                        SelectOption.of("Trial Healer", "trial_healer"),
                        SelectOption.of("Trial Eco", "trial_eco"),
                        SelectOption.of("War Ping", "war_ping")
                )
                .build();

        channel.sendMessage("If you are interested in warring, and want to help out, click this button below to get your roles!").setComponents(ActionRow.of(warMenu)).queue();
        event.reply("sent").setEphemeral(true).queue();
    }

    public static void sendEphemeralMessage(ButtonInteractionEvent event) {

        String userId = event.getUser().getId();
        Member member = event.getMember();

        Map<StringSelectorIds, List<String>> userSelections = selections.computeIfAbsent(userId, k -> new EnumMap<>(StringSelectorIds.class));
        List<String> classSelected = userSelections.computeIfAbsent(StringSelectorIds.CLASS, k -> new ArrayList<>());
        List<String> archetypeSelected = userSelections.computeIfAbsent(StringSelectorIds.ARCHETYPE, k -> new ArrayList<>());
        List<String> pingsSelected = userSelections.computeIfAbsent(StringSelectorIds.PINGS, k -> new ArrayList<>());
        List<String> pronounsSelected = userSelections.computeIfAbsent(StringSelectorIds.PRONOUNS, k -> new ArrayList<>());


        //add values from the members roles
        // Add all base classes
        if (RoleUtils.hasRole(member, Config.ClassRoles.Warrior)) classSelected.add("warrior");
        if (RoleUtils.hasRole(member, Config.ClassRoles.Mage)) classSelected.add("mage");
        if (RoleUtils.hasRole(member, Config.ClassRoles.Archer)) classSelected.add("archer");
        if (RoleUtils.hasRole(member, Config.ClassRoles.Assassin)) classSelected.add("assassin");
        if (RoleUtils.hasRole(member, Config.ClassRoles.Shaman)) classSelected.add("shaman");

        // Add archetypes for Warrior
        if (RoleUtils.hasRole(member, Config.ClassRoles.Fallen)) archetypeSelected.add("fallen");
        if (RoleUtils.hasRole(member, Config.ClassRoles.BattleMonk)) archetypeSelected.add("battle monk");
        if (RoleUtils.hasRole(member, Config.ClassRoles.Paladin)) archetypeSelected.add("paladin");

        // Add archetypes for Mage
        if (RoleUtils.hasRole(member, Config.ClassRoles.RiftWalker)) archetypeSelected.add("riftwalker");
        if (RoleUtils.hasRole(member, Config.ClassRoles.LightBender)) archetypeSelected.add("light bender");
        if (RoleUtils.hasRole(member, Config.ClassRoles.Arcanist)) archetypeSelected.add("arcanist");

        // Add archetypes for Archer
        if (RoleUtils.hasRole(member, Config.ClassRoles.Boltslinger)) archetypeSelected.add("boltslinger");
        if (RoleUtils.hasRole(member, Config.ClassRoles.Trapper)) archetypeSelected.add("trapper");
        if (RoleUtils.hasRole(member, Config.ClassRoles.Sharpshooter)) archetypeSelected.add("sharpshooter");

        // Add archetypes for Assassin
        if (RoleUtils.hasRole(member, Config.ClassRoles.Trickster)) archetypeSelected.add("trickster");
        if (RoleUtils.hasRole(member, Config.ClassRoles.Shadestepper)) archetypeSelected.add("shadestepper");
        if (RoleUtils.hasRole(member, Config.ClassRoles.Acrobat)) archetypeSelected.add("acrobat");

        // Add archetypes for Shaman
        if (RoleUtils.hasRole(member, Config.ClassRoles.Summoner)) archetypeSelected.add("summoner");
        if (RoleUtils.hasRole(member, Config.ClassRoles.Ritualist)) archetypeSelected.add("ritualist");
        if (RoleUtils.hasRole(member, Config.ClassRoles.Acolyte)) archetypeSelected.add("acolyte");

        // Pings
        if (RoleUtils.hasRole(member, Config.Roles.GiveawayRole)) pingsSelected.add("giveaway");
        if (RoleUtils.hasRole(member, Config.Roles.EventsRole)) pingsSelected.add("events");
        if (RoleUtils.hasRole(member, Config.Roles.AnniRole)) pingsSelected.add("annihilation");
        if (RoleUtils.hasRole(member, Config.Roles.GuildRaidsRole)) pingsSelected.add("graid");
        if (RoleUtils.hasRole(member, Config.Roles.BombBellRole)) pingsSelected.add("bomb_bell");

        // Pronouns
        if (RoleUtils.hasRole(member, Config.Roles.TheyThemRole)) pronounsSelected.add("they");
        if (RoleUtils.hasRole(member, Config.Roles.HeHimRole)) pronounsSelected.add("he");
        if (RoleUtils.hasRole(member, Config.Roles.SheHerRole)) pronounsSelected.add("she");


        Button applyBtn = Button.primary("apply-role-selection", "Apply ✅");

        // Send message
        event.reply("configure your roles").setEphemeral(true)
                .setComponents(
                        ActionRow.of(getClass(classSelected)),
                        ActionRow.of(getArchetypes(userId)),
                        ActionRow.of(getPings(pingsSelected)),
                        ActionRow.of(getPronouns(pronounsSelected)),
                        ActionRow.of(applyBtn)
                )
                .queue();

    }

    public static void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String userId = event.getUser().getId();
        StringSelectorIds id = StringSelectorIds.valueOf(event.getComponentId());

        switch (id) {
            case StringSelectorIds.CLASS -> {
                selections.get(userId).put(id, event.getValues());

                List<ActionRow> rows = event.getMessage().getActionRows();

                List<ActionRow> newRows = new ArrayList<>(rows);
                newRows.set(0, ActionRow.of(getClass(selections.get(userId).get(StringSelectorIds.CLASS))));
                newRows.set(1, ActionRow.of(getArchetypes(userId)));
                newRows.set(2, ActionRow.of(getPings(selections.get(userId).get(StringSelectorIds.PINGS))));
                newRows.set(3, ActionRow.of(getPronouns(selections.get(userId).get(StringSelectorIds.PRONOUNS))));

                event.editComponents(newRows).queue();

            }

            case StringSelectorIds.ARCHETYPE, StringSelectorIds.PINGS, StringSelectorIds.PRONOUNS -> {
                selections.get(userId).put(id, event.getValues());
                event.deferEdit().queue();
            }


            case WAR -> {
                Map<String, Config.Roles> VALUE_TO_ROLE = Map.of(
                        "trial_solo", Config.Roles.TrialSoloRole,
                        "trial_dps", Config.Roles.TrialDpsRole,
                        "trial_tank", Config.Roles.TrialTankRole,
                        "trial_healer", Config.Roles.TrialHealerRole,
                        "trial_eco", Config.Roles.TrialEcoRole,
                        "war_ping", Config.Roles.WarPingRole
                );

                StringBuilder sb = new StringBuilder();
                List<String> values = event.getValues();
                VALUE_TO_ROLE.forEach((key, role) -> {
                    if (values.contains(key)) {
                        sb.append(RoleUtils.addRole(event.getMember(), role));
                    } else {
                        sb.append(RoleUtils.removeRole(event.getMember(), role));
                    }
                });

                if (values.isEmpty()) {
                    sb.append(RoleUtils.removeRole(event.getMember(), Config.Roles.WarrerRole));
                } else sb.append(RoleUtils.addRole(event.getMember(), Config.Roles.WarrerRole));

                event.reply(sb.toString()).setEphemeral(true).queue();
            }

        }
    }

    public static StringSelectMenu getArchetypes(String userId) {
        boolean hasClass = !selections.get(userId).get(StringSelectorIds.CLASS).isEmpty();
        List<String> selected = selections.get(userId).get(StringSelectorIds.ARCHETYPE);

        if (hasClass) {
            String chosenClass = selections.get(userId).get(StringSelectorIds.CLASS).getFirst();

            // Generate archetype dropdown based on class
            if (chosenClass.equals("none")) {
                selections.get(userId).put(StringSelectorIds.ARCHETYPE, new ArrayList<>());
            }

            return switch (chosenClass) {
                case "warrior" -> StringSelectMenu.create(StringSelectorIds.ARCHETYPE.name())
                        .setPlaceholder("Select your favorite archetype")
                        .addOptions(
                                SelectOption.of("Fallen", "fallen").withDefault(selected.contains("fallen")),
                                SelectOption.of("Battle Monk", "battle monk").withDefault(selected.contains("battle monk")),
                                SelectOption.of("Paladin", "paladin").withDefault(selected.contains("paladin"))
                        ).build();

                case "mage" -> StringSelectMenu.create(StringSelectorIds.ARCHETYPE.name())
                        .setPlaceholder("Select your favorite archetype")
                        .addOptions(
                                SelectOption.of("Riftwalker", "riftwalker").withDefault(selected.contains("riftwalker")),
                                SelectOption.of("Light Bender", "light bender").withDefault(selected.contains("light bender")),
                                SelectOption.of("Arcanist", "arcanist").withDefault(selected.contains("arcanist"))
                        ).build();

                case "archer" -> StringSelectMenu.create(StringSelectorIds.ARCHETYPE.name())
                        .setPlaceholder("Select your favorite archetype")
                        .addOptions(
                                SelectOption.of("Boltslinger", "boltslinger").withDefault(selected.contains("boltslinger")),
                                SelectOption.of("Trapper", "trapper").withDefault(selected.contains("trapper")),
                                SelectOption.of("Sharpshooter", "sharpshooter").withDefault(selected.contains("sharpshooter"))
                        ).build();

                case "assassin" -> StringSelectMenu.create(StringSelectorIds.ARCHETYPE.name())
                        .setPlaceholder("Select your favorite archetype")
                        .addOptions(
                                SelectOption.of("Trickster", "trickster").withDefault(selected.contains("trickster")),
                                SelectOption.of("Shadestepper", "shadestepper").withDefault(selected.contains("shadestepper")),
                                SelectOption.of("Acrobat", "acrobat").withDefault(selected.contains("acrobat"))
                        ).build();

                case "shaman" -> StringSelectMenu.create(StringSelectorIds.ARCHETYPE.name())
                        .setPlaceholder("Select your favorite archetype")
                        .addOptions(
                                SelectOption.of("Summoner", "summoner").withDefault(selected.contains("summoner")),
                                SelectOption.of("Ritualist", "ritualist").withDefault(selected.contains("ritualist")),
                                SelectOption.of("Acolyte", "acolyte").withDefault(selected.contains("acolyte"))
                        ).build();

                default -> StringSelectMenu.create(StringSelectorIds.ARCHETYPE.name())
                        .setPlaceholder("No archetypes found")
                        .addOptions(
                                SelectOption.of("None", "none")
                        )
                        .setDisabled(true)
                        .build();
            };

        } else {
            selections.get(userId).put(StringSelectorIds.ARCHETYPE, new ArrayList<>());
            return StringSelectMenu.create(StringSelectorIds.ARCHETYPE.name())
                    .setPlaceholder("Select a class first")
                    .addOptions(
                            SelectOption.of("None", "none")
                    )
                    .setDisabled(true)
                    .build();
        }
    }

    public static StringSelectMenu getClass(List<String> selected) {
        return StringSelectMenu.create(StringSelectorIds.CLASS.name())
                .setPlaceholder("Choose your favorite class")
                .addOptions(
                        SelectOption.of("None", "none").withDefault(selected.contains("none")),
                        SelectOption.of("Warrior", "warrior").withDefault(selected.contains("warrior")),
                        SelectOption.of("Mage", "mage").withDefault(selected.contains("mage")),
                        SelectOption.of("Shaman", "shaman").withDefault(selected.contains("shaman")),
                        SelectOption.of("Archer", "archer").withDefault(selected.contains("archer")),
                        SelectOption.of("Assassin", "assassin").withDefault(selected.contains("assassin"))
                )
                .build();
    }

    public static StringSelectMenu getPings(List<String> selected){
        return StringSelectMenu.create(StringSelectorIds.PINGS.name())
                .setPlaceholder("Select pings")
                .setMinValues(0)
                .setMaxValues(5)
                .addOptions(
                        SelectOption.of("Giveaway", "giveaway").withDefault(selected.contains("giveaway")),
                        SelectOption.of("Events", "events").withDefault(selected.contains("events")),
                        SelectOption.of("Annihilation", "annihilation").withDefault(selected.contains("annihilation")),
                        SelectOption.of("Guild Raid", "graid").withDefault(selected.contains("graid")),
                        SelectOption.of("Bomb Bell", "bomb_bell").withDefault(selected.contains("bomb_bell"))
                )
                .build();
    }

    private static StringSelectMenu getPronouns(List<String> selected) {
        return StringSelectMenu.create(StringSelectorIds.PRONOUNS.name())
                .setPlaceholder("Select pronouns")
                .setMinValues(0)
                .setMaxValues(3)
                .addOptions(
                        SelectOption.of("They/Them", "they").withDefault(selected.contains("they")),
                        SelectOption.of("He/Him", "he").withDefault(selected.contains("he")),
                        SelectOption.of("She/Her", "she").withDefault(selected.contains("she"))
                )
                .build();
    }

    public enum StringSelectorIds {
        CLASS,
        ARCHETYPE,
        PINGS,
        WAR,
        PRONOUNS
    }
    
    public static void applySelections(ButtonInteractionEvent event) {
        Member member = event.getMember();
        Map<StringSelectorIds, List<String>> userSelections = selections.get(member.getId());
        StringBuilder sb = new StringBuilder();

        sb.append(RoleUtils.removeClassRolesExcept(member, Config.ClassRoles.getRoleFromName(userSelections.get(StringSelectorIds.CLASS).getFirst())));
        sb.append(RoleUtils.removeArchetypeRolesExcept(member, Config.ClassRoles.getRoleFromName(userSelections.get(StringSelectorIds.ARCHETYPE).getFirst())));

        List<String> pings = userSelections.get(StringSelectorIds.PINGS);
        List<String> pronouns = userSelections.get(StringSelectorIds.PRONOUNS);

        sb.append(pings.contains("giveaway") ? RoleUtils.addRole(member, Config.Roles.GiveawayRole) : RoleUtils.removeRole(member, Config.Roles.GiveawayRole));
        sb.append(pings.contains("events") ? RoleUtils.addRole(member, Config.Roles.EventsRole) : RoleUtils.removeRole(member, Config.Roles.EventsRole));
        sb.append(pings.contains("annihilation") ? RoleUtils.addRole(member, Config.Roles.AnniRole) : RoleUtils.removeRole(member, Config.Roles.AnniRole));
        sb.append(pings.contains("graid") ? RoleUtils.addRole(member, Config.Roles.GuildRaidsRole) : RoleUtils.removeRole(member, Config.Roles.GuildRaidsRole));
        sb.append(pings.contains("bomb_bell") ? RoleUtils.addRole(member, Config.Roles.BombBellRole) : RoleUtils.removeRole(member, Config.Roles.BombBellRole));

        sb.append(pronouns.contains("they") ? RoleUtils.addRole(member, Config.Roles.TheyThemRole) : RoleUtils.removeRole(member, Config.Roles.TheyThemRole));
        sb.append(pronouns.contains("he") ? RoleUtils.addRole(member, Config.Roles.HeHimRole) : RoleUtils.removeRole(member, Config.Roles.HeHimRole));
        sb.append(pronouns.contains("she") ? RoleUtils.addRole(member, Config.Roles.SheHerRole) : RoleUtils.removeRole(member, Config.Roles.SheHerRole));

        selections.remove(member.getId());

        event.getMessage().delete().queue();
        event.replyEmbeds(Utils.getEmbed("Roles Changed", sb.toString())).setEphemeral(true).queue();

    }
}
