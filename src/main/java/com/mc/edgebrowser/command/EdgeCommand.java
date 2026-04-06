package com.mc.edgebrowser.command;

import com.mc.edgebrowser.EdgeBrowser;
import com.mc.edgebrowser.browser.BrowserInstance;
import com.mc.edgebrowser.listener.InteractListener;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EdgeCommand implements org.bukkit.command.CommandExecutor, TabCompleter {

    private final EdgeBrowser plugin;
    private InteractListener interactListener;

    public EdgeCommand(EdgeBrowser plugin) {
        this.plugin = plugin;
        this.interactListener = plugin.getInteractListener();
        if (this.interactListener == null) {
            plugin.getLogger().warning("无法获取 InteractListener，浏览器创建功能将不可用");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create" -> cmdCreate(sender, args);
            case "remove" -> cmdRemove(sender, args);
            case "list" -> cmdList(sender);
            case "info" -> cmdInfo(sender, args);
            case "reload" -> cmdReload(sender);
            case "url" -> cmdUrl(sender, args);
            case "click" -> cmdClick(sender, args);
            case "pause" -> cmdPause(sender, args);
            case "resume" -> cmdResume(sender, args);
            case "clear" -> cmdClear(sender, args);
            case "teleport" -> cmdTeleport(sender, args);
            case "brightness" -> cmdBrightness(sender, args);
            case "scale" -> cmdScale(sender, args);
            case "fps" -> cmdFps(sender, args);
            case "border" -> cmdBorder(sender, args);
            case "title" -> cmdTitle(sender, args);
            case "key" -> cmdKey(sender, args);
            case "scroll" -> cmdScroll(sender, args);
            case "input" -> cmdInput(sender, args);
            case "back" -> cmdBack(sender, args);
            case "forward" -> cmdForward(sender, args);
            case "refresh" -> cmdRefresh(sender, args);
            case "toggle" -> cmdToggle(sender, args);
            case "rotate" -> cmdRotate(sender, args);
            case "give" -> cmdGive(sender);
            case "help" -> sendHelp(sender);
            default -> sender.sendMessage(plugin.getConfigManager().getPrefix() + "§c未知子命令。使用 /edge help 查看帮助。");
        }

        return true;
    }

    private void cmdCreate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c此命令只能由玩家执行。");
            return;
        }

        String id = args.length > 1 ? args[1] : "browser_" + System.currentTimeMillis() % 10000;

        if (plugin.getBrowserManager().getBrowser(id) != null) {
            sender.sendMessage(plugin.getConfigManager().getMsg("already-exists").replace("%id%", id));
            return;
        }

        if (interactListener == null) {
            sender.sendMessage("§c内部错误：交互监听器未加载，无法获取选择位置。");
            sender.sendMessage("§c请重启服务器或重新加载插件。");
            return;
        }

        int[] p1 = interactListener.getPos1(player.getUniqueId());
        int[] p2 = interactListener.getPos2(player.getUniqueId());
        String world = interactListener.getPosWorld(player.getUniqueId());

        if (p1 == null || p2 == null || world == null) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-selection"));
            return;
        }

        BrowserInstance browser = plugin.getBrowserManager().createBrowser(
                id, player.getUniqueId(), world,
                p1[0], p1[1], p1[2], p2[0], p2[1], p2[2]
        );

        if (browser == null) {
            sender.sendMessage(plugin.getConfigManager().getMsg("invalid-selection"));
            return;
        }

        browser.getDisplayBoard().place();

        String msg = plugin.getConfigManager().getMsg("browser-created")
                .replace("%id%", id)
                .replace("%w%", String.valueOf(browser.getMapsX()))
                .replace("%h%", String.valueOf(browser.getMapsY()));
        sender.sendMessage(msg);

        if (plugin.getConfigManager().isBridgeEnabled()) {
            sender.sendMessage(plugin.getConfigManager().getMsg("bridge-waiting"));
            sender.sendMessage("§7连接信息: §ews://" +
                    plugin.getConfigManager().getBridgeBind() + ":" +
                    plugin.getConfigManager().getBridgePort() + " §7密钥: §e" +
                    plugin.getConfigManager().getAuthKey());
        }

        interactListener.clearSelection(player.getUniqueId());
    }

    private void cmdRemove(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage("§c用法: /edge remove <id>");
            return;
        }
        String id = args[1];
        if (plugin.getBrowserManager().removeBrowser(id)) {
            sender.sendMessage(plugin.getConfigManager().getMsg("browser-removed").replace("%id%", id));
        } else {
            sender.sendMessage(plugin.getConfigManager().getMsg("not-found").replace("%id%", id));
        }
    }

    private void cmdList(CommandSender sender) {
        if (!sender.hasPermission("edge.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        var browsers = plugin.getBrowserManager().getAllBrowsers();
        if (browsers.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + "§7没有活跃的浏览器。");
            return;
        }
        sender.sendMessage(plugin.getConfigManager().getMsg("browser-list"));
        for (BrowserInstance b : browsers) {
            String status = b.isActive() ? (b.isPaused() ? "§e暂停" : "§a运行中") : "§c已停止";
            String fps = b.getRealFps() + " FPS";
            String msg = plugin.getConfigManager().getMsg("browser-list-item")
                    .replace("%id%", b.getId())
                    .replace("%url%", truncate(b.getCurrentUrl(), 30))
                    .replace("%w%", String.valueOf(b.getMapsX()))
                    .replace("%h%", String.valueOf(b.getMapsY()))
                    .replace("%status%", status + " §7(" + fps + ")");
            sender.sendMessage(msg);
        }
    }

    private void cmdInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§c用法: /edge info <id>");
            return;
        }
        BrowserInstance b = plugin.getBrowserManager().getBrowser(args[1]);
        if (b == null) {
            sender.sendMessage(plugin.getConfigManager().getMsg("not-found").replace("%id%", args[1]));
            return;
        }
        String status = b.isActive() ? (b.isPaused() ? "§e暂停" : "§a运行中") : "§c已停止";
        sender.sendMessage(plugin.getConfigManager().getMsg("browser-info"));
        sender.sendMessage(plugin.getConfigManager().getMsg("browser-info-id").replace("%id%", b.getId()));
        sender.sendMessage(plugin.getConfigManager().getMsg("browser-info-url").replace("%url%", b.getCurrentUrl()));
        sender.sendMessage(plugin.getConfigManager().getMsg("browser-info-size")
                .replace("%w%", String.valueOf(b.getMapsX())).replace("%h%", String.valueOf(b.getMapsY())));
        sender.sendMessage(plugin.getConfigManager().getMsg("browser-info-fps").replace("%fps%", b.getRealFps() + ""));
        sender.sendMessage(plugin.getConfigManager().getMsg("browser-info-status").replace("%status%", status));
        sender.sendMessage(plugin.getConfigManager().getMsg("browser-info-location")
                .replace("%world%", b.getWorld().getName())
                .replace("%x%", String.valueOf(b.getCornerX()))
                .replace("%y%", String.valueOf(b.getCornerY()))
                .replace("%z%", String.valueOf(b.getCornerZ())));
    }

    private void cmdReload(CommandSender sender) {
        if (!sender.hasPermission("edge.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        plugin.getConfigManager().reload();
        sender.sendMessage(plugin.getConfigManager().getMsg("reloaded"));
    }

    private void cmdUrl(CommandSender sender, String[] args) {
        if (plugin.getConfigManager().requireUrlPermission() &&
                !sender.hasPermission("edge.command.url")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§c用法: /edge url <id> <url>");
            return;
        }
        String id = args[1];
        String url = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        for (String blocked : plugin.getConfigManager().getUrlBlacklist()) {
            if (url.contains(blocked)) {
                sender.sendMessage(plugin.getConfigManager().getMsg("url-blocked"));
                return;
            }
        }

        BrowserInstance b = plugin.getBrowserManager().getBrowser(id);
        if (b == null) {
            sender.sendMessage(plugin.getConfigManager().getMsg("not-found").replace("%id%", id));
            return;
        }

        b.setCurrentUrl(url);
        plugin.getBridgeServer().sendNavigate(id, url);
        sender.sendMessage(plugin.getConfigManager().getMsg("url-navigated").replace("%url%", url));
    }

    private void cmdClick(CommandSender sender, String[] args) {
        if (plugin.getConfigManager().requireClickPermission() &&
                !sender.hasPermission("edge.command.click")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 5) {
            sender.sendMessage("§c用法: /edge click <id> <x> <y> <button>");
            sender.sendMessage("§7button: left, middle, right, drag-start, drag-end");
            return;
        }
        String id = args[1];
        double x = Double.parseDouble(args[2]);
        double y = Double.parseDouble(args[3]);
        String button = args[4];
        plugin.getBridgeServer().sendClick(id, x, y, button);
        sender.sendMessage("§a点击已发送: " + String.format("(%.2f, %.2f) %s", x, y, button));
    }

    private void cmdPause(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.pause")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 2) { sender.sendMessage("§c用法: /edge pause <id>"); return; }
        BrowserInstance b = plugin.getBrowserManager().getBrowser(args[1]);
        if (b == null) { sender.sendMessage(plugin.getConfigManager().getMsg("not-found").replace("%id%", args[1])); return; }
        b.setPaused(true);
        sender.sendMessage(plugin.getConfigManager().getMsg("paused"));
    }

    private void cmdResume(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.resume")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 2) { sender.sendMessage("§c用法: /edge resume <id>"); return; }
        BrowserInstance b = plugin.getBrowserManager().getBrowser(args[1]);
        if (b == null) { sender.sendMessage(plugin.getConfigManager().getMsg("not-found").replace("%id%", args[1])); return; }
        b.setPaused(false);
        sender.sendMessage(plugin.getConfigManager().getMsg("resumed"));
    }

    private void cmdClear(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.clear")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 2) { sender.sendMessage("§c用法: /edge clear <id>"); return; }
        BrowserInstance b = plugin.getBrowserManager().getBrowser(args[1]);
        if (b == null) { sender.sendMessage(plugin.getConfigManager().getMsg("not-found").replace("%id%", args[1])); return; }
        plugin.getBridgeServer().sendCommand(args[1], "clear");
        sender.sendMessage(plugin.getConfigManager().getMsg("cleared"));
    }

    private void cmdTeleport(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.teleport")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) { sender.sendMessage("§c只能由玩家执行。"); return; }
        if (args.length < 2) { sender.sendMessage("§c用法: /edge teleport <id>"); return; }
        BrowserInstance b = plugin.getBrowserManager().getBrowser(args[1]);
        if (b == null) { sender.sendMessage(plugin.getConfigManager().getMsg("not-found").replace("%id%", args[1])); return; }
        player.teleport(b.getCenterLocation());
        sender.sendMessage(plugin.getConfigManager().getMsg("teleported").replace("%id%", args[1]));
    }

    private void cmdBrightness(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.brightness")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 3) { sender.sendMessage("§c用法: /edge brightness <id> <-50~50>"); return; }
        BrowserInstance b = plugin.getBrowserManager().getBrowser(args[1]);
        if (b == null) { sender.sendMessage(plugin.getConfigManager().getMsg("not-found").replace("%id%", args[1])); return; }
        int val = Integer.parseInt(args[2]);
        b.setBrightness(val);
        sender.sendMessage(plugin.getConfigManager().getMsg("brightness-set").replace("%value%", String.valueOf(val)));
    }

    private void cmdScale(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage("§c用法: /edge scale <id> <0.1~5.0>"); return; }
        plugin.getBridgeServer().sendCommand(args[1], "scale:" + args[2]);
        sender.sendMessage(plugin.getConfigManager().getMsg("scale-set").replace("%value%", args[2]));
    }

    private void cmdFps(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.fps")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 3) { sender.sendMessage("§c用法: /edge fps <id> <1-30>"); return; }
        BrowserInstance b = plugin.getBrowserManager().getBrowser(args[1]);
        if (b == null) { sender.sendMessage(plugin.getConfigManager().getMsg("not-found").replace("%id%", args[1])); return; }
        int fps = Integer.parseInt(args[2]);
        fps = Math.max(1, Math.min(30, fps));
        b.setTargetFps(fps);
        plugin.getRenderEngine().stopRendering(args[1]);
        plugin.getRenderEngine().startRendering(b);
        sender.sendMessage(plugin.getConfigManager().getMsg("fps-set").replace("%value%", String.valueOf(fps)));
    }

    private void cmdBorder(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.border")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 3) { sender.sendMessage("§c用法: /edge border <id> <material>"); return; }
        sender.sendMessage(plugin.getConfigManager().getMsg("border-set"));
    }

    private void cmdTitle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.title")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 3) { sender.sendMessage("§c用法: /edge title <id> <标题>"); return; }
        BrowserInstance b = plugin.getBrowserManager().getBrowser(args[1]);
        if (b == null) { sender.sendMessage(plugin.getConfigManager().getMsg("not-found").replace("%id%", args[1])); return; }
        String title = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        b.setTitle(title);
        sender.sendMessage(plugin.getConfigManager().getMsg("title-set").replace("%title%", title));
    }

    private void cmdKey(CommandSender sender, String[] args) {
        if (plugin.getConfigManager().requireKeyPermission() &&
                !sender.hasPermission("edge.command.key")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 3) {
            sender.sendMessage("§c用法: /edge key <id> <按键名>");
            sender.sendMessage("§7示例: /edge key mybrowser Enter");
            sender.sendMessage("§7特殊键: Enter, Backspace, Tab, Escape, ArrowUp, ArrowDown, ArrowLeft, ArrowRight, F1-F12");
            return;
        }
        String id = args[1];
        String key = args[2];
        plugin.getBridgeServer().sendKey(id, key, false, false, false);
        sender.sendMessage(plugin.getConfigManager().getMsg("key-sent").replace("%key%", key));
    }

    private void cmdScroll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.scroll")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 3) { sender.sendMessage("§c用法: /edge scroll <id> <delta>"); return; }
        int delta = Integer.parseInt(args[2]);
        plugin.getBridgeServer().sendScroll(args[1], 0, -delta * 120);
        sender.sendMessage(plugin.getConfigManager().getMsg("scroll-done").replace("%delta%", String.valueOf(delta)));
    }

    private void cmdInput(CommandSender sender, String[] args) {
        if (plugin.getConfigManager().requireKeyPermission() &&
                !sender.hasPermission("edge.command.input")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 3) { sender.sendMessage("§c用法: /edge input <id> <文本>"); return; }
        String id = args[1];
        String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        plugin.getBridgeServer().sendInput(id, text);
        sender.sendMessage(plugin.getConfigManager().getMsg("input-sent").replace("%text%", text));
    }

    private void cmdBack(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.back")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 2) { sender.sendMessage("§c用法: /edge back <id>"); return; }
        plugin.getBridgeServer().sendCommand(args[1], "back");
        sender.sendMessage("§a已后退。");
    }

    private void cmdForward(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.forward")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 2) { sender.sendMessage("§c用法: /edge forward <id>"); return; }
        plugin.getBridgeServer().sendCommand(args[1], "forward");
        sender.sendMessage("§a已前进。");
    }

    private void cmdRefresh(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.refresh")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 2) { sender.sendMessage("§c用法: /edge refresh <id>"); return; }
        plugin.getBridgeServer().sendCommand(args[1], "refresh");
        sender.sendMessage("§a页面已刷新。");
    }

    private void cmdToggle(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.toggle")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 2) { sender.sendMessage("§c用法: /edge toggle <id>"); return; }
        BrowserInstance b = plugin.getBrowserManager().getBrowser(args[1]);
        if (b == null) { sender.sendMessage(plugin.getConfigManager().getMsg("not-found").replace("%id%", args[1])); return; }
        b.setActive(!b.isActive());
        if (b.isActive()) {
            plugin.getRenderEngine().startRendering(b);
            sender.sendMessage("§a浏览器已启用。");
        } else {
            plugin.getRenderEngine().stopRendering(args[1]);
            sender.sendMessage("§c浏览器已禁用。");
        }
    }

    private void cmdRotate(CommandSender sender, String[] args) {
        if (!sender.hasPermission("edge.command.rotate")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (args.length < 3) { sender.sendMessage("§c用法: /edge rotate <id> <0|90|180|270>"); return; }
        plugin.getBridgeServer().sendCommand(args[1], "rotate:" + args[2]);
        sender.sendMessage("§a旋转已应用: " + args[2] + "°");
    }

    private void cmdGive(CommandSender sender) {
        if (!sender.hasPermission("edge.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMsg("no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§c只能由玩家执行。");
            return;
        }

        Material mat;
        try {
            mat = Material.valueOf(plugin.getConfigManager().getSelectorMaterial());
        } catch (IllegalArgumentException e) {
            mat = Material.GOLDEN_SHOVEL;
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getConfigManager().getSelectorName());
        meta.setLore(List.of(
                "§7左键选择第一个角点",
                "§7右键选择第二个角点",
                "§7然后使用 §e/edge create"
        ));

        if (plugin.getConfigManager().isSelectorGlow()) {
            try {
                Enchantment glow = Enchantment.getByKey(NamespacedKey.minecraft("unbreaking"));
                if (glow != null) meta.addEnchant(glow, 1, true);
            } catch (Exception e1) {
                try {
                    meta.addEnchant(Enchantment.DURABILITY, 1, true);
                } catch (Exception e2) {
                }
            }
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        player.getInventory().addItem(item);
        sender.sendMessage(plugin.getConfigManager().getMsg("selector-given"));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getMsg("help-header"));
        sender.sendMessage("§e/edge give §7- 获取金铲子选择器");
        sender.sendMessage("§e/edge create [id] §7- 创建浏览器");
        sender.sendMessage("§e/edge remove <id> §7- 删除浏览器");
        sender.sendMessage("§e/edge list §7- 列出所有浏览器");
        sender.sendMessage("§e/edge info <id> §7- 查看浏览器信息");
        sender.sendMessage("§e/edge url <id> <url> §7- 导航到URL");
        sender.sendMessage("§e/edge click <id> <x> <y> <btn> §7- 模拟点击");
        sender.sendMessage("§e/edge key <id> <按键> §7- 发送按键");
        sender.sendMessage("§e/edge input <id> <文本> §7- 输入文本");
        sender.sendMessage("§e/edge scroll <id> <delta> §7- 滚动页面");
        sender.sendMessage("§e/edge back <id> §7- 后退");
        sender.sendMessage("§e/edge forward <id> §7- 前进");
        sender.sendMessage("§e/edge refresh <id> §7- 刷新页面");
        sender.sendMessage("§e/edge pause <id> §7- 暂停渲染");
        sender.sendMessage("§e/edge resume <id> §7- 恢复渲染");
        sender.sendMessage("§e/edge fps <id> <1-30> §7- 设置帧率");
        sender.sendMessage("§e/edge brightness <id> <-50~50> §7- 设置亮度");
        sender.sendMessage("§e/edge scale <id> <倍率> §7- 缩放页面");
        sender.sendMessage("§e/edge rotate <id> <角度> §7- 旋转画面");
        sender.sendMessage("§e/edge clear <id> §7- 清空画面");
        sender.sendMessage("§e/edge teleport <id> §7- 传送到浏览器");
        sender.sendMessage("§e/edge toggle <id> §7- 开关浏览器");
        sender.sendMessage("§e/edge border <id> <材料> §7- 设置边框");
        sender.sendMessage("§e/edge title <id> <标题> §7- 设置标题");
        sender.sendMessage("§e/edge reload §7- 重载配置");
        sender.sendMessage("§7§m                                  ");
        sender.sendMessage("§7空手左键=点击 | 空手右键=中键");
        sender.sendMessage("§7Shift+空手左键=拖拽 | 滚轮=滚动页面");
        sender.sendMessage(plugin.getConfigManager().getMsg("help-footer"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(Arrays.asList(
                    "create", "remove", "list", "info", "reload", "url", "click",
                    "pause", "resume", "clear", "teleport", "brightness", "scale",
                    "fps", "border", "title", "key", "scroll", "input", "back",
                    "forward", "refresh", "toggle", "rotate", "give", "help"
            ), args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("remove", "info", "url", "click", "pause", "resume", "clear",
                            "teleport", "brightness", "scale", "fps", "border", "title",
                            "key", "scroll", "input", "back", "forward", "refresh", "toggle", "rotate")
                    .contains(sub)) {
                return filterPrefix(getBrowserIds(), args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if ("brightness".equals(sub)) {
                return Arrays.asList("-50", "-25", "0", "5", "10", "25", "50");
            }
            if ("fps".equals(sub)) {
                return Arrays.asList("1", "3", "5", "8", "10", "12", "15", "20", "30");
            }
            if ("scale".equals(sub)) {
                return Arrays.asList("0.25", "0.5", "0.75", "1.0", "1.5", "2.0", "3.0");
            }
            if ("rotate".equals(sub)) {
                return Arrays.asList("0", "90", "180", "270");
            }
            if ("border".equals(sub)) {
                return Arrays.asList("LIGHT_GRAY_STAINED_GLASS", "BLACK_STAINED_GLASS",
                        "AIR", "STONE", "IRON_BLOCK");
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        List<String> result = new ArrayList<>();
        for (String s : list) {
            if (s.toLowerCase().startsWith(lower)) result.add(s);
        }
        return result;
    }

    private List<String> getBrowserIds() {
        List<String> ids = new ArrayList<>();
        for (BrowserInstance b : plugin.getBrowserManager().getAllBrowsers()) {
            ids.add(b.getId());
        }
        return ids;
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
