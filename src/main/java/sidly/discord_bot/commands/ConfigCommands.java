package sidly.discord_bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;

import java.awt.*;

public class ConfigCommands {

    public static void showConfigOptions(SlashCommandInteractionEvent event) {
        Config config = ConfigManager.getConfigInstance();
        StringBuilder sb = new StringBuilder();

        // Roles
        sb.append("**Roles:**\n");
        config.roles.forEach((key, value) -> {
            String mention = mentionRole(event.getGuild(), value);
            sb.append(key).append(" : ").append(mention).append("\n");
        });
        sb.append("\n");

        // Level Roles
        sb.append("**Level Roles:**\n");
        config.lvlRoles.forEach((key, value) -> {
            String mention = mentionRole(event.getGuild(), value);
            sb.append(key).append(" : ").append(mention).append("\n");
        });
        sb.append("\n");

        // Role Requirements
        sb.append("**Role Requirements:**\n");
        config.roleRequirements.forEach((cmd, role) -> {
            String roleId = config.roles.get(role);
            String mention = mentionRole(event.getGuild(), roleId);
            sb.append(cmd).append(" : ").append(mention).append("\n");
        });
        sb.append("\n");

        // Allowed Channels
        sb.append("**Allowed Channels:**\n");
        config.allowedChannels.forEach((channelId, allowed) -> {
            String mention = mentionChannel(event.getGuild(), channelId);
            sb.append(mention != null ? mention : channelId)
                    .append(" : ").append(allowed).append("\n");
        });
        sb.append("\n");

        // Channels
        sb.append("**Channels:**\n");
        config.channels.forEach((key, value) -> {
            String mention = mentionChannel(event.getGuild(), value);
            sb.append(key).append(" : ").append(mention).append("\n");
        });
        sb.append("\n");

        // Other settings
        sb.append("**Other Settings:**\n");
        config.other.forEach((key, value) -> {
            if (!key.equals(Config.Settings.Token) && !key.equals(Config.Settings.ApiToken)) {
                sb.append(key).append(" : ").append(value).append("\n");
            }
        });


        // Send embed
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Current Config:")
                .setDescription(sb.toString())
                .setColor(Color.CYAN)
                .setFooter("Requested by " + event.getUser().getName(), event.getUser().getEffectiveAvatarUrl())
                .setTimestamp(java.time.Instant.now());

        event.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    // Helper for roles
    private static String mentionRole(Guild guild, String roleId) {
        if (roleId == null || roleId.isEmpty()) return "None";
        Role role = Utils.getRoleFromGuild(guild, roleId);
        return role != null ? role.getAsMention() : roleId;
    }

    // Helper for channels
    private static String mentionChannel(Guild guild, String channelId) {
        if (channelId == null || channelId.isEmpty()) return null;
        GuildChannel channel = Utils.getChannelFromGuild(guild, channelId);
        return channel != null ? channel.getAsMention() : channelId;
    }


    public static void editConfigLvlRoleOption(SlashCommandInteractionEvent event) {
        String setting = event.getOption("role_name").getAsString();
        Role mention = event.getOption("role").getAsRole();
        String id = mention.getId();
        Config.LvlRoles option = Config.LvlRoles.valueOf(setting);

        ConfigManager.getConfigInstance().lvlRoles.put(option, id);
        ConfigManager.saveConfig();

        event.reply(event.getUser().getName() + " has changed the " + setting + " to " + id).setEphemeral(false).queue();
    }

    public static void editConfigSettingOther(SlashCommandInteractionEvent event) {
        String setting = event.getOption("setting_name").getAsString();
        String value  = event.getOption("setting_other").getAsString();

        boolean ephemeral = false;
        Config.Settings option;
        try {
            option = Config.Settings.valueOf(setting);
        } catch (IllegalArgumentException e) {
            event.reply("Invalid setting: " + setting).setEphemeral(true).queue();
            return;
        }

        if (option == Config.Settings.Token) {
            event.reply("If this is working the token is already correct and you don't want to change it").setEphemeral(true).queue();
            return;
        }

        if (option == Config.Settings.ApiToken) {
            ephemeral = true;
        }

        ConfigManager.getConfigInstance().other.put(option, value);
        ConfigManager.saveConfig();

        event.reply(event.getUser().getName() + " has changed the " + setting + " to " + value).setEphemeral(ephemeral).queue();
    }

    public static void editConfigRole(SlashCommandInteractionEvent event) {
        String setting = event.getOption("config_role").getAsString();
        Role mention  = event.getOption("role").getAsRole();
        String id = mention.getId();

        Config.Roles option;
        try {
            option = Config.Roles.valueOf(setting);
        } catch (IllegalArgumentException e) {
            event.reply("Invalid setting: " + setting).setEphemeral(true).queue();
            return;
        }

        ConfigManager.getConfigInstance().roles.put(option, id);
        ConfigManager.saveConfig();

        event.reply(event.getUser().getName() + " has changed the " + setting + " to " + id).setEphemeral(false).queue();
    }

    public static void editConfigChannel(SlashCommandInteractionEvent event) {
        String setting = event.getOption("channel_name").getAsString();
        Channel mention  = event.getOption("channel").getAsChannel();
        String id = mention.getId();

        Config.Channels option;
        try {
            option = Config.Channels.valueOf(setting);
        } catch (IllegalArgumentException e) {
            event.reply("Invalid setting: " + setting).setEphemeral(true).queue();
            return;
        }

        ConfigManager.getConfigInstance().channels.put(option, id);
        ConfigManager.saveConfig();

        event.reply(event.getUser().getName() + " has changed the " + setting + " to " + id).setEphemeral(false).queue();
    }
}
