1. 项目概述
应用名称：企业知识库助手

核心功能：通过集成Dify提供的API，实现企业内部知识库的智能问答功能

2. 功能需求
2.1 核心模块
<CLAUDE-CODE>
// 模块标识：Dify_API_Integration
// 技术要点：Retrofit2 + OkHttp3
- 实现Dify API的认证鉴权（API Key方式）
- 支持流式响应(SSE)和普通响应两种模式
- 处理知识库返回的引用来源显示
- 实现聊天消息的持久化存储，确保聊天记录能够保存到数据库并在历史记录中显示。

2.2 用户界面

2.2.1 核心页面需求

1） 主问答界面（ChatActivity）
顶部标题栏

显示“企业知识助手”标题
右侧菜单：历史记录、知识库搜索入口
支持深浅色主题切换

2）消息列表（RecyclerView）

AI 回复与用户提问采用不同气泡样式（AI：浅蓝背景，用户：浅灰背景）
长按消息可复制/转发
知识来源标注（如回答基于某份文档，需显示可点击的“来源”标签）
输入区


3）文本输入框：支持自动补全（匹配知识库常见问题）
附件上传（支持图片、PDF、Word）
语音输入（集成 ASR 语音识别）
发送按钮（FAB 浮动按钮）

2.2.2 知识库管理需求

1） 知识库导航页（KnowledgeNavFragment）
搜索栏
支持实时检索（匹配标题、内容、标签）

2）分类筛选

按部门（人事/财务/技术）
按文档类型（政策/流程/FAQ）

3）文档卡片列表

显示文档图标（PDF/Word/Excel）
显示标题、最后更新时间、用户评分（1-5星）
点击进入文档详情页（支持高亮搜索关键词）

2.2.3 交互与动效需求

1） AI 回复流式显示
文字逐字输出（模拟打字效果，30ms/字）
加载动画（脉冲效果，直至 AI 回复完成）

2.2.4 知识来源交互
点击“来源”标签 → 弹出底部弹窗（BottomSheetDialog）显示：
原文段落（高亮匹配部分）
文档信息（部门、更新时间）
“查看完整文档”按钮

2.2.5 多模态输入
图片上传 → 自动 OCR 识别并填充至输入框
文件上传 → 解析文本摘要供用户确认
四、UI/UX 规范

2.2.6 视觉风格

1） 颜色

主色 #4285F4（AI 气泡）
强调色 #34A853（知识来源标签）
警告色 #EA4335（错误提示）

2） 字体

消息正文：Roboto 14sp
知识来源：Roboto Mono 12sp（等宽字体）

3） 动效

页面转场：300ms 淡入淡出
按钮点击：100ms 波纹反馈

2.2.7、性能与适配需求

1） 性能要求
响应时间

普通问答 ≤1.5s（本地缓存优先）
知识库检索 ≤3s（需优化索引）
内存管理

在 1GB 内存设备上不出现 OOM
自动清理 7 天前的聊天记录

2.2.8 设备适配
设备类型	适配方案
折叠屏	双栏布局（消息列表 + 知识库导航）
平板	优化 FAB 位置，避免遮挡
小屏手机	隐藏非必要图标文字

2.2.9、验收标准
核心功能

AI 问答 + 知识库检索正常
多模态输入（文字/语音/图片/文件）可用
交互体验

流式回复无卡顿
知识来源点击延迟 ≤200ms
性能指标

冷启动时间 ≤1.2s
内存占用 ≤500MB（后台驻留时）

2.2.10 交付物：

Figma/Zeplin 设计稿链接
交互动效演示视频（Lottie 或 MP4）
性能测试报告（Android Profiler 数据）


2.3 安全需求
<CLAUDE-CODE>
// 模块标识：Security
- API Key加密存储（Android Keystore）
- 网络通信必须使用HTTPS
- 敏感数据不落盘

3. API接口规范

3.1 Dify集成参数

curl -X POST 'http://123.60.144.244/v1/chat-messages' \
--header 'Authorization: Bearer {api_key}' \
--header 'Content-Type: application/json' \
--data-raw '{
    "inputs": {},
    "query": "What is webpos?",
    "response_mode": "streaming",
    "conversation_id": "",
    "user": "wiki_chat_app",
    "files": [
      {
        "type": "image",
        "transfer_method": "remote_url",
        "url": "https://cloud.dify.ai/logo/logo-site.png"
      }
    ]
}'

API key需要做成环境变量

4. 开发规范
<CLAUDE-CODE>
// 模块标识：Development_Guidelines
- 架构模式：MVVM + Clean Architecture
- 网络层：Retrofit + Kotlin Coroutines
- 状态管理：ViewModel + StateFlow
- DI框架：Hilt

5. 测试用例模板
<CLAUDE-CODE>
// 模块标识：Test_Cases
1. API连接测试：
   - 验证401未授权情况处理
   - 测试流式响应解析
2. 知识库验证：
   - 提问包含"[需要引用]"的问题
   - 验证返回的文献来源格式

6. 部署要求
最低Android版本：API 26（Android 8.0）
必须适配的屏幕比例：16:9, 19.5:9
依赖服务：Dify API 2.0+