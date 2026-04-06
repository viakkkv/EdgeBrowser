package com.mc.edgebrowser.config;

import com.mc.edgebrowser.EdgeBrowser;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {

    private final EdgeBrowser plugin;
    private FileConfiguration config;

    public ConfigManager(EdgeBrowser plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    public int getTargetFps() { return config.getInt("render.target-fps", 8); }
    public String getQuality() { return config.getString("render.quality", "high"); }
    public int getBrightness() { return config.getInt("render.brightness", 5); }
    public int getColorEnhance() { return config.getInt("render.color-enhance", 15); }
    public boolean isDeltaUpdate() { return config.getBoolean("render.delta-update", true); }
    public int getDeltaThreshold() { return config.getInt("render.delta-threshold", 8); }
    public boolean isBilinear() { return config.getBoolean("render.bilinear", false); }

    public int getWebWidth() {
        if (isServerBrowserEnabled()) return getServerWebWidth();
        String q = getQuality();
        return switch (q) {
            case "low" -> 160;
            case "medium" -> 320;
            case "high" -> 480;
            case "ultra" -> 640;
            default -> 480;
        };
    }

    public int getWebHeight() {
        if (isServerBrowserEnabled()) return getServerWebHeight();
        String q = getQuality();
        return switch (q) {
            case "low" -> 128;
            case "medium" -> 256;
            case "high" -> 384;
            case "ultra" -> 512;
            default -> 384;
        };
    }

    public boolean isServerBrowserEnabled() { return config.getBoolean("server-browser.enabled", false); }
    public String getBrowserType() { return config.getString("server-browser.browser-type", "chromium"); }
    public String getBrowserPath() { return config.getString("server-browser.browser-path", ""); }
    public int getServerWebWidth() { return config.getInt("server-browser.web-width", 640); }
    public int getServerWebHeight() { return config.getInt("server-browser.web-height", 480); }
    public boolean isHeadless() { return config.getBoolean("server-browser.headless", true); }
    public String getExtraArgs() { return config.getString("server-browser.extra-args", ""); }

    public boolean isBridgeEnabled() { return config.getBoolean("bridge.enabled", true); }
    public int getBridgePort() { return config.getInt("bridge.port", 9527); }
    public String getBridgeBind() { return config.getString("bridge.bind", "0.0.0.0"); }
    public String getAuthKey() { return config.getString("bridge.auth-key", "edge-browser-2024-secret"); }
    public int getBridgeTimeout() { return config.getInt("bridge.timeout", 30); }
    public boolean isAutoReconnect() { return config.getBoolean("bridge.auto-reconnect", true); }
    public int getMaxConnections() { return config.getInt("bridge.max-connections", 10); }

    public String getDefaultUrl() { return config.getString("defaults.default-url", "https://www.bing.com"); }
    public int getMapSize() { return config.getInt("defaults.map-size", 1); }
    public boolean isAutoShow() { return config.getBoolean("defaults.auto-show", true); }
    public int getShowRange() { return config.getInt("defaults.show-range", 20); }
    public int getInteractRange() { return config.getInt("defaults.interact-range", 20); }
    public List<String> getUrlWhitelist() { return config.getStringList("defaults.url-whitelist"); }
    public List<String> getUrlBlacklist() { return config.getStringList("defaults.url-blacklist"); }
    public boolean isAllowZoom() { return config.getBoolean("defaults.allow-zoom", true); }

    public String getSelectorMaterial() { return config.getString("frames.selector.material", "GOLDEN_SHOVEL"); }
    public String getSelectorName() { return config.getString("frames.selector.name", "&6&l[Edge] &f区域选择器"); }
    public boolean isSelectorGlow() { return config.getBoolean("frames.selector.glow", true); }
    public String getBorderMaterial() { return config.getString("frames.border.material", "LIGHT_GRAY_STAINED_GLASS"); }
    public String getFloorMaterial() { return config.getString("frames.floor.material", "BLACK_CONCRETE"); }

    public int getCooldown() { return config.getInt("interaction.cooldown", 50); }
    public String getClickParticle() { return config.getString("interaction.click-particle", "FLAME"); }
    public String getClickSound() { return config.getString("interaction.click-sound", "ENTITY_PLAYER_BURP"); }
    public boolean isSneakDisable() { return config.getBoolean("interaction.sneak-disable", false); }

    public boolean requireUsePermission() { return config.getBoolean("permissions.require-use-permission", false); }
    public boolean requireClickPermission() { return config.getBoolean("permissions.require-click-permission", false); }
    public boolean requireUrlPermission() { return config.getBoolean("permissions.require-url-permission", false); }
    public boolean requireKeyPermission() { return config.getBoolean("permissions.require-key-permission", false); }

    public String getMsg(String key) {
        String raw = config.getString("messages." + key, "&c消息未定义: " + key);
        return ChatColor.translateAlternateColorCodes('&', raw);
    }
    public String getPrefix() { return getMsg("prefix"); }

    public boolean isDebug() { return config.getBoolean("debug.enabled", false); }
    public boolean logPackets() { return config.getBoolean("debug.log-packets", false); }
    public boolean logClicks() { return config.getBoolean("debug.log-clicks", false); }
    public boolean logFps() { return config.getBoolean("debug.log-fps", false); }
}