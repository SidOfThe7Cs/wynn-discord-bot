package sidly.discord_bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import sidly.discord_bot.api.MassGuild;
import sidly.discord_bot.commands.*;
import sidly.discord_bot.commands.demotion_promotion.InactivityCommands;
import sidly.discord_bot.commands.demotion_promotion.PromotionCommands;
import sidly.discord_bot.commands.demotion_promotion.RequirementType;
import sidly.discord_bot.database.SQLDB;
import sidly.discord_bot.database.records.GuildAverages;
import sidly.discord_bot.page.PageBuilder;
import sidly.discord_bot.page.PaginationIds;
import sidly.discord_bot.timed_actions.GuildRankUpdater;
import sidly.discord_bot.timed_actions.UpdatePlayers;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static net.dv8tion.jda.api.interactions.commands.OptionType.*;

public class MainEntrypoint extends ListenerAdapter {

    public static JDA jda;

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
    public static void main(String[] args) throws InterruptedException, SQLException {

        Runtime.getRuntime().addShutdownHook(new Thread(MainEntrypoint::shutdown));

        ConfigManager.load();
        SQLDB.init();
        String token = ConfigManager.getConfigInstance().other.get(Config.Settings.Token);
        if (token == null || token.isEmpty()){
            System.err.println("please assign your bot token in the config file located at " + ConfigManager.CONFIG_FILE.getAbsolutePath());
            shutdown();
        }

        JDA jda = JDABuilder.createLight(token, EnumSet.noneOf(GatewayIntent.class))
                .addEventListeners(new MainEntrypoint(), new RoleChangeListener())
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

        commands.addCommands(AllSlashCommands.editconfigclassrole.getBaseCommandData()
                .addOptions(
                        new OptionData(OptionType.STRING, "role_name", "Choose a role", true)
                                .addChoices(
                                        Arrays.stream(Config.ClassRoles.values())
                                                .map(e -> new Command.Choice(e.name(), e.name()))
                                                .toArray(Command.Choice[]::new)
                                ),
                        new OptionData(ROLE, "role", "The id for the role", true)
                )
        );
        AllSlashCommands.editconfigclassrole.setAction(ConfigCommands::editConfigClassRoleOption);

        commands.addCommands(AllSlashCommands.getconfigoptions.getBaseCommandData());
        AllSlashCommands.getconfigoptions.setAction(ConfigCommands::showConfigOptions);
        PageBuilder.PaginationManager.register(PaginationIds.CONFIG_LIST.name(), "All Config Options", 1);

        commands.addCommands(AllSlashCommands.checkforupdates.getBaseCommandData());
        AllSlashCommands.checkforupdates.setAction(UpdaterCommands::checkForUpdate);

        commands.addCommands(AllSlashCommands.getbotversion.getBaseCommandData());
        AllSlashCommands.getbotversion.setAction(UpdaterCommands::getBotVersion);

        commands.addCommands(AllSlashCommands.notindiscord.getBaseCommandData());
        AllSlashCommands.notindiscord.setAction(GuildCommands::notInDiscord);

        commands.addCommands(AllSlashCommands.leaderboardguildxp.getBaseCommandData()
                .addOption(STRING, "guild_prefix", "e", true));
        AllSlashCommands.leaderboardguildxp.setAction(GuildCommands::showGuildXpLeaderboard);

        commands.addCommands(AllSlashCommands.guildstats.getBaseCommandData()
                .addOption(STRING, "guild_prefix", "e", true));
        AllSlashCommands.guildstats.setAction(GuildCommands::showStats);
        PageBuilder.PaginationManager.register(PaginationIds.GUILD_STATS.name(),
                stats -> GuildCommands.guildStatsConverter((GuildCommands.GuildStatEntry) stats), "?", 6);


        commands.addCommands(AllSlashCommands.online.getBaseCommandData()
                .addOption(STRING, "guild_prefix", "e", true));
        AllSlashCommands.online.setAction(GuildCommands::showOnlineMembers);

        commands.addCommands(AllSlashCommands.lastlogins.getBaseCommandData()
                .addOption(STRING, "guild", "e", true));
        AllSlashCommands.lastlogins.setAction(GuildCommands::viewLastLogins);
        PageBuilder.PaginationManager.register(PaginationIds.LAST_LOGINS.name(),
                stats -> GuildCommands.lastLoginsConverter((GuildCommands.LastLoginInfo) stats), "Last Logins", 25);

        commands.addCommands(AllSlashCommands.verify.getBaseCommandData()
                .addOption(STRING, "username", "e", true));
        AllSlashCommands.verify.setAction(VerificationCommands::verify);

        commands.addCommands(AllSlashCommands.listcommands.getBaseCommandData());
        AllSlashCommands.listcommands.setAction(HelpCommands::listCommands);
        PageBuilder.PaginationManager.register(PaginationIds.COMMAND_LIST.name(),
                cmd -> HelpCommands.commandListConverter((AllSlashCommands) cmd), "List of All Bot Commands", 12);

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

        commands.addCommands(AllSlashCommands.averageplaytime.getBaseCommandData());
        AllSlashCommands.averageplaytime.setAction(InactivityCommands::getAveragePlaytime);
        PageBuilder.PaginationManager.register(PaginationIds.AVERAGE_PLAYTIME.name(), "Player, 10weeklinearavg, 1weekavg, 5weekavg, 20weekavg, alltimeavg", 20);

        commands.addCommands(AllSlashCommands.getratelimitinfo.getBaseCommandData());
        AllSlashCommands.getratelimitinfo.setAction(RateLimitCommands::getRateLimitInfo);

        commands.addCommands(AllSlashCommands.sendselfassignedrolemessage.getBaseCommandData()
                .addOption(CHANNEL, "channel", "channel", true));
        AllSlashCommands.sendselfassignedrolemessage.setAction(SelfAssignedRoles::sendMessage);

        commands.addCommands(AllSlashCommands.sendwarrolesmessage.getBaseCommandData()
                .addOption(CHANNEL, "channel", "channel", true));
        AllSlashCommands.sendwarrolesmessage.setAction(SelfAssignedRoles::sendWarMessage);

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

        commands.addCommands(AllSlashCommands.addtrackedguild.getBaseCommandData()
                .addOption(STRING, "guild_prefix", "e", true));
        AllSlashCommands.addtrackedguild.setAction(GuildCommands::addTrackedGuild);

        commands.addCommands(AllSlashCommands.removetrackedguild.getBaseCommandData()
                .addOption(STRING, "guild_prefix", "e", true));
        AllSlashCommands.removetrackedguild.setAction(GuildCommands::removeTrackedGuild);

        commands.addCommands(AllSlashCommands.activehours.getBaseCommandData()
                .addOption(STRING, "guild_prefix", "e", true)
                .addOption(BOOLEAN, "use_code_block", "doesn't show timestamps in favor of being readable on mobile", false)
                .addOption(INTEGER, "days", "average over the last number of days", false));
        AllSlashCommands.activehours.setAction(GuildCommands::viewActiveHours);

        commands.addCommands(AllSlashCommands.trackedguilds.getBaseCommandData()
                .addOption(INTEGER, "days", "average over the last number of days", false));
        AllSlashCommands.trackedguilds.setAction(GuildCommands::viewTrackedGuilds);
        PageBuilder.PaginationManager.register(PaginationIds.GUILD.name(),
                trackedGuild -> GuildCommands.guildConverter((GuildAverages) trackedGuild), "Average activity for tracked guilds",9);

        commands.addCommands(AllSlashCommands.getsysteminfo.getBaseCommandData());
        AllSlashCommands.getsysteminfo.setAction(HelpCommands::getSystemInfo);

        commands.addCommands(AllSlashCommands.updateplayerranks.getBaseCommandData());
        AllSlashCommands.updateplayerranks.setAction(GuildCommands::updatePlayerRanks);

        commands.addCommands(AllSlashCommands.gettimerstatus.getBaseCommandData());
        AllSlashCommands.gettimerstatus.setAction(TimerCommands::getTimerStatus);

        commands.addCommands(AllSlashCommands.starttimer.getBaseCommandData().addOptions(
                        new OptionData(OptionType.STRING, "timer_name", "the name of the timer to start", true)
                                .addChoices(
                                        new Command.Choice("playerUpdater", "playerUpdater"),
                                        new Command.Choice("guildTracker", "guildTracker"),
                                        new Command.Choice("yourGuildRankUpdater", "yourGuildRankUpdater")
                                )
                )
        );
        AllSlashCommands.starttimer.setAction(TimerCommands::startTimer);

        commands.addCommands(AllSlashCommands.stoptimer.getBaseCommandData().addOptions(
                new OptionData(OptionType.STRING, "timer_name", "the name of the timer to start", true)
                        .addChoices(
                                new Command.Choice("playerUpdater", "playerUpdater"),
                                new Command.Choice("guildTracker", "guildTracker"),
                                new Command.Choice("yourGuildRankUpdater", "yourGuildRankUpdater")
                        )
        ));
        AllSlashCommands.stoptimer.setAction(TimerCommands::stopTimer);

        commands.addCommands(AllSlashCommands.say.getBaseCommandData()
                .addOption(CHANNEL, "channel", "channel", true)
                .addOption(STRING, "message", "what to say", true));
        AllSlashCommands.say.setAction(HelpCommands::sendMessage);

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
        AllSlashCommands.checkforinactivity.setAction(InactivityCommands::checkForInactivity);

        commands.addCommands(AllSlashCommands.checkforpromotions.getBaseCommandData());
        AllSlashCommands.checkforpromotions.setAction(PromotionCommands::checkForPromotions);
        PageBuilder.PaginationManager.register(PaginationIds.PROMOTIONS.name(), entry -> PromotionCommands.promotionConverter((PromotionCommands.PromotionEntry) entry), "Promotions", 8);


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
                .addOption(USER, "user", "e", true));
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
        MainEntrypoint.jda = jda;

