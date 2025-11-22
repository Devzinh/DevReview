package br.com.devplugins.listener;

import br.com.devplugins.staging.StagingManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.List;

public class CommandInterceptor implements Listener {

    private final StagingManager stagingManager;
    private final List<String> criticalCommands = Arrays.asList("/op", "/stop", "/reload", "/restart", "/ban", "/deop");

    public CommandInterceptor(StagingManager stagingManager) {
        this.stagingManager = stagingManager;
    }

    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        String command = message.split(" ")[0].toLowerCase();

        if (criticalCommands.contains(command)) {
            event.setCancelled(true);
            stagingManager.stageCommand(event.getPlayer(), message);
        }
    }
}
