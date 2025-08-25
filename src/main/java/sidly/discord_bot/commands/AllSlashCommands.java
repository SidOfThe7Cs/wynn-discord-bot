package sidly.discord_bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.RoleUtils;
import sidly.discord_bot.Utils;

import java.util.Map;
import java.util.function.Consumer;

public enum AllSlashCommands {
    shutdown("shuts down the bot"),
    reloadconfig("reloads the bots config from the file"),
    editconfigother("edit a config option"),
    editconfigchannel("edit channels config"),
    editconfigrole("edit role config"),
    editconfiglvlrole("set the roles for lvls"),
    editconfigclassrole("set the roles for classes and archetypes"),
    getconfigoptions("shows config options"),
    checkforupdates("check if the bot has any updates"),
    getbotversion("gets the current bot version"),
    leaderboardguildxp("lists guild members and there contributed xp"),
    online("list online guild members"),
    verify("verify your minecraft account"),
    listcommands("list all bot commands"),
    setrolerequirement("add a role requirement to run a command"),
    removerolerequirement("remove a role requirement to run a command"),
    removeverification("remove a verification from a user"),
    updateplayerroles("update the roles of a player"),
    getratelimitinfo("get the rate limit info"),
    addtrackedguild("e"),
    removetrackedguild("e"),
    gettimerstatus("get the status's of the timed actions"),
    starttimer("start a timed action"),
    stoptimer("stop a timed action"),
    activehours("view the active hours of a guild"),
    trackedguilds("view average online for tracked guilds"),
    getsysteminfo("get the info of the server running the bot"),
    averageplaytime("get the average playtime of a user"),
    updateplayerranks("update the ranks of all members in your guild"),
    guildstats("view the stats of a guild", 2),
    notindiscord("view players who are in your guild but not in your discord"),
    say("send a message"),
    sendwarrolesmessage("send a message to let users give themselves war roles"),
    sendselfassignedrolemessage("sen the message so users can give themselves roles to a channel"),
    addchannelrestriction("if any channels are whitelisted only whitelisted channels will allow commands"),
    adddemotionexeption("adds a player to be excluded from demotion checks, default length is forever"),
    addinactivityexeption("adds a custom inactivity threshold for a player, default length is forever"),
    addpromotionexeption("adds a player top be excluded from promotion checks, default length is forever"),
    checkfordemotions("check your guild members to see who should be a lower rank"),
    checkforinactivity("check your guild members to see who should be a lower rank"),
    checkforpromotions("check your guild members to see who should be a lower rank"),
    addpromotionrequirement("add a requiment to be promoted to a rank"),
    getpromotionrequirements("just view em"),
    checkpromotionprogress("view what a member needs to do to be promoted"),
    setpromotionoptionalrequirement("set the required number of optional requirements that need to be met"),
    removepromotionrequirement("remove a requirement from the promotion check");

    public Consumer<SlashCommandInteractionEvent> getAction() {
        return action;
    }

    private final String description;
    private final int cooldown;
    private long lastRan;
    private Consumer<SlashCommandInteractionEvent> action = null;

    AllSlashCommands(String description) {
        this.description = description;
        this.cooldown = 0;
    }

    AllSlashCommands(String description, int cooldownSeconds) {
        this.description = description;
        this.cooldown = cooldownSeconds;
    }

    public String getDescription() {
        return description;
    }

    public void setAction(Consumer<SlashCommandInteractionEvent> action){
        this.action = action;
    }

    public SlashCommandData getBaseCommandData(){
        return Commands.slash(this.name(), this.getDescription())
                .setContexts(InteractionContextType.GUILD)
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL);
    }

    public void run(SlashCommandInteractionEvent event) {

        if (cooldown > 0) {
            long cooldownRemaining = lastRan + (cooldown * 1000L) - System.currentTimeMillis();
            if (cooldownRemaining > 0) {
                event.reply("this command is on cooldown for " + (cooldownRemaining / 1000) + " seconds").setEphemeral(true).queue();
                return;
            }
        }

        lastRan = System.currentTimeMillis();

        Map<String, Boolean> allowedChannels = ConfigManager.getConfigInstance().allowedChannels;
        boolean hasTrue = false;
        for (Boolean value : allowedChannels.values()) {
            if (Boolean.TRUE.equals(value)) {
                hasTrue = true;
                break;
            }
        }
        if (!allowedChannels.isEmpty()){
            String id = event.getChannel().getId();
            if (hasTrue){ // you need to be in a whitelisted channel to run command
                if (!allowedChannels.containsKey(id) || !allowedChannels.get(id).equals(true)){
                    event.reply("❌ you may not run commands here").setEphemeral(true).queue();
                    return;
                }
            }else { // you can run commands in any not blacklisted channel
                if (allowedChannels.containsKey(id) && allowedChannels.get(id).equals(false)){
                    event.reply("❌ you may not run commands here").setEphemeral(true).queue();
                    return;
                }
            }
        }
        
        Config.Roles requiredRole = getRequiredRole();
        if (requiredRole == null) {
            action.accept(event);
        } else if (RoleUtils.hasRole(event.getMember(), ConfigManager.getConfigInstance().roles.get(requiredRole))) {
            action.accept(event);
        } else if (requiredRole == Config.Roles.RecruitRole ||
                requiredRole == Config.Roles.RecruiterRole ||
                requiredRole == Config.Roles.CaptainRole ||
                requiredRole == Config.Roles.StrategistRole ||
                requiredRole == Config.Roles.ChiefRole ||
                requiredRole == Config.Roles.OwnerRole) {
            if (Utils.hasAtLeastRank(event.getMember(), ConfigManager.getConfigInstance().roles.get(requiredRole))) {
                action.accept(event);
            } else event.reply("❌ You don't have a high enough to use this command.").setEphemeral(true).queue();
        } else event.reply("❌ You don't have permission to use this command.").setEphemeral(true).queue();
    }

    public Config.Roles getRequiredRole() {
         return ConfigManager.getConfigInstance().roleRequirements.get(this);
    }
}
