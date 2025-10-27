# LangGraph Email Workflow Example

这是一个基于 LangGraph4j 的自动邮件沟通工作流示例，实现了拖车公司和仓库之间的协调任务系统。

## 功能特性

- 自动邮件发送和接收
- 基于 LangGraph4j 的状态机工作流
- IMAP 邮件轮询和解析
- 智能时间协商
- 多方协调确认流程

## 快速开始

### 1. 配置邮件服务

编辑 `src/main/resources/application.properties` 文件：

```properties
# 邮件配置
email.host=smtp.gmail.com
email.port=587
email.username=your-email@gmail.com
email.password=your-app-password
email.fromAddress=your-email@gmail.com

# 工作流配置
workflow.trailerCompanyEmail=trailer-company@example.com
workflow.warehouseEmail=warehouse@example.com
```

### 2. 构建和运行

```bash
# 编译项目
./mvnw clean compile

# 运行应用
./mvnw spring-boot:run
```

### 3. 使用交互式界面

应用启动后，可以通过命令行界面：

1. 创建新任务
2. 查看所有任务
3. 查看工作流图
4. 退出应用

## 工作流程

1. **创建任务** - 设置任务标题、时间、地点
2. **发送自动回复** - 向第一方（拖车公司）发送协调邮件
3. **等待回复** - 通过 IMAP 轮询等待邮件回复
4. **处理回复** - 分析回复内容，提取时间信息
5. **发送确认** - 向第二方（仓库）发送确认邮件
6. **最终确认** - 发送最终同意邮件给双方
7. **任务完成** - 结束工作流

## 核心组件

- **TaskWorkflow** - 主工作流引擎
- **EmailService** - 邮件发送和接收服务
- **TaskService** - 任务管理服务
- **ConfigService** - 配置管理服务
- **EmailTemplate** - 邮件模板工具

## 测试

```bash
# 运行单元测试
./mvnw test

# 运行完整构建
./mvnw clean verify
```

## 重要提示

提交代码前必须运行格式化：

```bash
./mvnw spring-javaformat:apply
```

## 技术栈

- Spring Boot 3.5.5
- LangGraph4j 1.7.0
- Java 21
- Maven