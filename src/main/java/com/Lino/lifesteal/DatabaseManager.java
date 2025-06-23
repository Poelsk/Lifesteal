package com.Lino.lifesteal;

import java.io.File;
import java.sql.*;
import java.util.UUID;

public class DatabaseManager {

    private Connection connection;
    private final File databaseFile;

    public DatabaseManager(File databaseFile) {
        this.databaseFile = databaseFile;
    }

    public void initialize() {
        try {
            if (!databaseFile.exists()) {
                databaseFile.getParentFile().mkdirs();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());

            try (Statement stmt = connection.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS players (" +
                        "uuid TEXT PRIMARY KEY," +
                        "name TEXT NOT NULL," +
                        "hearts INTEGER NOT NULL DEFAULT 20," +
                        "banned BOOLEAN NOT NULL DEFAULT 0" +
                        ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void createPlayer(UUID uuid, String name, int hearts) {
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT OR REPLACE INTO players (uuid, name, hearts, banned) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setInt(3, hearts);
            ps.setBoolean(4, false);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getHearts(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT hearts FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("hearts");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public void setHearts(UUID uuid, String name, int hearts) {
        createPlayer(uuid, name, hearts);
    }

    public void setBanned(UUID uuid, boolean banned) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE players SET banned = ? WHERE uuid = ?")) {
            ps.setBoolean(1, banned);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isBanned(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT banned FROM players WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("banned");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}