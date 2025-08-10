package sidly.discord_bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;

import java.util.function.Consumer;

public enum AllSlashCommands {
    shutdown("shuts down the bot"),
    reloadconfig("reloads the bots config from the file"),
    editconfigoption("edit a config option"),
    getconfigoptions("shows config options"),
    checkforupdates("check if the bot has any updates"),
    getbotversion("gets the current bot version"),
    leaderboardguildxp("lists guild members and there contributed xp"),
    online("list online guild members"),
    verify("verify your minecraft account"),
    listcommands("list all bot commands"),
    setrolerequirement("add a role requirement to run a command"),
    removerolerequirement("remove a role requirement to run a command"),
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
    private Consumer<SlashCommandInteractionEvent> action = null;

    AllSlashCommands(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setAction(Consumer<SlashCommandInteractionEvent> action){
        this.action = action;
    }

    public SlashCommandData getBaseCommandData(){
        SlashCommandData data = Commands.slash(this.name(), this.getDescription())
                .setContexts(InteractionContextType.GUILD)
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL);
        return data;
    }

    public void run(SlashCommandInteractionEvent event) {
        Config.Settings requiredRole = getRequiredRole();
        if (requiredRole == null) {
            action.accept(event);
        } else if (Utils.hasRole(event.getMember(), ConfigManager.getSetting(requiredRole))) {
            action.accept(event);
        } else if (requiredRole == Config.Settings.RecruitRole ||
                requiredRole == Config.Settings.RecruiterRole ||
                requiredRole == Config.Settings.CaptainRole ||
                requiredRole == Config.Settings.StrategistRole ||
                requiredRole == Config.Settings.ChiefRole) {
            if (Utils.hasAtLeastRank(event.getMember(), ConfigManager.getSetting(requiredRole))) {
                action.accept(event);
            }
        } else event.reply("❌ You don't have permission to use this command.").setEphemeral(true).queue();

    }

    public Config.Settings getRequiredRole() {
         return ConfigManager.getConfigInstance().roleRequirements.get(this);
    }
}
