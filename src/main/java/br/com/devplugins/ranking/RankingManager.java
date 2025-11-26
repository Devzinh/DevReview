package br.com.devplugins.ranking;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

public class RankingManager {

    private final JavaPlugin plugin;
    private final Map<UUID, RankingData> rankings = new HashMap<>();
    private File rankingFile;
    private YamlConfiguration rankingConfig;

    public RankingManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadRankings();
    }

    private void loadRankings() {
        rankingFile = new File(plugin.getDataFolder(), "ranking.yml");
        if (!rankingFile.exists()) {
            try {
                rankingFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create ranking.yml!");
                e.printStackTrace();
            }
        }
        rankingConfig = YamlConfiguration.loadConfiguration(rankingFile);

        checkReset();

        ConfigurationSection section = rankingConfig.getConfigurationSection("data");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                String name = section.getString(key + ".name");
                int approvals = section.getInt(key + ".approvals");
                int rejections = section.getInt(key + ".rejections");
                rankings.put(uuid, new RankingData(uuid, name, approvals, rejections));
            }
        }
    }

    public void saveRankings() {
        rankingConfig.set("last-reset", rankingConfig.getString("last-reset", LocalDateTime.now().toString()));

        ConfigurationSection section = rankingConfig.createSection("data");
        for (RankingData data : rankings.values()) {
            String key = data.getUuid().toString();
            section.set(key + ".name", data.getName());
            section.set(key + ".approvals", data.getApprovals());
            section.set(key + ".rejections", data.getRejections());
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                rankingConfig.save(rankingFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save ranking.yml!");
                e.printStackTrace();
            }
        });
    }

    private void checkReset() {
        if (!plugin.getConfig().getBoolean("ranking.enabled", true)) {
            return;
        }

        String lastResetStr = rankingConfig.getString("last-reset");
        if (lastResetStr == null) {
            rankingConfig.set("last-reset", LocalDateTime.now().toString());
            return;
        }

        LocalDateTime lastReset = LocalDateTime.parse(lastResetStr);
        LocalDateTime now = LocalDateTime.now();
        String resetPeriod = plugin.getConfig().getString("ranking.reset-period", "WEEKLY").toUpperCase();

        boolean shouldReset = false;
        if (resetPeriod.equals("WEEKLY")) {
            // Reset if we are in a new week (e.g., Monday as start of week)
            LocalDateTime nextWeek = lastReset.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).withHour(0).withMinute(0)
                    .withSecond(0);
            if (now.isAfter(nextWeek)) {
                shouldReset = true;
            }
        } else if (resetPeriod.equals("MONTHLY")) {
            // Reset if we are in a new month
            if (now.getMonth() != lastReset.getMonth() || now.getYear() != lastReset.getYear()) {
                shouldReset = true;
            }
        }

        if (shouldReset) {
            rankings.clear();
            rankingConfig.set("data", null);
            rankingConfig.set("last-reset", now.toString());
            saveRankings();
            plugin.getLogger().info("Ranking has been reset (" + resetPeriod + ").");
        }
    }

    public void addApproval(UUID uuid, String name) {
        RankingData data = rankings.computeIfAbsent(uuid, k -> new RankingData(k, name));
        data.setName(name); // Update name just in case
        data.addApproval();
        saveRankings();
    }

    public void addRejection(UUID uuid, String name) {
        RankingData data = rankings.computeIfAbsent(uuid, k -> new RankingData(k, name));
        data.setName(name); // Update name just in case
        data.addRejection();
        saveRankings();
    }

    public List<RankingData> getRankings() {
        return rankings.values().stream()
                .sorted(Comparator.comparingInt(RankingData::getApprovals).reversed())
                .collect(Collectors.toList());
    }
}
