package com.mc.edgebrowser.listener;

import com.mc.edgebrowser.EdgeBrowser;
import com.mc.edgebrowser.browser.BrowserInstance;
import com.mc.edgebrowser.browser.BrowserManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InteractListener implements Listener {

    private final EdgeBrowser plugin;
    private final Map<UUID, Long> clickCooldowns = new HashMap<>();

    private final Map<UUID, int[]> pos1 = new HashMap<>();
    private final Map<UUID, int[]> pos2 = new HashMap<>();
    private final Map<UUID, String> posWorld = new HashMap<>();

    public InteractListener(EdgeBrowser plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK &&
            action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack hand = player.getInventory().getItemInMainHand();

        if (isSelector(hand)) {
            event.setCancelled(true);
            handleSelector(player, action);
            return;
        }

        if (hand.getType() == Material.AIR) {
            if (plugin.getConfigManager().requireUsePermission() &&
                !player.hasPermission("edge.use")) return;

            event.setCancelled(true);
            handleBrowserClick(player, action);
            return;
        }
    }

    private void handleSelector(Player player, Action action) {
        if (!player.hasPermission("edge.admin")) {
            player.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }

        Block target = player.getTargetBlockExact(20);
        if (target == null) {
            target = getTargetBlockFromLook(player, 20);
        }
        if (target == null) {
            player.sendMessage("§c无法确定目标方块！请看向一个方块。");
            return;
        }

        UUID uuid = player.getUniqueId();

        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            pos1.put(uuid, new int[]{target.getX(), target.getY(), target.getZ()});
            posWorld.put(uuid, target.getWorld().getName());
            String msg = plugin.getConfigManager().getMsg("pos1-set")
                .replace("%x%", String.valueOf(target.getX()))
                .replace("%y%", String.valueOf(target.getY()))
                .replace("%z%", String.valueOf(target.getZ()));
            player.sendMessage(msg);
        } else {
            pos2.put(uuid, new int[]{target.getX(), target.getY(), target.getZ()});
            posWorld.put(uuid, target.getWorld().getName());
            String msg = plugin.getConfigManager().getMsg("pos2-set")
                .replace("%x%", String.valueOf(target.getX()))
                .replace("%y%", String.valueOf(target.getY()))
                .replace("%z%", String.valueOf(target.getZ()));
            player.sendMessage(msg);
        }
    }

    private void handleBrowserClick(Player player, Action action) {
        if (plugin.getConfigManager().isSneakDisable() && player.isSneaking()) return;

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();
        Long lastClick = clickCooldowns.get(uuid);
        if (lastClick != null && now - lastClick < plugin.getConfigManager().getCooldown()) {
            return;
        }
        clickCooldowns.put(uuid, now);

        BrowserInstance browser = rayTraceBrowser(player, 20);
        if (browser == null) return;

        if (plugin.getConfigManager().requireClickPermission() &&
            !player.hasPermission("edge.command.click")) return;

        double[] hit = getHitPosition(player, browser);
        if (hit == null) return;

        double x = hit[0];
        double y = hit[1];

        String button;
        boolean isShift = player.isSneaking();
        if (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK) {
            button = isShift ? "drag-start" : "left";
        } else {
            button = isShift ? "drag-end" : "middle";
        }

        plugin.getBridgeServer().sendClick(browser.getId(), x, y, button);

        spawnClickEffect(player, browser, hit);

        if (plugin.getConfigManager().logClicks()) {
            plugin.getLogger().info(player.getName() + " 点击浏览器 " + browser.getId() +
                " @ (" + String.format("%.2f", x) + ", " + String.format("%.2f", y) + ") [" + button + "]");
        }
    }

    private BrowserInstance rayTraceBrowser(Player player, double maxDist) {
        Location eye = player.getEyeLocation();
        var direction = eye.getDirection();

        double step = 0.1;
        double dist = 0;

        while (dist < maxDist) {
            dist += step;
            Location point = eye.clone().add(direction.clone().multiply(dist));

            int bx = point.getBlockX();
            int by = point.getBlockY();
            int bz = point.getBlockZ();

            BrowserInstance browser = plugin.getBrowserManager().getBrowserAt(
                eye.getWorld(), bx, by, bz);

            if (browser != null) {
                return browser;
            }
        }

        return null;
    }

    private double[] getHitPosition(Player player, BrowserInstance browser) {
        Location eye = player.getEyeLocation();
        var dir = eye.getDirection();

        double planeX = browser.getCornerX();
        double planeY = browser.getCornerY();
        double planeZ = browser.getCornerZ() + 0.5;

        var face = browser.getDisplayBoard().getDirection();
        double nx = face.getModX();
        double ny = face.getModY();
        double nz = face.getModZ();

        double denom = dir.getX() * nx + dir.getY() * ny + dir.getZ() * nz;
        if (Math.abs(denom) < 0.0001) return null;

        double originX = eye.getX() - (planeX + 0.5 * (browser.getMapsX() - 1));
        double originY = eye.getY() - (planeY + 0.5 * (browser.getMapsY() - 1));
        double originZ = eye.getZ() - planeZ;

        double t = -(originX * nx + originY * ny + originZ * nz) / denom;
        if (t < 0 || t > 20) return null;

        double hitX = eye.getX() + dir.getX() * t;
        double hitY = eye.getY() + dir.getY() * t;
        double hitZ = eye.getZ() + dir.getZ() * t;

        double localX, localY;

        if (nx != 0) {
            localX = (hitZ - browser.getCornerZ()) / browser.getMapsX();
            localY = (hitY - browser.getCornerY()) / browser.getMapsY();
        } else if (nz != 0) {
            localX = (hitX - browser.getCornerX()) / browser.getMapsX();
            localY = (hitY - browser.getCornerY()) / browser.getMapsY();
        } else {
            return null;
        }

        if (localX < 0 || localX > 1 || localY < 0 || localY > 1) return null;

        return new double[]{localX, localY};
    }

    private void spawnClickEffect(Player player, BrowserInstance browser, double[] hit) {
        try {
            String particleName = plugin.getConfigManager().getClickParticle();
            Particle particle = Particle.valueOf(particleName);
            Location loc = browser.getCenterLocation().clone();
            loc.add((hit[0] - 0.5) * browser.getMapsX(), (hit[1] - 0.5) * browser.getMapsY(), 0);
            player.spawnParticle(particle, loc, 5, 0.1, 0.1, 0.1, 0.02);
        } catch (Exception ignored) {}

        try {
            String soundName = plugin.getConfigManager().getClickSound();
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 0.3f, 1.5f);
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onItemSwitch(PlayerItemHeldEvent event) {
        if (event.getPlayer().getInventory().getItemInMainHand().getType() != Material.AIR) return;
        if (plugin.getConfigManager().requireClickPermission() &&
            !event.getPlayer().hasPermission("edge.command.scroll")) return;

        Player player = event.getPlayer();
        BrowserInstance browser = rayTraceBrowser(player, 20);
        if (browser == null) return;

        int delta = event.getNewSlot() - event.getPreviousSlot();
        if (delta > 5) delta -= 9;
        if (delta < -5) delta += 9;

        if (delta != 0) {
            plugin.getBridgeServer().sendScroll(browser.getId(), 0, -delta * 120);
            event.setCancelled(true);
        }
    }

    private boolean isSelector(ItemStack item) {
        if (item == null || item.getType() != Material.valueOf(plugin.getConfigManager().getSelectorMaterial())) {
            return false;
        }
        if (!item.hasItemMeta()) return false;
        String name = item.getItemMeta().getDisplayName();
        return name.contains("Edge") || name.contains("区域选择");
    }

    private Block getTargetBlockFromLook(Player player, int maxDist) {
        Location eye = player.getEyeLocation();
        var dir = eye.getDirection();
        for (int i = 1; i <= maxDist * 10; i++) {
            Location point = eye.clone().add(dir.clone().multiply(i * 0.1));
            Block b = point.getBlock();
            if (!b.isEmpty() && !b.isPassable()) return b;
        }
        return null;
    }

    public int[] getPos1(UUID player) { return pos1.get(player); }
    public int[] getPos2(UUID player) { return pos2.get(player); }
    public String getPosWorld(UUID player) { return posWorld.get(player); }
    public void clearSelection(UUID player) {
        pos1.remove(player);
        pos2.remove(player);
        posWorld.remove(player);
    }
}