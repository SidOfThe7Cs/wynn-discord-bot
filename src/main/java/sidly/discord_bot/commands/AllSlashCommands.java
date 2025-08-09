package sidly.discord_bot.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
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
    adddemotionexeption("adds a player to be excluded from demotion checks, default length is forever"),
    addinactivityexeption("adds a custom inactivity threshold for a player, default length is forever"),
    addpromotionexeption("adds a player top be excluded from promotion checks, default length is forever"),
    checkfordemotions("check your guild members to see who should be a lower rank"),
    checkforinactivity("check your guild members to see who should be a lower rank"),
    checkforpromotions("check your guild members to see who should be a lower rank"),
    addPromotionRequirement("add a requiment to be promoted to a rank"),
    getPromotionRequirements("just view em"),
    checkPromotionProgress("view what a member needs to do to be promoted"),
    setPromotionOptionalRequirement("set the required number of optional requirements that need to be met"),
    removePromotionRequirement("remove a requirement from the promotion check");


    private final String description;
    private Consumer<SlashCommandInteractionEvent> action = null;
    private String requiredRole = "";

    AllSlashCommands(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setAction(Consumer<SlashCommandInteractionEvent> action){
        this.action = action;
    }

    public void setRequiredRole(String id){
        this.requiredRole = id;
    }

    public SlashCommandData getBaseCommandData(){
        SlashCommandData data = Commands.slash(this.name(), this.getDescription())
                .setContexts(InteractionContextType.GUILD)
                .setIntegrationTypes(IntegrationType.GUILD_INSTALL);
        return data;
    }

    public void run(SlashCommandInteractionEvent event){
        if (requiredRole.isEmpty()) {
            action.accept(event);
        }else if (Utils.hasAtLeastRank(event.getMember(), requiredRole)) {
            action.accept(event);
        } else event.reply("❌ You don't have permission to use this command.").setEphemeral(true).queue();
    }
}
