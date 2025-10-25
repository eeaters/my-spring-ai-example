package io.eeaters.statemachine.state;

import io.eeaters.statemachine.config.StateMachineConfig;
import io.eeaters.statemachine.model.CoordinatorContext;
import io.eeaters.statemachine.model.EmailTask;
import io.eeaters.statemachine.service.TaskService;
import io.eeaters.statemachine.agent.CoordinatorAgent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 状态机配置测试
 *
 * 测试状态机的配置、转换和Guard功能
 *
 * @author eeaters
 * @version 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
class StateMachineConfigTest {

	@Autowired
	private StateMachineFactory<States, Events> stateMachineFactory;

	@Autowired
	private TaskService taskService;

	@Autowired
	private CoordinatorAgent coordinatorAgent;

	@Test
	void testStateMachineFactory() {
		// 测试状态机工厂
		StateMachine<States, Events> stateMachine = stateMachineFactory.getStateMachine();

		assertNotNull(stateMachine);
		assertEquals(States.IDLE, stateMachine.getState().getId());
	}

	@Test
	void testStateMachineStartStop() {
		// 测试状态机启动和停止
		StateMachine<States, Events> stateMachine = stateMachineFactory.getStateMachine();

		stateMachine.start();
		assertTrue(stateMachine.getState() != null);

		stateMachine.stop();
	}

	@Test
	void testCreateTaskTransition() {
		// 测试创建任务状态转换
		StateMachine<States, Events> stateMachine = stateMachineFactory.getStateMachine();
		stateMachine.start();

		// 准备任务数据
		EmailTask task = taskService.createTask("test-transition", "转换测试", "测试状态转换", "test@example.com", "测试主题",
				"测试内容");

		CoordinatorContext context = coordinatorAgent.createCoordinatorContext(task.getTaskId());
		stateMachine.getExtendedState().getVariables().put("coordinatorContext", context);

		// 发送创建任务事件
		boolean eventSent = stateMachine.sendEvent(Events.CREATE_TASK);

		assertTrue(eventSent);
		assertEquals(States.TASK_CREATED, stateMachine.getState().getId());

		stateMachine.stop();
	}

	@Test
	void testGuardConditions() {
		// 测试Guard条件
		StateMachine<States, Events> stateMachine = stateMachineFactory.getStateMachine();
		stateMachine.start();

		// 创建任务和上下文
		EmailTask task = taskService.createTask("test-guard", "Guard测试", "测试Guard条件", "test@example.com", "Guard测试主题",
				"Guard测试内容");

		CoordinatorContext context = coordinatorAgent.createCoordinatorContext(task.getTaskId());
		stateMachine.getExtendedState().getVariables().put("coordinatorContext", context);

		// 执行到RESPONSE_RECEIVED状态
		stateMachine.sendEvent(Events.CREATE_TASK);
		assertEquals(States.TASK_CREATED, stateMachine.getState().getId());

		stateMachine.sendEvent(Events.SEND_EMAIL);
		assertEquals(States.EMAIL_SENT, stateMachine.getState().getId());

		stateMachine.sendEvent(Events.START_POLLING);
		assertEquals(States.POLLING, stateMachine.getState().getId());

		// 模拟收到响应
		stateMachine.sendEvent(Events.RECEIVE_RESPONSE);
		assertEquals(States.RESPONSE_RECEIVED, stateMachine.getState().getId());

		stateMachine.stop();
	}

	@Test
	void testExtendedState() {
		// 测试扩展状态
		StateMachine<States, Events> stateMachine = stateMachineFactory.getStateMachine();
		stateMachine.start();

		// 设置扩展状态变量
		stateMachine.getExtendedState().getVariables().put("testVariable", "testValue");
		stateMachine.getExtendedState().getVariables().put("testNumber", 42);

		assertEquals("testValue", stateMachine.getExtendedState().getVariables().get("testVariable"));
		assertEquals(42, stateMachine.getExtendedState().getVariables().get("testNumber"));

		stateMachine.stop();
	}

	@Test
	void testStateMachineReset() {
		// 测试状态机重置
		StateMachine<States, Events> stateMachine = stateMachineFactory.getStateMachine();
		stateMachine.start();

		// 执行一些转换
		EmailTask task = taskService.createTask("test-reset", "重置测试", "测试状态机重置", "reset@example.com", "重置测试主题",
				"重置测试内容");

		CoordinatorContext context = coordinatorAgent.createCoordinatorContext(task.getTaskId());
		stateMachine.getExtendedState().getVariables().put("coordinatorContext", context);

		stateMachine.sendEvent(Events.CREATE_TASK);
		assertNotEquals(States.IDLE, stateMachine.getState().getId());

		// 重置状态机
		stateMachine.stop();
		stateMachine.setState(new DefaultStateMachineContext<>(States.IDLE, null, null, null));
		stateMachine.start();

		assertEquals(States.IDLE, stateMachine.getState().getId());
	}

}