        UpdatePlayers.init();
        GuildRankUpdater.init();
        MassGuild.init();
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

        if (fullId.equals("configure_roles")) {
            SelfAssignedRoles.sendEphemeralMessage(event);
            return;
        }

        if (fullId.equals("apply-role-selection")) {
            SelfAssignedRoles.applySelections(event);
            return;
        }

        if (fullId.startsWith("pagination")){
            PageBuilder.handlePagination(event);
            return;
        } else if (fullId.startsWith("verification")) {
            VerificationCommands.verify(event);
        } else if (fullId.startsWith("verC:") || fullId.startsWith("verD:")) {
            String[] parts = fullId.split(":");
            String discordId = parts[1];
            int index = Integer.parseInt(parts[2]);
            VerificationCommands.uuidAndUsername ids = VerificationCommands.tempVerificationUuidMap.get(index);

            event.getGuild().retrieveMemberById(discordId).queue(
                    member -> {
                        if (member == null) {
                            event.reply("User not found." + discordId + ids.username()).setEphemeral(true).queue();
                            return;
                        }

                        if (fullId.startsWith("verC:")) {
                            event.reply("Verification approved!").setEphemeral(true).queue();

                            // after the private channel opens run the verificationComplete and then when that completes send the user the results
                            member.getUser().openPrivateChannel().queue(privateChannel -> {
                                CompletableFuture<String> verificationFuture = VerificationCommands.completeVerification(member, event.getGuild(), index);
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
                        event.reply("User not found: " + discordId + " " + ids.username()).setEphemeral(true).queue();
                    }
            );
            return;
        }



        String[] id = event.getComponentId().split(":");
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
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        SelfAssignedRoles.onStringSelectInteraction(event);
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

        Role role = RoleUtils.getRoleFromGuild(guild, unverifiedRoleId);
        if (role != null) {
            guild.addRoleToMember(member, role).queue();
        }

        String message = ConfigManager.getConfigInstance().other.get(Config.Settings.GuildJoinMessage);

        String channelId = ConfigManager.getConfigInstance().channels.get(Config.Channels.WelcomeChannel);
        if (channelId != null && !channelId.isEmpty()) {
            TextChannel channel = guild.getTextChannelById(channelId);

            if (channel != null) {
                message = message.replace("%user%", member.getAsMention());
                channel.sendMessage(message).queue();
            }
        }
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        User user = event.getUser();
        Guild guild = event.getGuild();
        String message = ConfigManager.getConfigInstance().other.get(Config.Settings.GuildLeaveMessage);

        String channelId = ConfigManager.getConfigInstance().channels.get(Config.Channels.WelcomeChannel);
        if (channelId != null && !channelId.isEmpty()) {
            TextChannel channel = guild.getTextChannelById(channelId);

            if (channel != null) {
                String username = "**" + user.getEffectiveName() + "**";
                message = message.replace("%user%", username);
                channel.sendMessage(message).queue();
            }
        }
    }
}