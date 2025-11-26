package br.com.devplugins.staging;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class SqlCommandHistoryRepository implements CommandHistoryRepository {

    private final JavaPlugin plugin;
    private String url;
    private String username;
    private String password;
    private String tablePrefix;

    public SqlCommandHistoryRepository(JavaPlugin plugin) {
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

        this.url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true";
    }

    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private void initTable() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "command_history (" +
                "id VARCHAR(36), " +
                "player_id VARCHAR(36), " +
                "sender_id VARCHAR(36), " +
                "sender_name VARCHAR(64), " +
                "command_line TEXT, " +
                "timestamp BIGINT, " +
                "justification TEXT, " +
                "reviewer_id VARCHAR(36), " +
                "reviewer_name VARCHAR(64), " +
                "status VARCHAR(20), " +
                "PRIMARY KEY (id, player_id), " +
                "INDEX idx_player_id (player_id), " +
                "INDEX idx_player_timestamp (player_id, timestamp), " +
                "INDEX idx_reviewer_id (reviewer_id)" +
                ");";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            // Add additional indexes if they don't exist (for existing tables)
            addIndexesIfNeeded(conn);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize command history table", e);
        }
    }
    
    private void addIndexesIfNeeded(Connection conn) {
        // Try to add composite index for player_id + timestamp (for ORDER BY queries)
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE INDEX idx_player_timestamp ON " + tablePrefix + "command_history (player_id, timestamp)");
        } catch (SQLException e) {
            // Index likely already exists, ignore
        }
        
        // Try to add index for reviewer_id (for reviewer statistics)
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE INDEX idx_reviewer_id ON " + tablePrefix + "command_history (reviewer_id)");
        } catch (SQLException e) {
            // Index likely already exists, ignore
        }
    }

    @Override
    public void save(UUID playerId, List<StagedCommand> history) {
        // Delete existing history for this player
        String deleteSql = "DELETE FROM " + tablePrefix + "command_history WHERE player_id = ?";
        
        // Insert all commands in the history
        String insertSql = "INSERT INTO " + tablePrefix + "command_history " +
                "(id, player_id, sender_id, sender_name, command_line, timestamp, justification, reviewer_id, reviewer_name, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConnection()) {
            // Delete old history
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
                pstmt.setString(1, playerId.toString());
                pstmt.executeUpdate();
            }

            // Insert new history
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                for (StagedCommand command : history) {
                    pstmt.setString(1, command.getId().toString());
                    pstmt.setString(2, playerId.toString());
                    pstmt.setString(3, command.getSenderId().toString());
                    pstmt.setString(4, command.getSenderName());
                    pstmt.setString(5, command.getCommandLine());
                    pstmt.setLong(6, command.getTimestamp());
                    pstmt.setString(7, command.getJustification());
                    pstmt.setString(8, command.getReviewerId() != null ? command.getReviewerId().toString() : null);
                    pstmt.setString(9, command.getReviewerName());
                    pstmt.setString(10, command.getStatus() != null ? command.getStatus().name() : "PENDING");
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save command history to SQL", e);
        }
    }

    @Override
    public List<StagedCommand> load(UUID playerId) {
        List<StagedCommand> history = new ArrayList<>();
        String sql = "SELECT * FROM " + tablePrefix + "command_history WHERE player_id = ? ORDER BY timestamp DESC";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    StagedCommand cmd = buildCommandFromResultSet(rs);
                    history.add(cmd);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load command history from SQL", e);
        }
        return history;
    }

    @Override
    public Map<UUID, List<StagedCommand>> loadAll() {
        Map<UUID, List<StagedCommand>> allHistory = new HashMap<>();
        String sql = "SELECT * FROM " + tablePrefix + "command_history ORDER BY player_id, timestamp DESC";

        try (Connection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID playerId = UUID.fromString(rs.getString("player_id"));
                StagedCommand cmd = buildCommandFromResultSet(rs);
                
                allHistory.computeIfAbsent(playerId, k -> new ArrayList<>()).add(cmd);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load all command history from SQL", e);
        }
        return allHistory;
    }

    @Override
    public void delete(UUID playerId) {
        String sql = "DELETE FROM " + tablePrefix + "command_history WHERE player_id = ?";

        try (Connection conn = getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerId.toString());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete command history from SQL", e);
        }
    }

    private StagedCommand buildCommandFromResultSet(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID senderId = UUID.fromString(rs.getString("sender_id"));
        String senderName = rs.getString("sender_name");
        String commandLine = rs.getString("command_line");
        long timestamp = rs.getLong("timestamp");
        String justification = rs.getString("justification");

        StagedCommand cmd = new StagedCommand(id, senderId, senderName, commandLine, timestamp, justification);
        
        String reviewerIdStr = rs.getString("reviewer_id");
        if (reviewerIdStr != null) {
            cmd.setReviewerId(UUID.fromString(reviewerIdStr));
        }
        cmd.setReviewerName(rs.getString("reviewer_name"));
        
        String statusStr = rs.getString("status");
        if (statusStr != null) {
            cmd.setStatus(StagedCommand.Status.valueOf(statusStr));
        }
        
        return cmd;
    }
}
