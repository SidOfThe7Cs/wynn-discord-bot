package sidly.discord_bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import sidly.discord_bot.commands.*;
import sidly.discord_bot.commands.demotion_promotion.PlaytimeCommands;
import sidly.discord_bot.commands.demotion_promotion.PromotionCommands;
import sidly.discord_bot.commands.demotion_promotion.RequirementType;
import sidly.discord_bot.timed_actions.UpdatePlayers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class MainEntrypoint extends ListenerAdapter {

    private static boolean shuttingDown = false;
    public static void shutdown() {
        if (shuttingDown) return;
        shuttingDown = true;

        System.out.println("Shutting down");
        UpdatePlayers.shutdown();
        ConfigManager.save();
        System.exit(0);
    }

    private static void shutdown(SlashCommandInteractionEvent event) {
        shutdown();
    }


    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void main(String[] args) throws InterruptedException {

        Runtime.getRuntime().addShutdownHook(new Thread(MainEntrypoint::shutdown));

        ConfigManager.load();
        String token = ConfigManager.getConfigInstance().other.get(Config.Settings.Token);
        if (token == null || token.isEmpty()){
            System.err.println("please assign your bot token in the config file located at " + ConfigManager.CONFIG_FILE.getAbsolutePath());
            shutdown();
        }

        JDA jda = JDABuilder.createLight(token, EnumSet.noneOf(GatewayIntent.class))
                .addEventListeners(new MainEntrypoint())
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .build();

        System.setErr(new ConsoleInterceptor(System.err, jda));
        System.setOut(new ConsoleInterceptor(System.out, jda));


        // You might need to reload your Discord client if you don't see the commands
        CommandListUpdateAction commands = jda.updateCommands();

        commands.addCommands(AllSlashCommands.shutdown.getBaseCommandData());
        AllSlashCommands.shutdown.setAction(MainEntrypoint::shutdown);


        commands.addCommands(AllSlashCommands.reloadconfig.getBaseCommandData());
        AllSlashCommands.reloadconfig.setAction(ConfigManager::reloadConfig);

        commands.addCommands(AllSlashCommands.editconfigother.getBaseCommandData()
                .addOptions(
                        new OptionData(OptionType.STRING, "setting_name", "Choose a setting", true)
                                .addChoices(
                                        Arrays.stream(Config.Settings.values())
                                                .map(e -> new Command.Choice(e.name(), e.name()))
                                                .toArray(Command.Choice[]::new)
                                ),
                        new OptionData(OptionType.STRING, "setting_other", "The new value for the setting", true)
                )
        );
        AllSlashCommands.editconfigother.setAction(ConfigCommands::editConfigSettingOther);

        commands.addCommands(AllSlashCommands.editconfigrole.getBaseCommandData()
                .addOptions(
                        new OptionData(OptionType.STRING, "config_role", "Choose a setting", true).setAutoComplete(true),
                        new OptionData(ROLE, "role", "The new value for the setting", true)
                )
        );
        AllSlashCommands.editconfigrole.setAction(ConfigCommands::editConfigRole);

        commands.addCommands(AllSlashCommands.editconfigchannel.getBaseCommandData()
                .addOptions(
                        new OptionData(OptionType.STRING, "channel_name", "Choose a setting", true)
                                .addChoices(
                                        Arrays.stream(Config.Channels.values())
                                                .map(e -> new Command.Choice(e.name(), e.name()))
                                                .toArray(Command.Choice[]::new)
                                ),
                        new OptionData(CHANNEL, "channel", "The new value for the setting", true)
                )
        );
        AllSlashCommands.editconfigchannel.setAction(ConfigCommands::editConfigChannel);

        commands.addCommands(AllSlashCommands.editconfiglvlrole.getBaseCommandData()
                .addOptions(
                        new OptionData(OptionType.STRING, "role_name", "Choose a role", true)
                                .addChoices(
                                        Arrays.stream(Config.LvlRoles.values())
                                                .map(e -> new Command.Choice(e.name(), e.name()))
                                                .toArray(Command.Choice[]::new)
                                ),
                        new OptionData(ROLE, "role", "The id for the role", true)
                )
        );
        AllSlashCommands.editconfiglvlrole.setAction(ConfigCommands::editConfigLvlRoleOption);

        commands.addCommands(AllSlashCommands.getconfigoptions.getBaseCommandData());
        AllSlashCommands.getconfigoptions.setAction(ConfigCommands::showConfigOptions);

        commands.addCommands(AllSlashCommands.checkforupdates.getBaseCommandData());
        AllSlashCommands.checkforupdates.setAction(UpdaterCommands::checkForUpdate);

        commands.addCommands(AllSlashCommands.getbotversion.getBaseCommandData());
        AllSlashCommands.getbotversion.setAction(UpdaterCommands::getBotVersion);

        commands.addCommands(AllSlashCommands.leaderboardguildxp.getBaseCommandData()
                .addOption(STRING, "guild_prefix", "e", true));
        AllSlashCommands.leaderboardguildxp.setAction(GuildCommands::showGuildXpLeaderboard);

        commands.addCommands(AllSlashCommands.online.getBaseCommandData()
                .addOption(STRING, "guild_prefix", "e", true));
        AllSlashCommands.online.setAction(GuildCommands::showOnlineMembers);

        commands.addCommands(AllSlashCommands.verify.getBaseCommandData()
                .addOption(STRING, "username", "e", true));
        AllSlashCommands.verify.setAction(VerificationCommands::verify);

        commands.addCommands(AllSlashCommands.listcommands.getBaseCommandData());
        AllSlashCommands.listcommands.setAction(HelpCommands::listCommands);

        commands.addCommands(AllSlashCommands.setrolerequirement.getBaseCommandData().addOptions(
                new OptionData(OptionType.STRING, "command", "Command to add requirement to", true).setAutoComplete(true),
                new OptionData(ROLE, "role", "the required role", true)
        ));
        AllSlashCommands.setrolerequirement.setAction(RoleRequirementCommands::setRoleRequirement);

        commands.addCommands(AllSlashCommands.removerolerequirement.getBaseCommandData().addOptions(
                new OptionData(STRING, "command", "what command to remove the requirement from", true).setAutoComplete(true)
        ));
        AllSlashCommands.removerolerequirement.setAction(RoleRequirementCommands::removeRoleRequirement);

        commands.addCommands(AllSlashCommands.removeverification.getBaseCommandData()
                .addOption(STRING, "user_id", "discord id of user to unverify", true));
        AllSlashCommands.removeverification.setAction(VerificationCommands::removeVerification);

        commands.addCommands(AllSlashCommands.updateplayerroles.getBaseCommandData()
                .addOption(USER, "user", "user", true));
        AllSlashCommands.updateplayerroles.setAction(VerificationCommands::updateRoles);

        commands.addCommands(AllSlashCommands.getratelimitinfo.getBaseCommandData());
        AllSlashCommands.getratelimitinfo.setAction(RateLimitCommands::getRateLimitInfo);

        commands.addCommands(AllSlashCommands.addchannelrestriction.getBaseCommandData()
                .addOption(CHANNEL, "channel", "channel", true)
                .addOptions(
                new OptionData(OptionType.STRING, "allowed", "whitelisted / blacklisted / default", true)
                        .addChoices(
                                new Command.Choice("whitelisted", "true"),
                                new Command.Choice("blacklisted", "false"),
                                new Command.Choice("default", "null")
                        )
        ));
        AllSlashCommands.addchannelrestriction.setAction(ChannelRestrinctionCommands::addRestriction);

        commands.addCommands(AllSlashCommands.getallplayersaverageplaytime.getBaseCommandData()
                .addOption(INTEGER, "weeks", "average over the last number of week", true));
        AllSlashCommands.getallplayersaverageplaytime.setAction(PlaytimeCommands::getAllPlayersPlaytime);

        commands.addCommands(
                AllSlashCommands.adddemotionexeption.getBaseCommandData()
                        .addOption(USER, "user", "e", true)
                        .addOption(INTEGER, "length", "in days")
        );

        commands.addCommands(
                AllSlashCommands.addinactivityexeption.getBaseCommandData()
                        .addOption(USER, "user", "e", true)
                        .addOption(INTEGER, "length", "in days")
        );

        commands.addCommands(
                AllSlashCommands.addpromotionexeption.getBaseCommandData()
                        .addOption(USER, "user", "e", true)
                        .addOption(INTEGER, "length", "in days")
        );

        commands.addCommands(AllSlashCommands.checkfordemotions.getBaseCommandData());

        commands.addCommands(AllSlashCommands.checkforinactivity.getBaseCommandData());

        commands.addCommands(AllSlashCommands.checkforpromotions.getBaseCommandData());



        commands.addCommands(AllSlashCommands.addpromotionrequirement.getBaseCommandData().addOptions(
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
        AllSlashCommands.addpromotionrequirement.setAction(PromotionCommands::addRequirement);

        commands.addCommands(AllSlashCommands.getpromotionrequirements.getBaseCommandData());
        AllSlashCommands.getpromotionrequirements.setAction(PromotionCommands::getRequirements);

        commands.addCommands(AllSlashCommands.checkpromotionprogress.getBaseCommandData()
                .addOption(STRING, "username", "e", true));
        AllSlashCommands.checkpromotionprogress.setAction(PromotionCommands::checkPromotionProgress);

        commands.addCommands(AllSlashCommands.setpromotionoptionalrequirement.getBaseCommandData().addOptions(
                new OptionData(OptionType.STRING, "rank", "what rank to set for", true)
                        .addChoices(
                                Arrays.stream(Utils.RankList.values())
                                        .map(e -> new Command.Choice(e.name(), e.name()))
                                        .toArray(Command.Choice[]::new)
                        ),
                new OptionData(INTEGER, "value", "the number of required optional requirements", true)
        ));
        AllSlashCommands.setpromotionoptionalrequirement.setAction(PromotionCommands::setPromotionOptionalRequirement);

        commands.addCommands(AllSlashCommands.removepromotionrequirement.getBaseCommandData().addOptions(
                        new OptionData(OptionType.STRING, "rank", "what rank to remove the requirement from", true)
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
        );
        AllSlashCommands.removepromotionrequirement.setAction(PromotionCommands::removeRequirement);


        // Send the new set of commands to discord, this will override any existing global commands with the new set provided here
        commands.queue();

        jda.awaitReady();

        UpdatePlayers.init(jda);
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        // Only accept commands from guilds
        if (event.getGuild() == null) return;

        AllSlashCommands.valueOf(event.getName()).run(event);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String fullId = event.getComponentId();

        if (fullId.startsWith("verify_confirm_") || fullId.startsWith("verify_deny_")) {
            // Remove prefix
            String payload = fullId.substring(fullId.indexOf('_', 7) + 1);
            // Explanation: indexOf('_', 7) finds the underscore after "verify_" prefix,
            // then +1 to skip it and get the payload after that.

            // Payload format: "userId|username"
            String[] parts = payload.split("\\|", 2);
            if (parts.length < 2) {
                event.reply("Invalid button data").setEphemeral(true).queue();
                return;
            }
            String userId = parts[0];
            String username = parts[1];

            event.getGuild().retrieveMemberById(userId).queue(
                    member -> {
                        if (member == null) {
                            event.reply("User not found." + userId + username).setEphemeral(true).queue();
                            return;
                        }

                        if (fullId.startsWith("verify_confirm_")) {
                            event.reply("Verification approved!").setEphemeral(true).queue();

                            // after the private channel opens run the verificationComplete and then when that completes send the user the results
                            member.getUser().openPrivateChannel().queue(privateChannel -> {
                                CompletableFuture<String> verificationFuture = VerificationCommands.completeVerification(member, username, event.getGuild());
                                verificationFuture.thenAccept(result -> privateChannel.sendMessage(result).queue());
                            });

                            event.getMessage().delete().queue();
                        } else {
                            member.getUser().openPrivateChannel().queue(pc ->
                                    pc.sendMessage("Your verification request was denied.").queue()
                            );
                            event.reply("Verification denied.").setEphemeral(true).queue();

                            event.getMessage().delete().queue();
                        }
                    },
                    failure -> {
                        // member not found or error
                        event.reply("User not found: " + userId + " " + username).setEphemeral(true).queue();
                    }
            );
            return;
        }



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
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getFocusedOption().getName().equals("command")) {
            String userInput = event.getFocusedOption().getValue();

            List<Command.Choice> choices = Arrays.stream(AllSlashCommands.values())
                    .map(Enum::name)
                    .filter(name -> name.toLowerCase().startsWith(userInput.toLowerCase()))
                    .limit(10)
                    .map(name -> new Command.Choice(name, name))
                    .collect(Collectors.toList());

            event.replyChoices(choices).queue();
        }

        if (event.getFocusedOption().getName().equals("config_role")) {
            String userInput = event.getFocusedOption().getValue();

            List<Command.Choice> choices = Arrays.stream(Config.Roles.values())
                    .map(Enum::name)
                    .filter(name -> name.toLowerCase().startsWith(userInput.toLowerCase()))
                    .limit(10)
                    .map(name -> new Command.Choice(name, name))
                    .collect(Collectors.toList());

            event.replyChoices(choices).queue();
        }

        if (event.getFocusedOption().getName().equals("setting_other")) {
            List<Command.Choice> choices = new ArrayList<>();

            for (Config.Settings setting : Config.Settings.values()) {
                    choices.add(new Command.Choice(setting.name(), setting.name()));
            }

            event.replyChoices(
                    choices.stream().limit(25).toList()
            ).queue();
        }

    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Member member = event.getMember();
        Guild guild = event.getGuild();
        String unverifiedRoleId = ConfigManager.getConfigInstance().roles.get(Config.Roles.UnVerifiedRole);

        Role role = Utils.getRoleFromGuild(guild, unverifiedRoleId);
        if (role != null) {
            guild.addRoleToMember(member, role).queue();
        }
    }


}