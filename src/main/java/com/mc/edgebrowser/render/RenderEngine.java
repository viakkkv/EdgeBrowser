package com.mc.edgebrowser.render;

import com.mc.edgebrowser.EdgeBrowser;
import com.mc.edgebrowser.browser.BrowserInstance;
import com.mc.edgebrowser.bridge.BridgeServer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.*;

public class RenderEngine {

    private final EdgeBrowser plugin;
    private final ExecutorService renderPool;
    private final Map<String, ScheduledFuture<?>> renderTasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledExecutorService> renderSchedulers = new ConcurrentHashMap<>();
    private final Map<String, byte[]> lastFrameData = new ConcurrentHashMap<>();
    private boolean running = true;

    public RenderEngine(EdgeBrowser plugin) {
        this.plugin = plugin;
        int cores = Runtime.getRuntime().availableProcessors();
        this.renderPool = new ForkJoinPool(Math.max(2, cores - 1));
    }

    public void startRendering(BrowserInstance browser) {
        if (renderTasks.containsKey(browser.getId())) return;

        int interval = Math.max(50, 1000 / browser.getTargetFps());

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            if (!browser.isActive() || browser.isPaused() || !running) return;
            try {
                renderFrame(browser);
            } catch (Exception e) {
                if (plugin.getConfigManager().isDebug()) {
                    e.printStackTrace();
                }
            }
        }, 100, interval, TimeUnit.MILLISECONDS);

        renderTasks.put(browser.getId(), future);
        renderSchedulers.put(browser.getId(), scheduler);
    }

    private void renderFrame(BrowserInstance browser) {
        BridgeServer bridge = plugin.getBridgeServer();
        if (bridge == null || !bridge.isEnabled()) return;

        byte[] jpegData = bridge.getLatestFrame(browser.getId());
        if (jpegData == null || jpegData.length == 0) return;

        renderPool.submit(() -> {
            try {
                BufferedImage image = decodeJpeg(jpegData);
                if (image == null) return;

                int webW = image.getWidth();
                int webH = image.getHeight();
                int mapsX = browser.getMapsX();
                int mapsY = browser.getMapsY();
                int brightness = browser.getBrightness();
                int enhance = plugin.getConfigManager().getColorEnhance();
                boolean delta = plugin.getConfigManager().isDeltaUpdate();
                int threshold = plugin.getConfigManager().getDeltaThreshold();

                int[] argbPixels = image.getRGB(0, 0, webW, webH, null, 0, webW);

                for (int mx = 0; mx < mapsX; mx++) {
                    for (int my = 0; my < mapsY; my++) {
                        byte[] mapColors = renderMapTile(
                            argbPixels, webW, webH, mx, my, mapsX, mapsY,
                            brightness, enhance, delta, threshold, browser.getId() + "_" + mx + "_" + my
                        );

                        if (mapColors != null) {
                            final int fmx = mx, fmy = my;
                            final byte[] fColors = mapColors;
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                browser.getDisplayBoard().updateMapDisplay(fmx, fmy, fColors);
                            });
                        }
                    }
                }

                browser.incrementFrameCount();
                browser.setLastFrameTime(System.currentTimeMillis());

            } catch (Exception e) {
                if (plugin.getConfigManager().isDebug()) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void refreshAllMaps(BrowserInstance browser) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (int mx = 0; mx < browser.getMapsX(); mx++) {
                for (int my = 0; my < browser.getMapsY(); my++) {
                    String cacheKey = browser.getId() + "_" + mx + "_" + my;
                    lastFrameData.remove(cacheKey);
                }
            }
        });
    }

    private byte[] renderMapTile(int[] argbPixels, int webW, int webH,
                                  int tileX, int tileY, int totalX, int totalY,
                                  int brightness, int enhance, boolean delta, int threshold,
                                  String cacheKey) {
        int tileSize = 128;

        int srcX = tileX * webW / totalX;
        int srcY = tileY * webH / totalY;
        int srcEndX = (tileX + 1) * webW / totalX;
        int srcEndY = (tileY + 1) * webH / totalY;
        int srcW = srcEndX - srcX;
        int srcH = srcEndY - srcY;

        if (srcW <= 0 || srcH <= 0) return null;

        int[] tileArgb = new int[tileSize * tileSize];
        if (srcW == tileSize && srcH == tileSize) {
            for (int y = 0; y < tileSize; y++) {
                int srcOff = (srcY + y) * webW + srcX;
                int dstOff = y * tileSize;
                System.arraycopy(argbPixels, srcOff, tileArgb, dstOff, tileSize);
            }
        } else {
            for (int y = 0; y < tileSize; y++) {
                int sy = srcY + y * srcH / tileSize;
                if (sy >= webH) sy = webH - 1;
                for (int x = 0; x < tileSize; x++) {
                    int sx = srcX + x * srcW / tileSize;
                    if (sx >= webW) sx = webW - 1;
                    tileArgb[y * tileSize + x] = argbPixels[sy * webW + sx];
                }
            }
        }

        byte[] mapColors = new byte[tileSize * tileSize];
        ColorConverter.convertFrame(tileArgb, mapColors, brightness, enhance);

        if (delta) {
            byte[] last = lastFrameData.get(cacheKey);
            if (last != null) {
                boolean changed = false;
                for (int i = 0; i < mapColors.length; i++) {
                    int diff = Math.abs(mapColors[i] - last[i]);
                    if (diff > threshold) {
                        changed = true;
                        break;
                    }
                }
                if (!changed) return null;
            }
            lastFrameData.put(cacheKey, mapColors.clone());
        }

        return mapColors;
    }

    private BufferedImage decodeJpeg(byte[] data) {
        try {
            BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
            if (img.getType() != BufferedImage.TYPE_INT_ARGB) {
                BufferedImage argbImg = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = argbImg.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();
                return argbImg;
            }
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    public void stopRendering(String browserId) {
        ScheduledFuture<?> future = renderTasks.remove(browserId);
        if (future != null) {
            future.cancel(false);
        }
        ScheduledExecutorService scheduler = renderSchedulers.remove(browserId);
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        lastFrameData.entrySet().removeIf(e -> e.getKey().startsWith(browserId + "_"));
    }

    public void onFrameReceived(String browserId, byte[] jpegData) {
    }

    public void shutdown() {
        running = false;
        for (ScheduledFuture<?> f : renderTasks.values()) {
            f.cancel(false);
        }
        for (ScheduledExecutorService s : renderSchedulers.values()) {
            s.shutdownNow();
        }
        renderTasks.clear();
        renderSchedulers.clear();
        renderPool.shutdownNow();
        lastFrameData.clear();
    }
}