package sidly.discord_bot.api.sub;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.PlayerProfile;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RaidStats {
    public long damageTaken;
    public long damageDealt;
    public long healthHealed;
    public int deaths;
    public int buffsTaken;
    public int gambitsUsed;

    public static void getUser(SlashCommandInteractionEvent event) {
        if (event.getGuild() == null) {
            event.reply("use in server").queue();
            return;
        }

        User user = Optional.ofNullable(event.getOption("user"))
                .map(OptionMapping::getAsUser)
                .orElse(null);
        String username;
        if (user != null) username = event.getGuild().getMember(user).getEffectiveName();
        else {
            username = Optional.ofNullable(event.getOption("username"))
                    .map(OptionMapping::getAsString)
                    .orElse(null);
        }
        if (username == null || username.isEmpty()) {
            username = event.getMember().getEffectiveName();
        }

        PlayerProfile playerInfo = ApiUtils.getPlayerData(username);
        if (playerInfo == null) {
            event.reply("failed to get player info").queue();
            return;
        }

        GlobalData playerData = playerInfo.globalData;
        Map<String, Integer> currentGuildRaidsMap = new HashMap<>();
        Map<String, Integer> guildRaidsMap = new HashMap<>();
        Map<String, Integer> raidsMap = new HashMap<>();
        int gRaidTotal = 0;
        try {
            GlobalData guildData = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix)).members.getMemberInfo(playerInfo.uuid).globalData;
            currentGuildRaidsMap = guildData.currentGuildRaids.list;
            guildRaidsMap = guildData.guildRaids.list;
            raidsMap = guildData.raids.list;
            gRaidTotal = guildData.guildRaids.total;
        } catch (Exception ignored) {
        }

        if (guildRaidsMap.isEmpty()) guildRaidsMap = playerData.guildRaids.list;
        if (raidsMap.isEmpty()) raidsMap = playerData.raids.list;

        StringBuilder sb = new StringBuilder();

        sb.append("RaidCounts - Graids - In-")
                .append(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix))
                .append("\n");

        int total = playerData.raids.total;
        if (total == 0) return;

        sb.append("TOTAL: ").append(Utils.formatNumber(total))
                .append(" (").append(Utils.formatNumber(playerData.guildRaids.total)).append(") (")
                .append(Utils.formatNumber(gRaidTotal)).append(")\n");

        for (String key : raidsMap.keySet()) {
            int raidsValue = raidsMap.getOrDefault(key, 0);
            int guildRaidsValue = guildRaidsMap.getOrDefault(key, 0);
            int currentGuildRaidsValue = currentGuildRaidsMap.getOrDefault(key, 0);

            sb.append(key).append(": ")
                    .append(Utils.formatNumber(raidsValue)).append(" (")
                    .append(Utils.formatNumber(guildRaidsValue)).append(") (")
                    .append(Utils.formatNumber(currentGuildRaidsValue)).append(")\n");
        }

        sb.append("\n");

        RaidStats stats = playerData.raidStats;
        appendLine("Damage Taken", sb, (double) stats.damageTaken, total);
        appendLine("Damage Dealt", sb, (double) stats.damageDealt, total);
        appendLine("Health Healed", sb, (double) stats.healthHealed, total);
        appendLine("Deaths", sb, stats.deaths, total);
        appendLine("Buffs Taken", sb, stats.buffsTaken, total);
        appendLine("Gambits Used", sb, stats.gambitsUsed, total);

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Raid Stats for " + username)
                .setColor(Color.CYAN)
                .setDescription(sb.toString());

        event.replyEmbeds(embed.build()).setEphemeral(false).queue();
    }

    private static void appendLine(String name, StringBuilder sb, double stat, int total) {
        sb.append(name).append(": ").append(Utils.formatNumber(stat))
                .append(" - ")
                .append(Utils.formatNumber(stat / total))
                .append("per\n");
    }
}
