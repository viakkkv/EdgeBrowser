package com.mc.edgebrowser.browser;

import com.mc.edgebrowser.EdgeBrowser;
import com.mc.edgebrowser.bridge.BridgeServer;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BrowserManager {

    private final EdgeBrowser plugin;
    private final Map<String, BrowserInstance> browsers = new ConcurrentHashMap<>();
    private final Map<UUID, String> selections = new ConcurrentHashMap<>();

    public BrowserManager(EdgeBrowser plugin) {
        this.plugin = plugin;
    }

    public BrowserInstance createBrowser(String id, UUID owner, String worldName,
                                          int x1, int y1, int z1, int x2, int y2, int z2) {
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        int maxZ = Math.max(z1, z2);

        int mapsX = maxX - minX + 1;
        int mapsY = maxY - minY + 1;

        if (mapsX < 1 || mapsX > 10 || mapsY < 1 || mapsY > 10) return null;

        var world = plugin.getServer().getWorld(worldName);
        if (world == null) return null;

        String url = plugin.getConfigManager().getDefaultUrl();
        int fps = plugin.getConfigManager().getTargetFps();
        int brightness = plugin.getConfigManager().getBrightness();

        BrowserInstance browser = new BrowserInstance(id, owner, world,
            minX, minY, minZ, mapsX, mapsY, url, fps, brightness);

        browsers.put(id, browser);

        // 通知桥接服务器
        BridgeServer bridge = plugin.getBridgeServer();
        if (bridge != null && bridge.isEnabled()) {
            bridge.notifyBrowserCreated(browser);
        }

        // 启动渲染
        plugin.getRenderEngine().startRendering(browser);

        return browser;
    }

    public boolean removeBrowser(String id) {
        BrowserInstance browser = browsers.remove(id);
        if (browser == null) return false;

        browser.setActive(false);
        plugin.getRenderEngine().stopRendering(id);
        browser.getDisplayBoard().cleanup();

        BridgeServer bridge = plugin.getBridgeServer();
        if (bridge != null) {
            bridge.notifyBrowserRemoved(id);
        }

        return true;
    }

    public BrowserInstance getBrowser(String id) {
        return browsers.get(id);
    }

    public BrowserInstance getBrowserAt(org.bukkit.World world, int bx, int by, int bz) {
        for (BrowserInstance b : browsers.values()) {
            if (b.getWorld().equals(world) && b.containsBlock(bx, by, bz) && b.isActive()) {
                return b;
            }
        }
        return null;
    }

    public Collection<BrowserInstance> getAllBrowsers() {
        return Collections.unmodifiableCollection(browsers.values());
    }

    public void setSelection(UUID player, String posKey, int x, int y, int z, String world) {
        String key = player.toString();
        String data = posKey + ":" + x + ":" + y + ":" + z + ":" + world;
        selections.put(player, data);
    }

    public int[] getSelection(UUID player) {
        return null;
    }

    public Selection getFullSelection(UUID player) {
        return null;
    }

    public void clearSelection(UUID player) {
        selections.remove(player);
    }

    public void shutdownAll() {
        for (String id : browsers.keySet()) {
            removeBrowser(id);
        }
    }

    public static class Selection {
        public int x1, y1, z1, x2, y2, z2;
        public String world;

        public Selection(int x1, int y1, int z1, int x2, int y2, int z2, String world) {
            this.x1 = x1; this.y1 = y1; this.z1 = z1;
            this.x2 = x2; this.y2 = y2; this.z2 = z2;
            this.world = world;
        }

        public boolean isValid() {
            int w = Math.abs(x2 - x1) + 1;
            int h = Math.abs(y2 - y1) + 1;
            return w >= 1 && w <= 10 && h >= 1 && h <= 10 && z1 == z2;
        }
    }
}