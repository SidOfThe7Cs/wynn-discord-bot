import sidly.discord_bot.Utils;
import sidly.discord_bot.database.SQLDB;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static sidly.discord_bot.database.SQLDB.connection;

public class TestEntrypoint {
    private static List<String> list = new ArrayList<>();

    public static void main(String[] args) throws SQLException {
        //Map<String, GuildName> testApiResponce = ApiUtils.getAllGuildsList();

        //Gson gson = new GsonBuilder().setPrettyPrinting().create();
        //System.out.println("there are a total of: " + testApiResponce.entrySet().size() + " guilds");

        //new DynamicTimer(list, TestEntrypoint::testPriont, 7000, 400).start();

        //UuidMap.put("testUser", "123456789");
        //System.out.println(UuidMap.get("testUser"));

        System.out.println(Utils.getTotalGuildExperience(93, 1));
    }

    public static void testPriont() {
        list.add("e");
        list.add("e");
        list.add("e");
        list.add("e");
        list.add("e");
        System.out.println("function run");
    }

    public static String getFirstUuidForHour(int hour) {
        String sql = "SELECT uuid FROM guild_activity WHERE hour = ? ORDER BY id ASC LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, hour);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("uuid");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

}
/*
TODO

Add exception cmds

Server owner can always run commands

/delete old versions

guild stats show minutes for online

War ping role
Active hours and tracked guild should only be last 4 weeks


java.net.SocketException: Connection reset
    at java.base/sun.nio.ch.SocketChannelImpl.throwConnectionReset(SocketChannelImpl.java:401)
    at java.base/sun.nio.ch.SocketChannelImpl.read(SocketChannelImpl.java:434)
    at java.net.http/jdk.internal.net.http.SocketTube.readAvailable(SocketTube.java:1178)
    at java.net.http/jdk.internal.net.http.SocketTube$InternalReadPublisher$InternalReadSubscription.read(SocketTube.java:841)
    at java.net.http/jdk.internal.net.http.SocketTube$SocketFlowTask.run(SocketTube.java:181)
    at java.net.http/jdk.internal.net.http.common.SequentialScheduler$SchedulableTask.run(SequentialScheduler.java:207)
    at java.net.http/jdk.internal.net.http.common.SequentialScheduler.runOrSchedule(SequentialScheduler.java:280)
    at java.net.http/jdk.internal.net.http.common.SequentialScheduler.runOrSchedule(SequentialScheduler.java:233)
    at java.net.http/jdk.internal.net.http.SocketTube$InternalReadPublisher$InternalReadSubscription.signalReadable(SocketTube.java:782)
    at java.net.http/jdk.internal.net.http.SocketTube$InternalReadPublisher$ReadEvent.signalEvent(SocketTube.java:965)
    at java.net.http/jdk.internal.net.http.SocketTube$SocketFlowEvent.handle(SocketTube.java:253)
    at java.net.http/jdk.internal.net.http.HttpClientImpl$SelectorManager.handleEvent(HttpClientImpl.java:1470)
    at java.net.http/jdk.internal.net.http.HttpClientImpl$SelectorManager.lambda$run$3(HttpClientImpl.java:1415)
    at java.base/java.util.ArrayList.forEach(ArrayList.java:1596)
    at java.net.http/jdk.internal.net.http.HttpClientImpl$SelectorManager.run(HttpClientImpl.java:1415)
 */
