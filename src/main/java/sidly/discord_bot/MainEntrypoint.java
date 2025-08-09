package sidly.discord_bot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import sidly.discord_bot.commands.*;
import sidly.discord_bot.commands.demotion_promotion.PromotionCommands;
import sidly.discord_bot.commands.demotion_promotion.RequirementType;

import java.awt.*;
import java.util.Arrays;
import java.util.EnumSet;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class MainEntrypoint extends ListenerAdapter {

    private static boolean shuttingDown = false;
    public static void shutdown() {
        if (shuttingDown) return;
        shuttingDown = true;

        System.out.println("Shutting down");
        ConfigManager.save();
        System.exit(0);
    }

    private static void shutdown(SlashCommandInteractionEvent event) {
        shutdown();
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread(MainEntrypoint::shutdown));

        ConfigManager.load();
        String token = ConfigManager.getSetting(Config.Settings.Token);
        if (token == null || token.isEmpty()){
            System.err.println("please assign your bot token in the config file");
            shutdown();
        }

        JDA jda = JDABuilder.createLight(token, EnumSet.noneOf(GatewayIntent.class))
                .addEventListeners(new MainEntrypoint())
                .build();

        // You might need to reload your Discord client if you don't see the commands
        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(AllSlashCommands.shutdown.getBaseCommandData());
        AllSlashCommands.shutdown.setRequiredRole(ConfigManager.getSetting(Config.Settings.OwnerRole));
        AllSlashCommands.shutdown.setAction(MainEntrypoint::shutdown);


        commands.addCommands(AllSlashCommands.reloadconfig.getBaseCommandData());
        AllSlashCommands.reloadconfig.setRequiredRole(ConfigManager.getSetting(Config.Settings.OwnerRole));
        AllSlashCommands.reloadconfig.setAction(ConfigManager::reloadConfig);

        commands.addCommands(AllSlashCommands.editconfigoption.getBaseCommandData()
                .addOptions(
                        new OptionData(OptionType.STRING, "setting", "Choose a setting", true)
                                .addChoices(
                                        Arrays.stream(Config.Settings.values())
                                                .map(e -> new Command.Choice(e.name(), e.name()))
                                                .toArray(Command.Choice[]::new)
                                ),
                        new OptionData(OptionType.STRING, "new_value", "The new value for the setting", true)
                )
        );
        AllSlashCommands.editconfigoption.setRequiredRole(ConfigManager.getSetting(Config.Settings.ChiefRole));
        AllSlashCommands.editconfigoption.setAction(ConfigCommands::editConfigOption);

        commands.addCommands(AllSlashCommands.getconfigoptions.getBaseCommandData());
        AllSlashCommands.getconfigoptions.setAction(ConfigCommands::showConfigOptions);

        commands.addCommands(AllSlashCommands.checkforupdates.getBaseCommandData());
        AllSlashCommands.checkforupdates.setRequiredRole(ConfigManager.getSetting(Config.Settings.ChiefRole));
        AllSlashCommands.checkforupdates.setAction(UpdaterCommands::checkForUpdate);

        commands.addCommands(AllSlashCommands.getbotversion.getBaseCommandData());
        AllSlashCommands.getbotversion.setAction(UpdaterCommands::getBotVersion);

        commands.addCommands(AllSlashCommands.leaderboardguildxp.getBaseCommandData()
                .addOption(STRING, "guild_prefix", "e", true));
        AllSlashCommands.leaderboardguildxp.setAction(GuildCommands::showGuildXpLeaderboard);

        commands.addCommands(
                AllSlashCommands.adddemotionexeption.getBaseCommandData()
                        .addOption(STRING, "username", "e", true)
                        .addOption(INTEGER, "length", "in days")
        );

        commands.addCommands(
                AllSlashCommands.addinactivityexeption.getBaseCommandData()
                        .addOption(STRING, "username", "e", true)
                        .addOption(INTEGER, "length", "in days")
        );

        commands.addCommands(
                AllSlashCommands.addpromotionexeption.getBaseCommandData()
                        .addOption(STRING, "username", "e", true)
                        .addOption(INTEGER, "length", "in days")
        );

        commands.addCommands(AllSlashCommands.checkfordemotions.getBaseCommandData());

        commands.addCommands(AllSlashCommands.checkforinactivity.getBaseCommandData());

        commands.addCommands(AllSlashCommands.checkforpromotions.getBaseCommandData());



        commands.addCommands(AllSlashCommands.addPromotionRequirement.getBaseCommandData()
                .addOptions(
                        new OptionData(OptionType.STRING, "rank", "what rank to add the requirement too", true)
                                .addChoices(
                                        Arrays.stream(Utils.RankList.values())
                                                .map(e -> new Command.Choice(e.name(), e.name()))
                                                .toArray(Command.Choice[]::new)
                                ),
                        new OptionData(OptionType.STRING, "requirement", "the type of requirement to add", true)
                                .addChoices(
                                        Arrays.stream(RequirementType.values())
                                                .map(e -> new Command.Choice(e.name(), e.name()))
                                                .toArray(Command.Choice[]::new)
                                )
                )
                .addOption(INTEGER, "value", "the value for the requirement 0 is false 1 is true", true)
                .addOption(BOOLEAN, "required", "is this requirement always required", true));
        AllSlashCommands.addPromotionRequirement.setRequiredRole(ConfigManager.getSetting(Config.Settings.ChiefRole));
        AllSlashCommands.addPromotionRequirement.setAction(PromotionCommands::addRequirement);

        commands.addCommands(AllSlashCommands.getPromotionRequirements.getBaseCommandData());

        commands.addCommands(AllSlashCommands.checkPromotionProgress.getBaseCommandData());

        commands.addCommands(AllSlashCommands.setPromotionOptionalRequirement.getBaseCommandData());

        commands.addCommands(AllSlashCommands.removePromotionRequirement.getBaseCommandData());


        // Send the new set of commands to discord, this will override any existing global commands with the new set provided here
        commands.queue();
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // Only accept commands from guilds
        if (event.getGuild() == null) return;

        AllSlashCommands.valueOf(event.getName()).run(event);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] id = event.getComponentId().split(":"); // this is the custom id we specified in our button
        String authorId = id[0];
        String type = id[1];
        // Check that the button is for the user that clicked it, otherwise just ignore the event (let interaction fail)
        if (!authorId.equals(event.getUser().getId())) return;

        MessageChannel channel = event.getChannel();
        Message message = event.getMessage();
        MessageEmbed originalEmbed = message.getEmbeds().isEmpty() ? null : message.getEmbeds().getFirst();
        switch (type)
        {
            case "update.confirm":
                UpdaterCommands.update();
                break;
            case "say.like":
                if (originalEmbed != null) {
                    EmbedBuilder updated = new EmbedBuilder(originalEmbed)
                            .setColor(Color.GREEN)
                            .setDescription("✅ You clicked: " + event.getComponentId());

                    event.editMessageEmbeds(updated.build()).queue();
                }
                break;
            case "say.shrug":
                event.reply("shruggggged").setEphemeral(true).queue();
                break;
            case "say.dislike":
                event.reply("You disliked this message. 👎").setEphemeral(true).queue();
                break;
            default:
                event.reply("Unknown button clicked!").setEphemeral(true).queue();
        }
    }

    public static void say(SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        String content = "test \n";
        String pref = event.getOption("guild_prefix").getAsString();
        content += pref;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(":red_circle: buttons?")
                .setDescription(content)
                .setColor(Color.CYAN)
                .setFooter("Requested by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl())
                .setTimestamp(java.time.Instant.now());

        Button primary = Button.primary(userId + ":say.like", "👍 Like");
        Button secondary = Button.secondary(userId + ":say.shrug", "shrug");
        Button danger = Button.danger(userId + ":say.dislike", "👎 Dislike");

        event.replyEmbeds(embed.build())
                .addComponents(ActionRow.of(
                        primary,
                        secondary,
                        danger
                ))
                .queue();
    }
}