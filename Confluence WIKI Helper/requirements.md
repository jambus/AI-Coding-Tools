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
2.2 用户界面
<CLAUDE-CODE>
// 模块标识：UI_Components
// 技术要点：Jetpack Compose
1. 主问答界面：
   - 消息列表（支持文本/引用卡片）
   - 底部输入栏（带发送按钮）
   
2. 附加功能：
   - 历史会话保存（Room Database）
   - 常用问题快捷入口
2.3 安全需求
<CLAUDE-CODE>
// 模块标识：Security
- API Key加密存储（Android Keystore）
- 网络通信必须使用HTTPS
- 敏感数据不落盘
3. API接口规范
3.1 Dify集成参数
<JSON>
{
  "base_url": "https://api.dify.ai/v1",
  "endpoints": {
    "completion": "/completion-messages",
    "chat": "/chat-messages"
  },
  "headers": {
    "Authorization": "Bearer {api_key}",
    "Content-Type": "application/json"
  }
}
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