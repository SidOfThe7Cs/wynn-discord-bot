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
        if (username == null) {
            event.reply("please specify a user or username").setEphemeral(true).queue();
            return;
        }

        if (username.isEmpty()) return;
        PlayerProfile playerInfo = ApiUtils.getPlayerData(username);

        Map<String, Integer> guildDataRaidsMap = new HashMap<>();
        int gRaidTotal = 0;
        try {
            GlobalData guildData = ApiUtils.getGuildInfo(ConfigManager.getConfigInstance().other.get(Config.Settings.YourGuildPrefix)).members.getMemberInfo(playerInfo.uuid).globalData;
            guildDataRaidsMap = guildData.guildRaids.list;
            gRaidTotal = guildData.guildRaids.total;
        } catch (Exception ignored) {
        }
        GlobalData playerData = playerInfo.globalData;

        StringBuilder sb = new StringBuilder();

        sb.append("RaidCounts - Graids - In-HOC\n");
        Map<String, Integer> raidsMap = playerData.raids.list;
        Map<String, Integer> guildRaidsMap = playerData.guildRaids.list;

        int total = playerData.raids.total;
        if (total == 0) return;

        sb.append("TOTAL: ").append(Utils.formatNumber(total))
                .append(" (").append(Utils.formatNumber(playerData.guildRaids.total)).append(") (")
                .append(Utils.formatNumber(gRaidTotal)).append(")\n");

        for (String key : raidsMap.keySet()) {
            int raidsValue = raidsMap.getOrDefault(key, 0);
            int guildRaidsValue = guildRaidsMap.getOrDefault(key, 0);
            int guildDataRaidsValue = guildDataRaidsMap.getOrDefault(key, 0);

            sb.append(key).append(": ")
                    .append(Utils.formatNumber(raidsValue)).append(" (")
                    .append(Utils.formatNumber(guildRaidsValue)).append(") (")
                    .append(Utils.formatNumber(guildDataRaidsValue)).append(")\n");
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
