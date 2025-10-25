package io.eeaters.statemachine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Email Coordinator State Machine Application
 *
 * 基于Spring State Machine的邮件协调机器人主启动类 实现自动邮件沟通流程，包含任务创建、邮件发送、轮询响应、处理回复等功能
 *
 * @author eeaters
 * @version 1.0.0
 */
@SpringBootApplication
public class EmailCoordinatorApplication {

	public static void main(String[] args) {
		SpringApplication.run(EmailCoordinatorApplication.class, args);
	}

}