package com.mc.edgebrowser.listener;

import com.mc.edgebrowser.EdgeBrowser;
import com.mc.edgebrowser.browser.BrowserInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class PlayerListener implements Listener {

    private final EdgeBrowser plugin;

    public PlayerListener(EdgeBrowser plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getBrowserManager().clearSelection(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!plugin.getConfigManager().isAutoShow()) return;
        if (event.getFrom().toVector().equals(event.getTo().toVector())) return;

        Player player = event.getPlayer();
        int range = plugin.getConfigManager().getShowRange();

        for (BrowserInstance browser : plugin.getBrowserManager().getAllBrowsers()) {
            if (!browser.isActive() || !browser.getDisplayBoard().isPlaced()) continue;

            double dist = player.getLocation().distanceSquared(browser.getCenterLocation());
            boolean inRange = dist <= range * range;

            if (inRange) {
                browser.getDisplayBoard().sendMapsToPlayer(player);
            }
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
    }
}