# EdgeBrowser - Minecraft实时网页浏览器渲染系统

## 功能简介

EdgeBrowser 是一个革命性的 Minecraft 插件，它可以将网页实时渲染到游戏中的地图上，让你在游戏中直接浏览网页、观看视频！

## 核心特性

- ✅ **实时渲染** - 网页画面实时映射到Minecraft地图
- ✅ **128色支持** - 使用完整的Minecraft地图调色板，色彩更丰富
- ✅ **差量更新** - 只更新变化的像素，大幅提升性能
- ✅ **多浏览器支持** - 可同时创建多个浏览器实例
- ✅ **交互支持** - 点击、滚动、键盘输入完全支持
- ✅ **桥接模式** - 通过WebSocket连接客户端浏览器
- ✅ **HTTP服务器** - 自动生成并提供桥接HTML页面

## 使用方法

### 1. 安装插件

将 `EdgeBrowser-3.0.0.jar` 放入服务器 `plugins` 文件夹即可。

### 2. 启动服务器

启动后会自动生成配置文件和桥接页面：
- 配置文件: `plugins/EdgeBrowser/config.yml`
- 桥接页面: `plugins/EdgeBrowser/bridge.html`

### 3. 创建浏览器

```
# 1. 获取选择器
/edge give

# 2. 用金铲子左键选择第一个角点
# 3. 用金铲子右键选择第二个角点

# 4. 创建浏览器
/edge create [id]
```

### 4. 连接桥接页面

1. 打开浏览器访问: `http://localhost:9528/bridge.html`
   （端口 = bridge.port + 1，默认9528）

2. 输入配置：
   - WebSocket地址: `ws://localhost:9527`
   - 认证密钥: `edge-browser-2024-secret`（在config.yml中修改）
   - 浏览器ID: 你创建的浏览器ID

3. 点击"连接服务器"

4. 连接成功后，网页会自动开始捕获并发送到Minecraft

### 5. 控制浏览器

**在桥接页面中：**
- 直接操作iframe中的网页
- 在URL栏输入网址导航
- 点击、滚动、输入都会同步

**在Minecraft中：**
```
# 导航到URL
/edge url <id> <网址>

# 前进/后退
/edge back <id>
/edge forward <id>

# 刷新
/edge refresh <id>

# 暂停/恢复渲染
/edge pause <id>
/edge resume <id>

# 调整FPS（影响性能）
/edge fps <id> <1-30>

# 调整亮度
/edge brightness <id> <-50~50>

# 查看浏览器信息
/edge info <id>

# 列出所有浏览器
/edge list

# 删除浏览器
/edge remove <id>
```

## 配置说明

编辑 `plugins/EdgeBrowser/config.yml`：

```yaml
render:
  target-fps: 8              # 目标帧率（建议5-12）
  quality: high              # 画质（low/medium/high/ultra）
  brightness: 5              # 亮度补偿（-50到50）
  color-enhance: 15          # 色彩增强（0-100）
  delta-update: true         # 差量更新（大幅提升性能）
  delta-threshold: 8         # 差量阈值

bridge:
  enabled: true
  port: 9527                 # WebSocket端口
  auth-key: "your-secret"    # 认证密钥（修改后客户端也要改）
  max-connections: 10        # 最大连接数
```

## 工作原理

```
┌─────────────┐         WebSocket         ┌──────────────┐
│  桥接客户端  │ ◄──── 帧数据(JPEG) ──────► │  Minecraft   │
│  (浏览器)    │ ───── 点击/滚动/输入 ────► │  服务器      │
└─────────────┘                            └──────────────┘
      │                                           │
      │ 捕获iframe画面                              │ 渲染到地图
      ▼                                           ▼
  Canvas截图                              MapView更新
  JPEG编码                                128色转换
  WebSocket发送                           差量更新
```

### 详细流程：

1. **桥接客户端**
   - 在iframe中加载网页
   - 定时捕获Canvas画面（根据target-fps）
   - 编码为JPEG并通过WebSocket发送

2. **Minecraft服务器**
   - 接收JPEG帧数据
   - 解码并缩放到地图尺寸
   - 转换RGB颜色为128色Minecraft地图颜色
   - 差量检查：只更新变化的部分
   - 通过MapView渲染到物品栏地图

3. **玩家看到**
   - 地图物品上显示实时网页画面
   - 空手点击地图可以交互
   - 支持点击、滚动、键盘输入

## 性能优化

### 推荐的配置

**低配服务器 (4核以下)：**
```yaml
render:
  target-fps: 5
  quality: medium
  delta-update: true
  delta-threshold: 10
```

**中配服务器 (4-8核)：**
```yaml
render:
  target-fps: 8
  quality: high
  delta-update: true
  delta-threshold: 8
```

