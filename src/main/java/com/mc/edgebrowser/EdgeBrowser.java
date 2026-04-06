package com.mc.edgebrowser;

import com.mc.edgebrowser.browser.BrowserManager;
import com.mc.edgebrowser.bridge.BridgeServer;
import com.mc.edgebrowser.bridge.HttpFileServer;
import com.mc.edgebrowser.command.EdgeCommand;
import com.mc.edgebrowser.config.ConfigManager;
import com.mc.edgebrowser.listener.InteractListener;
import com.mc.edgebrowser.listener.PlayerListener;
import com.mc.edgebrowser.map.MapManager;
import com.mc.edgebrowser.render.RenderEngine;
import org.bukkit.plugin.java.JavaPlugin;

public class EdgeBrowser extends JavaPlugin {

    private static EdgeBrowser instance;
    private ConfigManager configManager;
    private MapManager mapManager;
    private RenderEngine renderEngine;
    private BrowserManager browserManager;
    private BridgeServer bridgeServer;
    private HttpFileServer httpServer;
    private InteractListener interactListener;

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();

        configManager = new ConfigManager(this);
        configManager.load();
        mapManager = new MapManager(this);
        renderEngine = new RenderEngine(this);
        browserManager = new BrowserManager(this);
        bridgeServer = new BridgeServer(this);
        httpServer = new HttpFileServer(this, configManager.getBridgePort() + 1);

        this.interactListener = new InteractListener(this);
        getServer().getPluginManager().registerEvents(interactListener, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        EdgeCommand edgeCommand = new EdgeCommand(this);
        getCommand("edge").setExecutor(edgeCommand);
        getCommand("edge").setTabCompleter(edgeCommand);

        if (configManager.isBridgeEnabled()) {
            bridgeServer.start();
            httpServer.start();
        }

        getLogger().info("EdgeBrowser v" + getDescription().getVersion() + " 已启用!");
        getLogger().info("初始化耗时: " + (System.currentTimeMillis() - start) + "ms");

        if (configManager.isBridgeEnabled()) {
            getLogger().info("桥接模式已启动 - WebSocket: " + configManager.getBridgePort() + ", HTTP: " + (configManager.getBridgePort() + 1));
            getLogger().info("桥接页面: http://localhost:" + (configManager.getBridgePort() + 1) + "/bridge.html");
        }
        if (configManager.isServerBrowserEnabled()) {
            getLogger().info("服务端浏览器模式已启动");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("正在关闭 EdgeBrowser...");

        if (httpServer != null) {
            try {
                httpServer.stop();
            } catch (Exception e) {
                getLogger().warning("关闭HTTP服务器时发生错误: " + e.getMessage());
            }
        }

        if (bridgeServer != null) {
            try {
                bridgeServer.stop();
            } catch (InterruptedException e) {
                getLogger().warning("关闭桥接服务器时被中断: " + e.getMessage());
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                getLogger().warning("关闭桥接服务器时发生错误: " + e.getMessage());
            }
        }

        if (browserManager != null) {
            try {
                browserManager.shutdownAll();
            } catch (Exception e) {
                getLogger().warning("关闭浏览器管理器时发生错误: " + e.getMessage());
            }
        }

        if (renderEngine != null) {
            try {
                renderEngine.shutdown();
            } catch (Exception e) {
                getLogger().warning("关闭渲染引擎时发生错误: " + e.getMessage());
            }
        }

        getLogger().info("EdgeBrowser 已禁用!");
    }

    public static EdgeBrowser getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MapManager getMapManager() {
        return mapManager;
    }

    public RenderEngine getRenderEngine() {
        return renderEngine;
    }

    public BrowserManager getBrowserManager() {
        return browserManager;
    }

    public BridgeServer getBridgeServer() {
        return bridgeServer;
    }

    public HttpFileServer getHttpServer() {
        return httpServer;
    }

    public InteractListener getInteractListener() {
        return interactListener;
    }
}