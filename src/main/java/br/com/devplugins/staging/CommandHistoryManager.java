package br.com.devplugins.staging;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CommandHistoryManager {
    private final Map<UUID, List<StagedCommand>> commandHistory = new ConcurrentHashMap<>();
    private final int maxHistoryPerPlayer = 50;

    public CommandHistoryManager(JavaPlugin plugin) {
    }

    public void addToHistory(StagedCommand command) {
        UUID senderId = command.getSenderId();
        commandHistory.computeIfAbsent(senderId, k -> new ArrayList<>()).add(command);
        
        List<StagedCommand> history = commandHistory.get(senderId);
        if (history.size() > maxHistoryPerPlayer) {
            history.remove(0);
        }
    }

    public List<StagedCommand> getHistoryForPlayer(UUID playerId) {
        return commandHistory.getOrDefault(playerId, new ArrayList<>())
                .stream()
                .sorted(Comparator.comparingLong(StagedCommand::getTimestamp).reversed())
                .collect(Collectors.toList());
    }

    public List<StagedCommand> getRecentHistoryForPlayer(UUID playerId, int limit) {
        return getHistoryForPlayer(playerId).stream()
                .limit(limit)
                .collect(Collectors.toList());
    }
}

