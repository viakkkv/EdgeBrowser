package com.mc.edgebrowser.map;

import com.mc.edgebrowser.EdgeBrowser;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;

import java.util.*;

public class DisplayBoard {

    private final EdgeBrowser plugin;
    private final Location corner;
    private final int mapsX, mapsY;
    private final Map<String, ItemStack> mapItems = new HashMap<>();
    private final Map<String, Integer> mapItemIds = new HashMap<>();
    private final Map<String, ItemFrame> itemFrames = new HashMap<>();
    private boolean placed = false;
    private BlockFace direction = BlockFace.SOUTH;

    public DisplayBoard(EdgeBrowser plugin, Location corner, int mapsX, int mapsY) {
        this.plugin = plugin;
        this.corner = corner.clone();
        this.mapsX = mapsX;
        this.mapsY = mapsY;
    }

    public void place() {
        if (placed) return;
        World world = corner.getWorld();
        if (world == null) return;

        determineDirection();

        String borderMat = plugin.getConfigManager().getBorderMaterial();
        String floorMat = plugin.getConfigManager().getFloorMaterial();

        for (int x = -1; x <= mapsX; x++) {
            for (int y = -1; y <= mapsY; y++) {
                if (x == -1 || x == mapsX || y == -1 || y == mapsY) {
                    Location loc = getBlockLocation(x, y);
                    try {
                        Block b = loc.getBlock();
                        b.setType(Material.valueOf(borderMat));
                    } catch (Exception ignored) {}
                }
            }
        }

        for (int x = 0; x < mapsX; x++) {
            for (int y = 0; y < mapsY; y++) {
                Location loc = getBlockLocation(x, y);
                try {
                    loc.getBlock().setType(Material.valueOf(floorMat));
                } catch (Exception ignored) {}

                int mapId = plugin.getMapManager().createMap(world);
                ItemStack mapItem = plugin.getMapManager().getMapItem(mapId);
                if (mapItem == null) continue;
                
                mapItems.put(x + "," + y, mapItem);
                mapItemIds.put(x + "," + y, mapId);

                Location frameLoc = loc.clone().add(0.5, 0.5, 0.5);
                ItemFrame frame = world.spawn(frameLoc, ItemFrame.class);
                frame.setFacingDirection(direction);
                frame.setItem(mapItem);
                frame.setFixed(true);
                frame.setVisible(false);
                itemFrames.put(x + "," + y, frame);
            }
        }

        placed = true;
    }

    private void determineDirection() {
        World world = corner.getWorld();
        if (world == null) return;

        BlockFace[] faces = {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
        for (BlockFace face : faces) {
            Location test = corner.clone().add(face.getModX(), 0, face.getModZ());
            if (test.getBlock().isPassable() || test.getBlock().isEmpty()) {
                direction = face;
                return;
            }
        }
        direction = BlockFace.SOUTH;
    }

    private Location getBlockLocation(int x, int y) {
        return corner.clone().add(x, y, 0);
    }

    public void showToPlayers(Collection<Player> players) {
        if (!placed) place();
        for (Player p : players) {
            showToPlayer(p);
        }
    }

    public void showToPlayer(Player player) {
        if (!placed) place();
        for (ItemStack map : mapItems.values()) {
            if (map != null && map.hasItemMeta() && map.getItemMeta() instanceof MapMeta) {
                MapMeta meta = (MapMeta) map.getItemMeta();
                player.sendMap(meta.getMapView());
            }
        }
    }

    public void updateMapDisplay(int x, int y, byte[] colors) {
        String key = x + "," + y;
        Integer mapId = mapItemIds.get(key);
        if (mapId == null) return;

        plugin.getMapManager().updateMapColors(mapId, colors);

        ItemFrame frame = itemFrames.get(key);
        if (frame != null && !frame.isDead()) {
            frame.setItem(mapItems.get(key));
        }
    }

    public void sendMapsToPlayer(Player player) {
        if (!placed) return;
        for (String key : mapItems.keySet()) {
            ItemStack mapItem = mapItems.get(key);
            if (mapItem != null && mapItem.getItemMeta() instanceof MapMeta) {
                MapMeta meta = (MapMeta) mapItem.getItemMeta();
                if (meta.hasMapView()) {
                    player.sendMap(meta.getMapView());
                }
            }
        }
    }

    public int getMapsX() { return mapsX; }
    public int getMapsY() { return mapsY; }
    public boolean isPlaced() { return placed; }
    public Location getCorner() { return corner.clone(); }

    public void cleanup() {
        for (ItemFrame frame : itemFrames.values()) {
            if (frame != null && !frame.isDead()) {
                frame.remove();
            }
        }
        itemFrames.clear();
        mapItems.clear();
        mapItemIds.clear();
        placed = false;
    }

    public BlockFace getDirection() { return direction; }
}