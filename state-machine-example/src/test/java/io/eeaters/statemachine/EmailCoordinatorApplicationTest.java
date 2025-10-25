package io.eeaters.statemachine;

import io.eeaters.statemachine.config.EmailConfig;
import io.eeaters.statemachine.service.EmailService;
import io.eeaters.statemachine.service.TaskService;
import io.eeaters.statemachine.state.StateMachineService;
import io.eeaters.statemachine.state.States;
import io.eeaters.statemachine.agent.CoordinatorAgent;
import io.eeaters.statemachine.agent.ResponseAnalyzerAgent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 邮件协调器应用集成测试
 *
 * 测试整个应用的启动和基本功能
 *
 * @author eeaters
 * @version 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
class EmailCoordinatorApplicationTest {

	@Autowired
	private StateMachineService stateMachineService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private EmailService emailService;

	@Autowired
	private CoordinatorAgent coordinatorAgent;

	@Autowired
	private ResponseAnalyzerAgent responseAnalyzerAgent;

	@Autowired
	private EmailConfig emailConfig;

	@Test
	void contextLoads() {
		// 测试Spring上下文是否正常加载
		assertNotNull(stateMachineService);
		assertNotNull(taskService);
		assertNotNull(emailService);
		assertNotNull(coordinatorAgent);
		assertNotNull(responseAnalyzerAgent);
		assertNotNull(emailConfig);
	}

	@Test
	void testStateMachineCreation() {
		// 测试状态机创建
		String taskId = "test-task-001";
		String stateMachineId = stateMachineService.createStateMachine(taskId);

		assertNotNull(stateMachineId);
		assertEquals(1, stateMachineService.getActiveStateMachineCount());

		// 清理
		stateMachineService.destroyStateMachine(stateMachineId);
	}

	@Test
	void testEmailTaskCreation() {
		// 测试邮件任务创建
		String taskId = "test-task-002";
		String title = "测试任务";
		String description = "这是一个测试任务";
		String targetEmail = "test@example.com";
		String subject = "测试邮件";
		String body = "这是测试邮件内容";

		var task = taskService.createTask(title, description, targetEmail, subject, body);

		assertNotNull(task);
		assertEquals(taskId, task.getTaskId());
		assertEquals(title, task.getTitle());
		assertEquals(targetEmail, task.getTargetEmail());
		assertEquals(subject, task.getInitialSubject());
		assertEquals(body, task.getInitialBody());
		assertNotNull(task.getCreatedAt());
	}

	@Test
	void testCoordinatorContextCreation() {
		// 测试协调上下文创建
		String taskId = "test-task-003";
		String title = "协调上下文测试";
		String targetEmail = "coordinator@example.com";

		var task = taskService.createTask(title, "测试描述", targetEmail, "测试主题", "测试内容");
		var context = coordinatorAgent.createCoordinatorContext(task.getTaskId());

		assertNotNull(context);
		assertEquals(task.getTaskId(), context.getCurrentTaskId());
		assertEquals(task, context.getCurrentTask());
		assertNotNull(context.getStateMachineId());
		assertNotNull(context.getStartTime());
	}

	@Test
	void testResponseAnalysis() {
		// 测试响应分析
		String taskId = "test-task-004";
		String subject = "Re: 协调任务邀请";
		String content = "我同意参加这个协调任务，请安排具体时间。";
		String fromEmail = "user@example.com";
		String toEmail = "system@example.com";

		var response = responseAnalyzerAgent.analyzeResponse(taskId, subject, content, fromEmail, toEmail);

		assertNotNull(response);
		assertEquals(taskId, response.getTaskId());
		assertEquals(subject, response.getSubject());
		assertEquals(content, response.getContent());
		assertNotNull(response.getResponseType());
		assertTrue(response.getConfidence() >= 0.0 && response.getConfidence() <= 1.0);
	}

	@Test
	void testStateMachineFullFlow() {
		// 测试完整的状态机流程
		String taskId = "test-full-flow";
		String stateMachineId = stateMachineService.createStateMachine(taskId);

		assertNotNull(stateMachineId);
		assertEquals(States.IDLE, stateMachineService.getCurrentState(stateMachineId));

		// 启动任务
		boolean started = stateMachineService.startEmailTask(stateMachineId, "完整流程测试", "测试完整的状态机流程", "test@example.com",
				"流程测试邮件", "这是完整流程测试邮件");

		assertTrue(started);

		// 验证状态转换
		States currentState = stateMachineService.getCurrentState(stateMachineId);
		assertNotNull(currentState);
		assertNotEquals(States.IDLE, currentState);

		// 清理
		stateMachineService.destroyStateMachine(stateMachineId);
	}

	@Test
	void testTaskStatistics() {
		// 测试任务统计
		var stats = taskService.getTaskStatistics();

		assertNotNull(stats);
		assertTrue(stats.getTotalTasks() >= 0);
		assertTrue(stats.getCompletedTasks() >= 0);
		assertTrue(stats.getFailedTasks() >= 0);
		assertTrue(stats.getRunningTasks() >= 0);
		assertTrue(stats.getSuccessRate() >= 0.0 && stats.getSuccessRate() <= 1.0);
	}

	@Test
	void testEmailConfig() {
		// 测试邮件配置
		assertNotNull(emailConfig);
		assertNotNull(emailConfig.getPolling());
		assertNotNull(emailConfig.getTemplates());
		assertNotNull(emailConfig.getResponseAnalysis());
		assertNotNull(emailConfig.getTask());

		assertTrue(emailConfig.getPolling().isEnabled());
		assertTrue(emailConfig.getPolling().getInterval() > 0);
		assertTrue(emailConfig.getPolling().getMaxAttempts() > 0);

		assertNotNull(emailConfig.getTemplates().getInitialEmail());
		assertNotNull(emailConfig.getTemplates().getFollowUpEmail());

		assertFalse(emailConfig.getResponseAnalysis().getAgreeKeywords().isEmpty());
		assertFalse(emailConfig.getResponseAnalysis().getDisagreeKeywords().isEmpty());
		assertTrue(emailConfig.getResponseAnalysis().getConfidenceThreshold() >= 0.0);
		assertTrue(emailConfig.getResponseAnalysis().getConfidenceThreshold() <= 1.0);

		assertTrue(emailConfig.getTask().getTimeout() > 0);
		assertTrue(emailConfig.getTask().getRetryCount() >= 0);
	}

}