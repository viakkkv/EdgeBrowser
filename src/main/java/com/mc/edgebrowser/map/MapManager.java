package com.mc.edgebrowser.map;

import com.mc.edgebrowser.EdgeBrowser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class MapManager {

    private final EdgeBrowser plugin;
    private final AtomicInteger mapIdCounter = new AtomicInteger(0);
    private final Map<Integer, MapView> mapViews = new ConcurrentHashMap<>();
    private final Map<Integer, byte[]> lastColors = new ConcurrentHashMap<>();
    private final Map<Integer, FastMapRenderer> renderers = new ConcurrentHashMap<>();

    public MapManager(EdgeBrowser plugin) {
        this.plugin = plugin;
    }

    public int createMap(World world) {
        int id = mapIdCounter.incrementAndGet();
        if (world == null) {
            world = plugin.getServer().getWorlds().get(0);
        }
        MapView view = Bukkit.createMap(world);
        view.getRenderers().clear();
        view.setScale(MapView.Scale.FARTHEST);
        view.setTrackingPosition(false);
        view.setUnlimitedTracking(false);
        
        FastMapRenderer renderer = new FastMapRenderer();
        view.addRenderer(renderer);
        renderers.put(id, renderer);
        
        mapViews.put(id, view);
        return id;
    }

    public ItemStack getMapItem(int mapId) {
        MapView view = mapViews.get(mapId);
        if (view == null) return null;
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) item.getItemMeta();
        meta.setMapView(view);
        item.setItemMeta(meta);
        return item;
    }

    public void updateMapColors(int mapId, byte[] colors) {
        MapView view = mapViews.get(mapId);
        if (view == null) return;

        lastColors.put(mapId, colors.clone());
        
        FastMapRenderer renderer = renderers.get(mapId);
        if (renderer != null) {
            renderer.updateColors(colors);
        }
    }

    public void refreshMap(int mapId) {
        MapView view = mapViews.get(mapId);
        if (view == null) return;
        
        byte[] colors = lastColors.get(mapId);
        if (colors != null) {
            FastMapRenderer renderer = renderers.get(mapId);
            if (renderer != null) {
                renderer.setDirty(true);
            }
        }
    }

    public void sendMapToPlayer(Player player, int mapId) {
        MapView view = mapViews.get(mapId);
        if (view == null) return;
        player.sendMap(view);
    }

    public byte[] getLastColors(int mapId) {
        return lastColors.get(mapId);
    }

    public void cleanup() {
        mapViews.clear();
        lastColors.clear();
        renderers.clear();
    }

    private static class FastMapRenderer extends MapRenderer {
        private volatile byte[] colors;
        private volatile boolean dirty = true;

        public FastMapRenderer() {
            super(false);
        }

        @Override
        public void render(MapView view, MapCanvas canvas, Player player) {
            if (dirty && colors != null) {
                for (int i = 0; i < 128 * 128 && i < colors.length; i++) {
                    canvas.setPixel(i % 128, i / 128, colors[i]);
                }
                dirty = false;
            }
        }

        public void updateColors(byte[] newColors) {
            this.colors = newColors.clone();
            this.dirty = true;
        }

        public void setDirty(boolean dirty) {
            this.dirty = dirty;
        }
    }
}