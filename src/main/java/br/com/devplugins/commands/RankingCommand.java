package br.com.devplugins.commands;

import br.com.devplugins.lang.LanguageManager;
import br.com.devplugins.ranking.RankingManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankingCommand implements CommandExecutor {

    private final RankingManager rankingManager;
    private final LanguageManager languageManager;

    public RankingCommand(RankingManager rankingManager, LanguageManager languageManager) {
        this.rankingManager = rankingManager;
        this.languageManager = languageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("devreview.rank")) {
            sender.sendMessage(languageManager.getMessage(sender, "messages.no-permission"));
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(languageManager.getMessage(sender, "messages.only-players"));
            return true;
        }

        new br.com.devplugins.gui.RankingMenu(rankingManager, languageManager, (Player) sender).open();
        return true;
    }
}
