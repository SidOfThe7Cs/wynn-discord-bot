package sidly.discord_bot.database;

import sidly.discord_bot.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuildDataActivity {
    // minutes since day started mapped to online count
    private final Map<Integer, List<TimestampedDouble>> playersOnline;
    private final Map<Integer, List<TimestampedDouble>> captainsOnline;

    public String getGuildName() {
        return guildName;
    }

    private String guildName;

    public GuildDataActivity(String guildName) {
        this.playersOnline = new HashMap<>();
        this.captainsOnline = new HashMap<>();
        this.guildName = guildName;
    }

    public double getAverageOnline(int time, int daysInPast, boolean captainPlus){
        if (time < 0 || time > 23) return 0;

        double total = 0;
        double count = 0;

        long startTime = 0;
        if (daysInPast > 0){
            startTime = System.currentTimeMillis() - ((long) daysInPast * 24 * 60 * 60 * 1000);
        }
        List<TimestampedDouble> list = captainPlus ? captainsOnline.get(time) : playersOnline.get(time);
        if (list == null || list.isEmpty()) return -1;
        for (TimestampedDouble onlineCount : list){
            if (onlineCount.time >= startTime){
                total += onlineCount.value;
                count++;
            }
        }
        if (count == 0) return -1;

        return total / count;
    }

    public double getAverageOnline(int daysInPast, boolean captainPlus){
        double total = 0;
        double count = 0;

        long startTime = 0;
        if (daysInPast > 0){
            startTime = System.currentTimeMillis() - ((long) daysInPast * 24 * 60 * 60 * 1000);
        }
        Map<Integer, List<TimestampedDouble>> map = captainPlus ? captainsOnline : playersOnline;
        if (map.isEmpty()) return -1;
        for (Map.Entry<Integer, List<TimestampedDouble>> entry : map.entrySet()) {
            for (TimestampedDouble timestampedDouble : entry.getValue()){
                if (timestampedDouble.time >= startTime){
                    total += timestampedDouble.value;
                    count++;
                }
            }
        }
        if (count == 0) return -1;

        return total / count;
    }

    public void add(double onlineCount, boolean captainPlus) {
        int hoursSinceDayStarted = Utils.getHoursSinceDayStarted(System.currentTimeMillis());

        Map<Integer, List<TimestampedDouble>> targetMap = captainPlus ? captainsOnline : playersOnline;
        targetMap.computeIfAbsent(hoursSinceDayStarted, k -> new ArrayList<>())
                .add(new TimestampedDouble(onlineCount, System.currentTimeMillis()));

    }

    public record TimestampedDouble(double value, long time) {}
}
