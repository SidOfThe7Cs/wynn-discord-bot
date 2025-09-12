package sidly.discord_bot.database.tables;

import sidly.discord_bot.database.records.GuildName;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static sidly.discord_bot.database.SQLDB.connection;

public class AllGuilds {
    // Adds or replaces a guild row
    public static void addGuild(String prefix, String uuid, String name) {
        String sql = "INSERT OR REPLACE INTO all_guilds (prefix, uuid, name) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, prefix);
            stmt.setString(2, uuid);
            stmt.setString(3, name);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addGuilds(Map<String, GuildName> guilds) {
        String sql = "INSERT OR REPLACE INTO all_guilds (prefix, uuid, name) VALUES (?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            connection.setAutoCommit(false); // begin transaction

            for (GuildName g : guilds.values()) {
                ps.setString(1, g.prefix());
                ps.setString(2, g.uuid());
                ps.setString(3, g.name());
                ps.addBatch();
            }

            ps.executeBatch(); // run all inserts at once
            connection.commit(); // commit transaction

        } catch (SQLException e) {
            e.printStackTrace();
            try {
                connection.rollback(); // rollback if something failed
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            try {
                connection.setAutoCommit(true); // restore default
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static int getTotalGuilds() {
        String sql = "SELECT COUNT(*) AS total FROM all_guilds";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0; // fallback if table is empty or query fails
    }

    public static String getPrefixByUuid(String uuid) {
        String sql = "SELECT prefix FROM all_guilds WHERE uuid = ? AND prefix IS NOT NULL LIMIT 1";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("prefix");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // No valid prefix found
    }

    public static Map<String, String> getAllPrefixes() {
        Map<String, String> prefixes = new HashMap<>();
        String sql = "SELECT uuid, prefix FROM all_guilds WHERE prefix IS NOT NULL";

        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                String uuid = rs.getString("uuid");
                String prefix = rs.getString("prefix");
                prefixes.put(uuid, prefix);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return prefixes;
    }

    // Gets a guild by prefix
    public static GuildName getGuild(String prefix) {
        String sql = "SELECT prefix, uuid, name FROM all_guilds WHERE prefix = ? AND prefix IS NOT NULL AND uuid IS NOT NULL AND name IS NOT NULL";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, prefix);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new GuildName(
                        rs.getString("prefix"),
                        rs.getString("uuid"),
                        rs.getString("name")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // not found
    }

    public static Set<GuildName> getAllByPrefixes(Set<String> prefixes) {
        Set<GuildName> guilds = new HashSet<>();
        if (prefixes.isEmpty()) return guilds; // nothing to do

        // build placeholders for the IN clause
        String placeholders = prefixes.stream().map(p -> "?").collect(Collectors.joining(","));
        String sql = "SELECT prefix, uuid, name FROM all_guilds WHERE prefix IN (" + placeholders + ") " +
                "AND prefix IS NOT NULL AND uuid IS NOT NULL AND name IS NOT NULL";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int idx = 1;
            for (String prefix : prefixes) {
                stmt.setString(idx++, prefix);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                guilds.add(new GuildName(
                        rs.getString("prefix"),
                        rs.getString("uuid"),
                        rs.getString("name")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return guilds;
    }


    // Checks if a guild exists by prefix
    public static boolean containsGuild(String prefix) {
        String sql = "SELECT 1 FROM all_guilds WHERE prefix = ? LIMIT 1";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, prefix);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static List<String> getTracked(boolean lowPrio) {
        List<String> tracked = new ArrayList<>();
        String sql = "SELECT prefix FROM guilds_40_plus WHERE low_priority = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, lowPrio ? 1 : 0);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                tracked.add(rs.getString("prefix"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tracked;
    }

    public static void addTracked(String prefix, boolean lowPrio) {
        String sql = "INSERT OR REPLACE INTO guilds_40_plus (prefix, low_priority) VALUES (?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, prefix);
            stmt.setInt(2, lowPrio ? 1 : 0);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public static void unTracked(String prefix) {
        String sql = "DELETE FROM guilds_40_plus WHERE prefix = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, prefix);
            stmt.executeUpdate();
            System.out.println("deleted guild: " + prefix);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
