# 邮件自动回复系统

基于Spring AI的智能邮件自动回复系统，能够自动接收邮件、分析意图并生成格式化回复。

## 功能特性

- 📧 **自动邮件接收**: 定期检查邮箱中的新邮件
- 🤖 **AI意图识别**: 使用AI分析邮件内容，识别用户意图和需求
- 📝 **格式化回复**: 生成类似客户端回复邮件的专业格式
- 🔄 **邮件线程支持**: 正确设置邮件头，支持邮件客户端的线程显示
- ⚡ **自动化处理**: 支持定时任务，无需人工干预
- 🛡️ **智能过滤**: 自动跳过系统邮件、自动回复等，避免回复循环

## 项目结构

```
src/main/java/io/eeaters/
├── App.java                          # Spring Boot启动类
├── model/                             # 数据模型
│   ├── EmailMessage.java             # 邮件消息模型
│   ├── EmailReply.java               # 邮件回复模型
│   └── IntentAnalysisResult.java     # 意图分析结果模型
├── service/                           # 业务服务
│   ├── EmailReceiver.java            # 邮件接收服务
│   ├── EmailIntentAnalyzer.java      # AI意图分析服务
│   ├── EmailFormatter.java           # 邮件格式化服务
│   ├── EmailSender.java              # 邮件发送服务
│   └── EmailAutoReplyService.java    # 自动回复主服务
├── config/                            # 配置类
│   └── ChatClientConfig.java         # AI聊天客户端配置
└── controller/                        # REST控制器
    └── EmailController.java          # 邮件API控制器
```

## 环境要求

- Java 21+
- Spring Boot 3.5+
- Spring AI 1.1.0-M2
- Maven 3.6+

## 配置说明

在 `application.yml` 中配置以下参数：

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}        # OpenAI API密钥
  mail:
    host: ${MAIL_HOST}                  # 邮件服务器地址
    port: ${MAIL_PORT}                  # 邮件服务器端口
    username: ${MAIL_USERNAME}          # 邮箱用户名
    password: ${MAIL_PASSWORD}          # 邮箱密码或应用密码

email:
  auto-reply:
    enabled: true                       # 是否启用自动回复
    check-interval: 60000               # 检查邮件间隔(毫秒)
    reply:
      from-name: "AI Assistant"         # 回复邮件的发件人名称
      signature: "Best regards,\\nAI Assistant"  # 邮件签名
```

## 环境变量

需要设置以下环境变量：

```bash
export OPENAI_API_KEY="your-openai-api-key"
export MAIL_HOST="imap.gmail.com"              # 对于Gmail
export MAIL_PORT="993"                         # IMAP SSL端口
export MAIL_USERNAME="your-email@gmail.com"
export MAIL_PASSWORD="your-app-password"      # Gmail应用密码
```

### Gmail配置说明

1. 开启两步验证
2. 生成应用密码用于IMAP访问
3. 确保IMAP访问已启用

## 运行方式

### 1. 直接运行

```bash
mvn spring-boot:run
```

### 2. 编译后运行

```bash
mvn clean package
java -jar target/email-example-0.0.1-SNAPSHOT.jar
```

## API接口

### 手动触发邮件处理

```bash
POST /api/email/process
```

### 获取未读邮件

```bash
GET /api/email/unread
```

### 分析邮件意图

```bash
POST /api/email/analyze
Content-Type: application/json

{
    "from": "user@example.com",
    "subject": "技术支持请求",
    "textContent": "我的账户无法登录，请帮助解决。"
}
```

### 自动回复控制

```bash
# 启用自动回复
POST /api/email/auto-reply/enable

# 禁用自动回复
POST /api/email/auto-reply/disable

# 查询自动回复状态
GET /api/email/auto-reply/status
```

## 回复格式示例

系统生成的回复邮件格式如下：

```
感谢您的来信。我们已经收到您关于技术支持的请求，我们的技术团队将会尽快为您解决登录问题。请您稍作等候，我们会在24小时内与您联系。

如果您有紧急需求，请直接联系我们的客服热线：400-xxx-xxxx。

Best regards,
AI Assistant

-----原始邮件-----
发件人: 张三 <zhangsan@example.com>
发送时间: 2024年1月15日 10:30
收件人: support@company.com
主题: 技术支持请求

> 我的账户无法登录，请帮助解决。
>
> 用户名：zhangsan
> 错误信息：密码不正确
```

## 智能特性

### 意图识别
- **问题咨询**: 技术问题、产品询问等
- **请求处理**: 功能请求、服务申请等
- **投诉反馈**: 问题反馈、建议等
- **商务咨询**: 合作洽谈、购买咨询等

### 自动过滤
- 跳过自动回复邮件，避免回复循环
- 过滤系统邮件（noreply、system等）
- 识别营销邮件和垃圾邮件

### 回复策略
- 根据邮件内容调整回复语调（正式、友好、专业）
- 自动判断是否需要人工介入
- 设置回复优先级（高、中、低）

## 注意事项

1. **API密钥安全**: 确保OpenAI API密钥的安全，不要提交到代码仓库
2. **邮箱权限**: 确保邮箱账户有IMAP访问权限
3. **回复频率**: 避免过于频繁的邮件检查，以免触发邮件服务器限制
4. **人工介入**: 对于复杂问题或投诉，系统会标记为需要人工处理
5. **邮件线程**: 系统会正确设置邮件头，确保邮件客户端能正确显示对话线程

## 故障排除

### 常见问题

1. **邮件接收失败**
   - 检查邮箱配置和认证信息
   - 确认IMAP服务已启用
   - 检查网络连接

2. **AI分析失败**
   - 验证OpenAI API密钥
   - 检查API配额和网络连接
   - 查看错误日志

3. **邮件发送失败**
   - 确认SMTP配置正确
   - 检查发件人邮箱权限
   - 验证收件人地址格式

### 日志查看

系统使用SLF4J进行日志记录，可以通过日志查看详细的处理过程和错误信息。