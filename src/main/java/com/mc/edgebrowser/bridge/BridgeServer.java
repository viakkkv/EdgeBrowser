package com.mc.edgebrowser.bridge;

import com.mc.edgebrowser.EdgeBrowser;
import com.mc.edgebrowser.browser.BrowserInstance;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BridgeServer extends WebSocketServer {

    private final EdgeBrowser plugin;
    private final Gson gson = new Gson();
    private final Map<String, WebSocket> browserConnections = new ConcurrentHashMap<>();
    private final Map<String, byte[]> latestFrames = new ConcurrentHashMap<>();
    private final Map<String, Long> frameTimes = new ConcurrentHashMap<>();
    private boolean enabled = false;
    private String authKey;

    public BridgeServer(EdgeBrowser plugin) {
        super(new InetSocketAddress(
                plugin.getConfigManager().getBridgeBind(),
                plugin.getConfigManager().getBridgePort()
        ));
        this.plugin = plugin;
        this.authKey = plugin.getConfigManager().getAuthKey();
        setConnectionLostTimeout(plugin.getConfigManager().getBridgeTimeout());
        setMaxPendingConnections(plugin.getConfigManager().getMaxConnections());
    }

    public void start() {
        try {
            generateBridgeHtml();
            super.start();
            enabled = true;
            plugin.getLogger().info("桥接服务器已启动: " + getAddress());
        } catch (Exception e) {
            plugin.getLogger().severe("桥接服务器启动失败: " + e.getMessage());
            enabled = false;
        }
    }

    private void generateBridgeHtml() {
        try {
            File dataFolder = plugin.getDataFolder();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            File htmlFile = new File(dataFolder, "bridge.html");
            if (!htmlFile.exists()) {
                plugin.saveResource("bridge.html", false);
                plugin.getLogger().info("已生成桥接页面: " + htmlFile.getAbsolutePath());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("生成桥接页面失败: " + e.getMessage());
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("新连接: " + conn.getRemoteSocketAddress());
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        browserConnections.entrySet().removeIf(e -> e.getValue() == conn);
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("连接关闭: " + conn.getRemoteSocketAddress());
        }
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        try {
            JsonObject json = JsonParser.parseString(message).getAsJsonObject();
            String type = json.get("type").getAsString();

            switch (type) {
                case "auth" -> handleAuth(conn, json);
                case "frame" -> handleFrame(conn, json);
                case "event" -> handleEvent(json);
                case "status" -> handleStatus(conn, json);
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                plugin.getLogger().warning("消息处理错误: " + e.getMessage());
            }
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        try {
            String browserId = null;
            for (Map.Entry<String, WebSocket> e : browserConnections.entrySet()) {
                if (e.getValue() == conn) {
                    browserId = e.getKey();
                    break;
                }
            }
            if (browserId != null) {
                byte[] bytes = new byte[message.remaining()];
                message.get(bytes);
                latestFrames.put(browserId, bytes);
                frameTimes.put(browserId, System.currentTimeMillis());
            }
        } catch (Exception e) {
            if (plugin.getConfigManager().isDebug()) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().warning("WebSocket错误: " + ex.getMessage());
        }
    }

    @Override
    public void onStart() {
        plugin.getLogger().info("桥接服务器就绪");
    }

    private void handleAuth(WebSocket conn, JsonObject json) {
        String key = json.get("key").getAsString();
        String browserId = json.has("browserId") ? json.get("browserId").getAsString() : null;

        if (!authKey.equals(key)) {
            JsonObject resp = new JsonObject();
            resp.addProperty("type", "auth_result");
            resp.addProperty("success", false);
            resp.addProperty("message", "验证密钥错误");
            conn.send(resp.toString());
            conn.close(4001, "认证失败");
            return;
        }

        if (browserId != null) {
            BrowserInstance browser = plugin.getBrowserManager().getBrowser(browserId);
            if (browser == null) {
                JsonObject resp = new JsonObject();
                resp.addProperty("type", "auth_result");
                resp.addProperty("success", false);
                resp.addProperty("message", "浏览器ID不存在");
                conn.send(resp.toString());
                return;
            }

            browserConnections.put(browserId, conn);

            JsonObject config = new JsonObject();
            config.addProperty("type", "config");
            config.addProperty("url", browser.getCurrentUrl());
            config.addProperty("width", plugin.getConfigManager().getWebWidth());
            config.addProperty("height", plugin.getConfigManager().getWebHeight());
            config.addProperty("fps", browser.getTargetFps());
            conn.send(config.toString());
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("type", "auth_result");
        resp.addProperty("success", true);
        conn.send(resp.toString());

        plugin.getLogger().info("客户端已认证: " + conn.getRemoteSocketAddress() +
                (browserId != null ? " -> " + browserId : ""));
    }

    private void handleFrame(WebSocket conn, JsonObject json) {
    }

    private void handleEvent(JsonObject json) {
        String browserId = json.has("browserId") ? json.get("browserId").getAsString() : "";
        String event = json.has("event") ? json.get("event").getAsString() : "";
        if (plugin.getConfigManager().logClicks()) {
            plugin.getLogger().info("浏览器事件: " + event + " from " + browserId);
        }
    }

    private void handleStatus(WebSocket conn, JsonObject json) {
        String browserId = json.has("browserId") ? json.get("browserId").getAsString() : null;
        String status = json.has("status") ? json.get("status").getAsString() : "";

        if ("loading".equals(status) && browserId != null) {
            BrowserInstance browser = plugin.getBrowserManager().getBrowser(browserId);
            if (browser != null && json.has("url")) {
                browser.setCurrentUrl(json.get("url").getAsString());
            }
        }
    }

    public byte[] getLatestFrame(String browserId) {
        byte[] frame = latestFrames.get(browserId);
        if (frame == null) return null;

        Long time = frameTimes.get(browserId);
        if (time != null && System.currentTimeMillis() - time > 2000) {
            latestFrames.remove(browserId);
            return null;
        }

        return frame;
    }

    public void sendClick(String browserId, double x, double y, String button) {
        WebSocket conn = browserConnections.get(browserId);
        if (conn == null || !conn.isOpen()) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "click");
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("button", button);
        conn.send(json.toString());
    }

    public void sendScroll(String browserId, double deltaX, double deltaY) {
        WebSocket conn = browserConnections.get(browserId);
        if (conn == null || !conn.isOpen()) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "scroll");
        json.addProperty("deltaX", deltaX);
        json.addProperty("deltaY", deltaY);
        conn.send(json.toString());
    }

    public void sendKey(String browserId, String key, boolean shift, boolean ctrl, boolean alt) {
        WebSocket conn = browserConnections.get(browserId);
        if (conn == null || !conn.isOpen()) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "key");
        json.addProperty("key", key);
        json.addProperty("shift", shift);
        json.addProperty("ctrl", ctrl);
        json.addProperty("alt", alt);
        conn.send(json.toString());
    }

    public void sendInput(String browserId, String text) {
        WebSocket conn = browserConnections.get(browserId);
        if (conn == null || !conn.isOpen()) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "input");
        json.addProperty("text", text);
        conn.send(json.toString());
    }

    public void sendNavigate(String browserId, String url) {
        WebSocket conn = browserConnections.get(browserId);
        if (conn == null || !conn.isOpen()) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "navigate");
        json.addProperty("url", url);
        conn.send(json.toString());
    }

    public void sendCommand(String browserId, String command) {
        WebSocket conn = browserConnections.get(browserId);
        if (conn == null || !conn.isOpen()) return;
        JsonObject json = new JsonObject();
        json.addProperty("type", "command");
        json.addProperty("command", command);
        conn.send(json.toString());
    }

    public void notifyBrowserCreated(BrowserInstance browser) {}

    public void notifyBrowserRemoved(String browserId) {
        WebSocket conn = browserConnections.remove(browserId);
        if (conn != null && conn.isOpen()) {
            JsonObject json = new JsonObject();
            json.addProperty("type", "close");
            conn.send(json.toString());
            conn.close(1000, "浏览器已删除");
        }
        latestFrames.remove(browserId);
        frameTimes.remove(browserId);
    }

    public boolean isEnabled() { return enabled; }
    public boolean isConnected(String browserId) {
        WebSocket conn = browserConnections.get(browserId);
        return conn != null && conn.isOpen();
    }
}