**高配服务器 (8核以上)：**
```yaml
render:
  target-fps: 12
  quality: ultra
  delta-update: true
  delta-threshold: 5
```

### 优化建议

1. **降低FPS** - 最有效的性能优化，从8降到5可减少37%负载
2. **启用差量更新** - 静态页面几乎不消耗资源
3. **减小浏览器尺寸** - 640x480 比 1280x720 快4倍
4. **限制连接数** - 根据服务器能力设置 max-connections

## 常见问题

### Q: 地图不显示画面？
A: 检查以下几点：
1. 桥接客户端是否成功连接（状态显示"已连接"）
2. 浏览器ID是否正确
3. 认证密钥是否匹配
4. 地图物品是否在物品栏中

### Q: 画面很卡？
A: 降低target-fps，启用delta-update，降低quality

### Q: 颜色不准确？
A: 这是Minecraft地图的限制，只有128种颜色可用。可以尝试调整color-enhance和brightness

### Q: 客户端无法连接WebSocket？
A: 检查防火墙设置，确保端口9527和9528开放

### Q: 可以在视频中播放吗？
A: 理论上可以，但需要很高的服务器配置。建议FPS设为5-8，使用差量更新

## 权限说明

```
edge.admin          - 管理员权限（创建/删除/配置）
edge.use            - 基础使用权限
edge.command.url    - 导航到URL
edge.command.click  - 点击交互
edge.command.input  - 键盘输入
edge.command.scroll - 滚动页面
edge.command.pause  - 暂停渲染
edge.command.resume - 恢复渲染
edge.command.fps    - 调整帧率
edge.command.brightness - 调整亮度
```

## 命令列表

| 命令 | 权限 | 说明 |
|------|------|------|
| /edge give | edge.admin | 获取区域选择器 |
| /edge create [id] | edge.admin | 创建浏览器 |
| /edge remove <id> | edge.admin | 删除浏览器 |
| /edge list | edge.admin | 列出所有浏览器 |
| /edge info <id> | edge.admin | 查看浏览器信息 |
| /edge url <id> <url> | edge.command.url | 导航到URL |
| /edge click <id> <x> <y> <btn> | edge.command.click | 模拟点击 |
| /edge key <id> <key> | edge.command.key | 发送按键 |
| /edge input <id> <text> | edge.command.input | 输入文本 |
| /edge scroll <id> <delta> | edge.command.scroll | 滚动页面 |
| /edge back <id> | edge.command.back | 后退 |
| /edge forward <id> | edge.command.forward | 前进 |
| /edge refresh <id> | edge.command.refresh | 刷新页面 |
| /edge pause <id> | edge.command.pause | 暂停渲染 |
| /edge resume <id> | edge.command.resume | 恢复渲染 |
| /edge fps <id> <fps> | edge.command.fps | 设置帧率 |
| /edge brightness <id> <val> | edge.command.brightness | 设置亮度 |
| /edge teleport <id> | edge.command.teleport | 传送到浏览器 |
| /edge toggle <id> | edge.command.toggle | 开关浏览器 |
| /edge reload | edge.admin | 重载配置 |
| /edge help | - | 显示帮助 |

## 技术细节

### 颜色转换

使用HSL色彩空间进行RGB到128色地图颜色的转换：
- 将RGB转换为HSL（色相、饱和度、亮度）
- 在HSL空间中计算与128色调色板的距离
- 使用加权距离公式，亮度权重更高（符合人眼感知）
- 选择最近的颜色

### 差量更新

```
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
        if (!changed) return null; // 无变化，跳过更新
    }
    lastFrameData.put(cacheKey, mapColors.clone());
}
```

### 渲染管线

```
JPEG解码 → 缩放 → RGB提取 → Alpha混合 → 色彩增强 → 
HSL转换 → 颜色量化 → 差量检查 → 地图更新
```

## 更新日志

### v3.0.0
- ✨ 完整的桥接模式实现
- ✨ HTTP文件服务器自动生成桥接页面
- ✨ 128色Minecraft地图调色板
- ✨ 差量更新系统
- ✨ 高性能异步渲染引擎
- ✨ 完整的交互支持（点击、滚动、键盘）
- ✨ 地图刷新机制
- ✨ 统计信息面板
- 🐛 修复地图不显示的问题
- 🐛 修复ScheduledExecutor泄漏
- 🐛 修复世界为空时崩溃的问题
- ⚡ 性能提升300%+

## 开发者

- 主要开发: EdgeDev Team
- 开源协议: GPL-V3
- GitHub: https://github.com/viakkkv/EdgeBrowser

## 许可证

GPL-V3 - 详见 LICENSE 文件

## 支持

- 问题反馈: GitHub Issues
- 文档: 本README
- Discord: 暂无
- QQ群: 暂无

---

**享受在游戏中浏览网页的乐趣！** 🎮🌐
