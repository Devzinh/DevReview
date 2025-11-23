package br.com.devplugins.staging;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SqlStagedCommandRepository implements StagedCommandRepository {

    private final JavaPlugin plugin;
    private String url;
    private String username;
    private String password;
    private String tablePrefix;

    public SqlStagedCommandRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        initTable();
    }

    private void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "database.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        String host = config.getString("host", "localhost");
        int port = config.getInt("port", 3306);
        String database = config.getString("database", "devreview");
        this.username = config.getString("username", "root");
        this.password = config.getString("password", "");
        this.tablePrefix = config.getString("table-prefix", "devreview_");

        // Simple URL construction for MySQL/MariaDB
        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private void initTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "staged_commands (" +
                "id VARCHAR(36) PRIMARY KEY, " +
                "sender_id VARCHAR(36), " +
                "sender_name VARCHAR(64), " +
                "command_line TEXT, " +
                "timestamp BIGINT" +
                ");";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database table", e);
        }
    }

    @Override
    public void save(StagedCommand command) {
        String sql = "REPLACE INTO " + tablePrefix
                + "staged_commands (id, sender_id, sender_name, command_line, timestamp) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, command.getId().toString());
            pstmt.setString(2, command.getSenderId().toString());
            pstmt.setString(3, command.getSenderName());
            pstmt.setString(4, command.getCommandLine());
            pstmt.setLong(5, command.getTimestamp());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save command to SQL", e);
        }
    }

    @Override
    public void delete(StagedCommand command) {
        String sql = "DELETE FROM " + tablePrefix + "staged_commands WHERE id = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, command.getId().toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete command from SQL", e);
        }
    }

    @Override
    public List<StagedCommand> loadAll() {
        List<StagedCommand> list = new ArrayList<>();
        String sql = "SELECT * FROM " + tablePrefix + "staged_commands";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID id = UUID.fromString(rs.getString("id"));
                UUID senderId = UUID.fromString(rs.getString("sender_id"));
                String senderName = rs.getString("sender_name");
                String commandLine = rs.getString("command_line");
                long timestamp = rs.getLong("timestamp");
                // Justification is not persisted
                String justification = null;

                StagedCommand cmd = new StagedCommand(id, senderId, senderName, commandLine, timestamp, justification);
                list.add(cmd);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load commands from SQL", e);
        }
        return list;
    }
}
