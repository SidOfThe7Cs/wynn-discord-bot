package sidly.discord_bot.v2.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import sidly.discord_bot.Config;
import sidly.discord_bot.ConfigManager;
import sidly.discord_bot.Utils;
import sidly.discord_bot.api.ApiUtils;
import sidly.discord_bot.api.PlayerProfile;
import sidly.discord_bot.api.sub.GlobalData;
import sidly.discord_bot.api.sub.RaidStats;
import sidly.discord_bot.v2.botapi.Command;
import sidly.discord_bot.v2.botapi.CommandResult;
import sidly.discord_bot.v2.options.UserOption;
import sidly.discord_bot.v2.options.UsernameOption;

import java.util.HashMap;
import java.util.Map;

public class RaidStatsCmd {
    private static final Command raidStatsCmd = new Command.Builder("get_raid_stats")
            .option(new UserOption(false))
            .option(new UsernameOption(false))
            .action(RaidStatsCmd::getUser)
            .build();

    // TODO make work with any guild and update to new api changes
    public static CommandResult getUser(SlashCommandInteractionEvent event) {
        String username = UsernameOption.fromUserOrName(event);
        PlayerProfile playerInfo = ApiUtils.getPlayerData(username);
        if (playerInfo == null) return new CommandResult.Builder("failed to get player data").ethereal(true).build();

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
        if (total == 0) return new CommandResult.Builder("user has completed 0 raids").ethereal(true).build();

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

        return new CommandResult.Builder(sb.toString()).title("Raid Stats for " + username).build();
    }

    private static void appendLine(String name, StringBuilder sb, double stat, int total) {
        sb.append(name).append(": ").append(Utils.formatNumber(stat))
                .append(" - ")
                .append(Utils.formatNumber(stat / total))
                .append("per\n");
    }
}
