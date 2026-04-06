package com.mc.edgebrowser.browser;

import com.mc.edgebrowser.EdgeBrowser;
import com.mc.edgebrowser.map.DisplayBoard;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class BrowserInstance {

    private final String id;
    private final UUID owner;
    private final World world;
    private final int cornerX, cornerY, cornerZ;
    private final int mapsX, mapsY;
    private final DisplayBoard displayBoard;
    private final AtomicReference<String> currentUrl = new AtomicReference<>("about:blank");
    private final AtomicBoolean paused = new AtomicBoolean(false);
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final AtomicLong lastFrameTime = new AtomicLong(0);
    private final AtomicLong frameCount = new AtomicLong(0);
    private final AtomicLong fpsCounter = new AtomicLong(0);
    private final AtomicLong lastFpsCheck = new AtomicLong(System.currentTimeMillis());
    private int targetFps;
    private int brightness;
    private String title;
    private long createdTime;

    public BrowserInstance(String id, UUID owner, World world, int cornerX, int cornerY, int cornerZ,
                           int mapsX, int mapsY, String url, int targetFps, int brightness) {
        this.id = id;
        this.owner = owner;
        this.world = world;
        this.cornerX = cornerX;
        this.cornerY = cornerY;
        this.cornerZ = cornerZ;
        this.mapsX = mapsX;
        this.mapsY = mapsY;
        this.targetFps = targetFps;
        this.brightness = brightness;
        this.title = "EdgeBrowser - " + id;
        this.currentUrl.set(url);
        this.createdTime = System.currentTimeMillis();

        Location loc = new Location(world, cornerX, cornerY, cornerZ);
        this.displayBoard = new DisplayBoard(EdgeBrowser.getInstance(), loc, mapsX, mapsY);
    }

    public boolean containsBlock(int bx, int by, int bz) {
        return bx >= cornerX && bx < cornerX + mapsX &&
               by >= cornerY && by < cornerY + mapsY &&
               bz >= cornerZ && bz < cornerZ + mapsY;
    }

    public double getClickX(int bx) {
        return (bx - cornerX + 0.5) / mapsX;
    }

    public double getClickY(int by) {
        return (by - cornerY + 0.5) / mapsY;
    }

    // Getters
    public String getId() { return id; }
    public UUID getOwner() { return owner; }
    public World getWorld() { return world; }
    public int getCornerX() { return cornerX; }
    public int getCornerY() { return cornerY; }
    public int getCornerZ() { return cornerZ; }
    public int getMapsX() { return mapsX; }
    public int getMapsY() { return mapsY; }
    public DisplayBoard getDisplayBoard() { return displayBoard; }
    public String getCurrentUrl() { return currentUrl.get(); }
    public void setCurrentUrl(String url) { currentUrl.set(url); }
    public boolean isPaused() { return paused.get(); }
    public void setPaused(boolean p) { paused.set(p); }
    public boolean isActive() { return active.get(); }
    public void setActive(boolean a) { active.set(a); }
    public int getTargetFps() { return targetFps; }
    public void setTargetFps(int fps) { this.targetFps = fps; }
    public int getBrightness() { return brightness; }
    public void setBrightness(int b) { this.brightness = b; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public long getLastFrameTime() { return lastFrameTime.get(); }
    public void setLastFrameTime(long t) { lastFrameTime.set(t); }
    public long getFrameCount() { return frameCount.get(); }
    public void incrementFrameCount() { frameCount.incrementAndGet(); fpsCounter.incrementAndGet(); }
    public int getRealFps() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastFpsCheck.get();
        if (elapsed >= 1000) {
            int fps = (int) (fpsCounter.get() * 1000 / elapsed);
            fpsCounter.set(0);
            lastFpsCheck.set(now);
            return fps;
        }
        return (int) (fpsCounter.get() * 1000 / Math.max(1, elapsed));
    }
    public long getCreatedTime() { return createdTime; }

    public Location getCenterLocation() {
        return new Location(world,
            cornerX + mapsX / 2.0,
            cornerY + mapsY / 2.0,
            cornerZ + 0.5);
    }
}