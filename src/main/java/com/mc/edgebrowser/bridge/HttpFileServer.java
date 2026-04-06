package com.mc.edgebrowser.bridge;

import com.mc.edgebrowser.EdgeBrowser;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;

public class HttpFileServer {

    private final EdgeBrowser plugin;
    private HttpServer server;
    private int port;
    private boolean running = false;

    public HttpFileServer(EdgeBrowser plugin, int port) {
        this.plugin = plugin;
        this.port = port;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            
            server.createContext("/bridge.html", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    File dataFolder = plugin.getDataFolder();
                    File htmlFile = new File(dataFolder, "bridge.html");
                    
                    if (!htmlFile.exists()) {
                        plugin.saveResource("bridge.html", false);
                    }
                    
                    if (htmlFile.exists()) {
                        byte[] content = Files.readAllBytes(htmlFile.toPath());
                        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                        exchange.sendResponseHeaders(200, content.length);
                        exchange.getResponseBody().write(content);
                    } else {
                        String error = "Bridge HTML file not found";
                        exchange.sendResponseHeaders(404, error.length());
                        exchange.getResponseBody().write(error.getBytes());
                    }
                    exchange.close();
                }
            });

            server.createContext("/", new HttpHandler() {
                @Override
                public void handle(HttpExchange exchange) throws IOException {
                    String response = "<html><body><h1>EdgeBrowser Bridge Server</h1>" +
                            "<p>Access <a href='/bridge.html'>/bridge.html</a> to connect</p>" +
                            "<p>WebSocket: ws://localhost:" + (port + 1) + "</p></body></html>";
                    exchange.getResponseHeaders().set("Content-Type", "text/html");
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    exchange.getResponseBody().write(response.getBytes());
                    exchange.close();
                }
            });

            server.setExecutor(null);
            server.start();
            running = true;
            plugin.getLogger().info("HTTP文件服务器已启动: http://localhost:" + port);
        } catch (IOException e) {
            plugin.getLogger().severe("HTTP文件服务器启动失败: " + e.getMessage());
            running = false;
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            running = false;
            plugin.getLogger().info("HTTP文件服务器已停止");
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }
}
