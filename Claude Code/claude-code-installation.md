# Claude Code 本地安装指南

## 系统要求
- macOS 10.15+ 或 Windows 10+
- Node.js 18+ 
- Git

## 国内用户替代方案：使用 Kimi K2

由于 Claude Code 需要 Anthropic API，国内用户可以使用 Kimi K2 作为替代方案。

### Kimi K2 安装步骤

reference: [在 software agents 中使用 kimi k2 模型]( https://platform.moonshot.cn/docs/guide/agent-support#%E8%8E%B7%E5%8F%96-api-key)


#### 1. 获取 API 密钥
1. 访问 [kimi.moonshot.cn](https://kimi.moonshot.cn)
2. 注册账号并登录
3. 进入 [API 密钥管理页面](https://platform.moonshot.cn/console/api-keys)
4. 创建新的 API 密钥

#### 2. Claude Code vs Kimi K2 对比

| 功能 | Claude Code | Kimi K2 |
|------|-------------|---------|
| API 访问 | 需海外网络 | 国内可访问 |
| 价格 | $20/月 | 免费额度 + 按量付费 |
| 模型 | Claude 3.5 Sonnet | Kimi K2 |
| 集成度 | Git/IDE 深度集成 | 基础 CLI 工具 |

#### 3. MacOS 和 Linux 安装

MacOS 和 Linux 上安装 nodejs
curl -fsSL https://fnm.vercel.app/install | bash
 
新开一个terminal，让 fnm 生效

```bash
fnm install 24.3.0
fnm default 24.3.0
fnm use 24.3.0
``` 
 
安装 claude-code

```bash
npm install -g @anthropic-ai/claude-code --registry=https://registry.npmmirror.com
``` 

初始化配置

```bash
node --eval "
    const homeDir = os.homedir(); 
    const filePath = path.join(homeDir, '.claude.json');
    if (fs.existsSync(filePath)) {
        const content = JSON.parse(fs.readFileSync(filePath, 'utf-8'));
        fs.writeFileSync(filePath,JSON.stringify({ ...content, hasCompletedOnboarding: true }, 2), 'utf-8');
    } else {
        fs.writeFileSync(filePath,JSON.stringify({ hasCompletedOnboarding: true }), 'utf-8');
    }"
```

#### 4. 验证安装
```bash
claude --version
claude --help
```

#### 5. 配置环境变量

Linux/macOS 启动高速版 kimi-k2-turbo-preview 模型

```bash
export ANTHROPIC_BASE_URL=https://api.moonshot.cn/anthropic
export ANTHROPIC_AUTH_TOKEN=${YOUR_MOONSHOT_API_KEY}
export ANTHROPIC_MODEL=kimi-k2-turbo-preview
export ANTHROPIC_SMALL_FAST_MODEL=kimi-k2-turbo-preview
claude
```

## 基本使用

### 启动交互模式
```bash
claude
```

### 在当前项目中使用
```bash
claude /your/project/path
```

### 常用命令
- `claude /help` - 显示帮助
- `claude /init` - 初始化项目分析
- `claude /git-status` - 查看 git 状态

## 配置文件

配置文件位于：
- macOS: `~/.claude/config.json`
- Windows: `%USERPROFILE%\.claude\config.json`

示例配置：
```json
{
  "apiKey": "your-api-key-here",
  "model": "claude-3-5-sonnet-20241022",
  "maxTokens": 4000,
  "temperature": 0.2
}
```

## 故障排除

### 常见问题
1. **权限错误**: 使用 `sudo npm install -g @anthropic-ai/claude-code`
2. **网络问题**: 检查代理设置或 VPN
3. **Node 版本**: 确保 Node.js 18+

### 日志位置
- macOS: `~/.claude/logs/`
- Windows: `%USERPROFILE%\.claude\logs\`

## 更新 Claude Code
```bash
npm update -g @anthropic-ai/claude-code
```

## 卸载
```bash
npm uninstall -g @anthropic-ai/claude-code
rm -rf ~/.claude  # 同时删除配置和缓存