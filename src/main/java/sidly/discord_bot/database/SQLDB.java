package sidly.discord_bot.database;

import sidly.discord_bot.ConfigManager;

import java.io.File;
import java.sql.*;
import java.util.Map;

public class SQLDB {
    public static File dbFile = new File(ConfigManager.JAR_DIR, "database.db");

    public static Connection connection;

    public static void executeQuery(String sql) {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static ResultSet executeAndGetQuery(String sql) {
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void init() throws SQLException {
        SQLDB.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

        createTable("uuidMap", Map.of(
                "username", "TEXT PRIMARY KEY",
                "discord_id", "TEXT"
        ));
        addColumn("uuidMap", "minecraft_id", "TEXT");

        createTable("players", Map.of(
                "uuid", "TEXT PRIMARY KEY",
                "username", "TEXT",
                "level", "INTEGER",
                "guildWars", "INTEGER",
                "latestPlaytime", "REAL",
                "lastModified", "INTEGER",
                "lastJoined", "TEXT",
                "firstJoined", "TEXT",
                "supportRank", "TEXT",
                "highestLvl", "INTEGER"
        ));
        addColumn("players", "wars", "INTEGER");

        createTable("playtime_history", Map.of(
                "id", "INTEGER PRIMARY KEY AUTOINCREMENT",
                "uuid", "TEXT NOT NULL",
                "playtime", "REAL",
                "timeLogged", "INTEGER"
        ));
        executeQuery("CREATE INDEX IF NOT EXISTS idx_playtime_uuid ON playtime_history(uuid)");

        createTable("guild_activity", Map.of(
                "id", "INTEGER PRIMARY KEY AUTOINCREMENT",
                "uuid", "TEXT",
                "prefix", "TEXT",
                "name", "TEXT",
                "hour", "INTEGER",
                "online_count", "REAL",
                "captains_online", "REAL",
                "timestamp", "INTEGER"
        ));
        executeQuery("CREATE INDEX IF NOT EXISTS idx_activity_uuid ON guild_activity(uuid)");
        executeQuery("CREATE INDEX IF NOT EXISTS idx_activity_uuid_time ON guild_activity(uuid, hour)");
        executeQuery("CREATE INDEX IF NOT EXISTS idx_activity_hour_timestamp ON guild_activity(hour, timestamp)");

        createTable("all_guilds", Map.of(
                "prefix", "TEXT",
                "uuid", "TEXT PRIMARY KEY",
                "name", "TEXT"
        ));
        executeQuery("DROP TABLE if EXISTS tracked_guilds");

        createTable("guilds_40_plus", Map.of(
                "prefix", "TEXT PRIMARY KEY"
        ));
        addColumn("guilds_40_plus", "low_priority", "INTEGER");
        executeQuery("CREATE INDEX IF NOT EXISTS idx_tracked_priority ON guilds_40_plus(low_priority)");

        createTable("inactivity_exceptions", Map.of(
                "uuid", "TEXT PRIMARY KEY",
                "exp_timestamp", "INTEGER"
        ));
        createTable("promotion_exceptions", Map.of(
                "uuid", "TEXT PRIMARY KEY",
                "exp_timestamp", "INTEGER"
        ));
    }

    public static void createTable(String tableName, Map<String, String> columns) {
        // Build SQL string
        StringBuilder sb = new StringBuilder("CREATE TABLE IF NOT EXISTS ");
        sb.append(tableName).append(" (");

        boolean first = true;
        for (Map.Entry<String, String> entry : columns.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(entry.getKey()).append(" ").append(entry.getValue());
            first = false;
        }
        sb.append(");");
        String sql = sb.toString();

        executeQuery(sql);
    }

    public static boolean columnExists(String table, String column) {
        String sql = "PRAGMA table_info(" + table + ")";
        try (Statement stmt = SQLDB.connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void addColumn(String table, String column, String type) {
        if (columnExists(table, column)) {
            return;
        }
        String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + type;
        executeQuery(sql);
    }

}

