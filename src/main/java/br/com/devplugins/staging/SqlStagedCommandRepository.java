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
                "timestamp BIGINT, " +
                "justification TEXT, " +
                "INDEX idx_sender_id (sender_id), " +
                "INDEX idx_timestamp (timestamp)" +
                ");";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            // Add justification column if it doesn't exist (for existing tables)
            addJustificationColumnIfNeeded(conn);
            // Add indexes if they don't exist (for existing tables)
            addIndexesIfNeeded(conn);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database table", e);
        }
    }

    private void addJustificationColumnIfNeeded(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // Try to add the column - will fail silently if it already exists
            String alterSql = "ALTER TABLE " + tablePrefix + "staged_commands ADD COLUMN justification TEXT";
            stmt.execute(alterSql);
        } catch (SQLException e) {
            // Column likely already exists, ignore
        }
    }
    
    private void addIndexesIfNeeded(Connection conn) {
        // Try to add indexes - will fail silently if they already exist
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE INDEX idx_sender_id ON " + tablePrefix + "staged_commands (sender_id)");
        } catch (SQLException e) {
            // Index likely already exists, ignore
        }
        
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE INDEX idx_timestamp ON " + tablePrefix + "staged_commands (timestamp)");
        } catch (SQLException e) {
            // Index likely already exists, ignore
        }
    }

    @Override
    public void save(StagedCommand command) {
        String sql = "REPLACE INTO " + tablePrefix
                + "staged_commands (id, sender_id, sender_name, command_line, timestamp, justification) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, command.getId().toString());
            pstmt.setString(2, command.getSenderId().toString());
            pstmt.setString(3, command.getSenderName());
            pstmt.setString(4, command.getCommandLine());
            pstmt.setLong(5, command.getTimestamp());
            pstmt.setString(6, command.getJustification());
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
                String justification = rs.getString("justification");

                StagedCommand cmd = new StagedCommand(id, senderId, senderName, commandLine, timestamp, justification);
                list.add(cmd);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load commands from SQL", e);
        }
        return list;
    }
    
    @Override
    public void saveAll(List<StagedCommand> commands) {
        if (commands.isEmpty()) {
            return;
        }
        
        String sql = "REPLACE INTO " + tablePrefix
                + "staged_commands (id, sender_id, sender_name, command_line, timestamp, justification) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false); // Start transaction
            
            for (StagedCommand command : commands) {
                pstmt.setString(1, command.getId().toString());
                pstmt.setString(2, command.getSenderId().toString());
                pstmt.setString(3, command.getSenderName());
                pstmt.setString(4, command.getCommandLine());
                pstmt.setLong(5, command.getTimestamp());
                pstmt.setString(6, command.getJustification());
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            conn.commit(); // Commit transaction
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to batch save commands to SQL", e);
        }
    }
    
    @Override
    public void deleteAll(List<StagedCommand> commands) {
        if (commands.isEmpty()) {
            return;
        }
        
        String sql = "DELETE FROM " + tablePrefix + "staged_commands WHERE id = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            conn.setAutoCommit(false); // Start transaction
            
            for (StagedCommand command : commands) {
                pstmt.setString(1, command.getId().toString());
                pstmt.addBatch();
            }
            
            pstmt.executeBatch();
            conn.commit(); // Commit transaction
            
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to batch delete commands from SQL", e);
        }
    }
}
