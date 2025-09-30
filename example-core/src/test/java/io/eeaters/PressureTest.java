package io.eeaters;

import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class PressureTest {

    public record Message(String message, Long timestamp) {
    }

    // Reactor模式的简易Demo - 模拟StdioMcpSessionTransport的工作原理
    public static class ReactorTransportDemo {

        // 模拟inboundSink - 接收来自外部(控制台输入)的数据
        private final Sinks.Many<Message> inboundSink = Sinks.many().unicast().onBackpressureBuffer();

        // 模拟outboundSink - 发送数据到外部
        private final Sinks.Many<Message> outboundSink = Sinks.many().unicast().onBackpressureBuffer();

        // 模拟outboundReady - 控制何时可以发送数据
        private final AtomicBoolean outboundReady = new AtomicBoolean(true);

        private final Scanner scanner = new Scanner(System.in);

        public void start() {
            System.out.println("=== Reactor Transport Demo Start ===");
            System.out.println("输入消息发送到系统，输入'quit'退出");

            // 启动输入监听线程 - 模拟从stdin读取数据
            startInputListener();

            // 启动消息处理器 - 模拟处理逻辑
            startMessageProcessor();

            // 启动输出监听器 - 模拟向stdout发送数据
            startOutputListener();
        }

        private void startInputListener() {
            // 在独立线程中监听控制台输入
            Schedulers.boundedElastic().schedule(() -> {
                while (true) {
                    try {
                        System.out.print("请输入消息: ");
                        String input = scanner.nextLine();

                        if ("quit".equalsIgnoreCase(input.trim())) {
                            inboundSink.tryEmitComplete();
                            break;
                        }

                        // 创建消息并发送到inboundSink
                        Message message = new Message(input, System.currentTimeMillis());
                        System.out.println("[INPUT] 接收到消息: " + message);

                        // 这是关键点：将外部输入的数据发射到inbound流中
                        Sinks.EmitResult result = inboundSink.tryEmitNext(message);
                        if (result.isFailure()) {
                            System.err.println("[INPUT] 发送失败: " + result);
                        }

                    } catch (Exception e) {
                        System.err.println("[INPUT] 错误: " + e.getMessage());
                        inboundSink.tryEmitError(e);
                        break;
                    }
                }
            });
        }

        private void startMessageProcessor() {
            // 订阅inboundSink的数据流，处理消息
            inboundSink.asFlux()
                .subscribeOn(Schedulers.parallel())
                .doOnNext(msg -> System.out.println("[PROCESSOR] 开始处理消息: " + msg.message()))
                .map(this::processMessage)
                .subscribe(
                    processedMsg -> {
                        System.out.println("[PROCESSOR] 处理完成: " + processedMsg.message());

                        // 处理完成后，将结果发送到outboundSink
                        if (outboundReady.get()) {
                            Sinks.EmitResult result = outboundSink.tryEmitNext(processedMsg);
                            if (result.isFailure()) {
                                System.err.println("[PROCESSOR] 输出发送失败: " + result);
                            }
                        } else {
                            System.out.println("[PROCESSOR] 输出未就绪，等待...");
                        }
                    },
                    error -> System.err.println("[PROCESSOR] 处理错误: " + error.getMessage()),
                    () -> System.out.println("[PROCESSOR] 处理器结束")
                );
        }

        private void startOutputListener() {
            // 订阅outboundSink的数据流，将数据输出到控制台
            outboundSink.asFlux()
                .subscribeOn(Schedulers.boundedElastic())
                .doOnNext(msg -> System.out.println("[OUTPUT] 准备输出消息: " + msg.message()))
                .delayElements(Duration.ofMillis(500)) // 模拟网络延迟
                .subscribe(
                    msg -> {
                        System.out.println(">>> 输出结果: " + msg.message() +
                                         " (处理时间: " + (System.currentTimeMillis() - msg.timestamp()) + "ms)");

                        // 模拟输出完成后的状态更新
                        outboundReady.set(true);
                    },
                    error -> System.err.println("[OUTPUT] 输出错误: " + error.getMessage()),
                    () -> System.out.println("[OUTPUT] 输出监听器结束")
                );
        }

        private Message processMessage(Message input) {
            // 模拟消息处理逻辑
            try {
                // 模拟处理耗时
                Thread.sleep(200);

                // 简单的处理逻辑：转换为大写并添加处理标记
                String processedContent = "[PROCESSED] " + input.message().toUpperCase();
                return new Message(processedContent, System.currentTimeMillis());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new Message("[ERROR] 处理被中断", System.currentTimeMillis());
            }
        }

        public void stop() {
            System.out.println("=== 停止Reactor Transport Demo ===");
            inboundSink.tryEmitComplete();
            outboundSink.tryEmitComplete();
        }
    }

    public static void main(String[] args) {
        ReactorTransportDemo demo = new ReactorTransportDemo();

        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(demo::stop));

        try {
            demo.start();

            // 主线程等待
            Thread.sleep(Long.MAX_VALUE);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("程序被中断");
        }
    }
}