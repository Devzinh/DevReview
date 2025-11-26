package br.com.devplugins.placeholders;

import br.com.devplugins.lang.LanguageManager;
import br.com.devplugins.ranking.RankingData;
import br.com.devplugins.ranking.RankingManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class RankingPlaceholderExpansion extends PlaceholderExpansion {

    private final JavaPlugin plugin;
    private final RankingManager rankingManager;
    private final LanguageManager languageManager;

    public RankingPlaceholderExpansion(JavaPlugin plugin, RankingManager rankingManager,
            LanguageManager languageManager) {
        this.plugin = plugin;
        this.rankingManager = rankingManager;
        this.languageManager = languageManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "devreview";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (params.equalsIgnoreCase("rank")) {
            List<RankingData> rankings = rankingManager.getRankings();
            for (int i = 0; i < rankings.size(); i++) {
                if (rankings.get(i).getUuid().equals(player.getUniqueId())) {
                    return String.valueOf(i + 1);
                }
            }
            return "N/A";
        }

        if (params.equalsIgnoreCase("tag")) {
            List<RankingData> rankings = rankingManager.getRankings();
            for (int i = 0; i < rankings.size(); i++) {
                if (rankings.get(i).getUuid().equals(player.getUniqueId())) {
                    CommandSender sender = player.isOnline() ? (Player) player : Bukkit.getConsoleSender();
                    if (i == 0)
                        return ChatColor.translateAlternateColorCodes('&',
                                languageManager.getMessage(sender, "ranking.rank-first"));
                    if (i == rankings.size() - 1 && rankings.size() > 1)
                        return ChatColor.translateAlternateColorCodes('&',
                                languageManager.getMessage(sender, "ranking.rank-last"));
                    return "";
                }
            }
            return "";
        }

        if (params.startsWith("top_name_")) {
            try {
                int index = Integer.parseInt(params.replace("top_name_", "")) - 1;
                List<RankingData> rankings = rankingManager.getRankings();
                if (index >= 0 && index < rankings.size()) {
                    return rankings.get(index).getName();
                }
                return "N/A";
            } catch (NumberFormatException e) {
                return "Invalid Index";
            }
        }

        if (params.startsWith("top_approvals_")) {
            try {
                int index = Integer.parseInt(params.replace("top_approvals_", "")) - 1;
                List<RankingData> rankings = rankingManager.getRankings();
                if (index >= 0 && index < rankings.size()) {
                    return String.valueOf(rankings.get(index).getApprovals());
                }
                return "0";
            } catch (NumberFormatException e) {
                return "0";
            }
        }

        return null;
    }
}
