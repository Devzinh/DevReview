package br.com.devplugins.gui;

import br.com.devplugins.lang.LanguageManager;
import br.com.devplugins.ranking.RankingData;
import br.com.devplugins.ranking.RankingManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.Arrays;
import java.util.List;

public class RankingMenu implements InventoryHolder {

    private final RankingManager rankingManager;
    private final LanguageManager languageManager;
    private final Inventory inventory;
    private final Player viewer;

    public RankingMenu(RankingManager rankingManager, LanguageManager languageManager, Player viewer) {
        this.rankingManager = rankingManager;
        this.languageManager = languageManager;
        this.viewer = viewer;
        this.inventory = Bukkit.createInventory(this, 54, languageManager.getMessage(viewer, "gui.ranking-title"));
        loadItems();
    }

    private void loadItems() {
        List<RankingData> rankings = rankingManager.getRankings();
        int slot = 0;

        for (int i = 0; i < rankings.size(); i++) {
            if (slot >= 54)
                break;

            RankingData data = rankings.get(i);
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();

            if (meta != null) {
                meta.setOwningPlayer(Bukkit.getOfflinePlayer(data.getUuid()));

                String rankPrefix;
                if (i == 0) {
                    rankPrefix = languageManager.getMessage(viewer, "ranking.rank-first");
                } else if (i == rankings.size() - 1 && rankings.size() > 1) {
                    rankPrefix = languageManager.getMessage(viewer, "ranking.rank-last");
                } else {
                    rankPrefix = languageManager.getMessage(viewer, "ranking.rank-other").replace("%rank%",
                            String.valueOf(i + 1));
                }

                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', rankPrefix + data.getName()));

                String approvals = languageManager.getMessage(viewer, "gui.ranking.lore-approvals").replace("%count%",
                        String.valueOf(data.getApprovals()));
                String rejections = languageManager.getMessage(viewer, "gui.ranking.lore-rejections").replace("%count%",
                        String.valueOf(data.getRejections()));

                meta.setLore(Arrays.asList(
                        "",
                        approvals,
                        rejections));

                item.setItemMeta(meta);
            }
            inventory.setItem(slot++, item);
        }
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
