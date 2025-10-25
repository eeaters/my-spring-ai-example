package io.eeaters.statemachine.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.eeaters.statemachine.controller.EmailCoordinatorController.CreateTaskRequest;
import io.eeaters.statemachine.controller.EmailCoordinatorController.TriggerEventRequest;
import io.eeaters.statemachine.state.StateMachineService;
import io.eeaters.statemachine.service.TaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 邮件协调器控制器测试
 *
 * 测试REST API接口的功能
 *
 * @author eeaters
 * @version 1.0.0
 */
@WebMvcTest(EmailCoordinatorController.class)
class EmailCoordinatorControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private StateMachineService stateMachineService;

	@MockBean
	private TaskService taskService;

	@Test
	void testCreateTaskSuccess() throws Exception {
		// 测试创建任务成功
		String taskId = "test-task-001";
		String stateMachineId = "sm-001";

		when(stateMachineService.createStateMachine(taskId)).thenReturn(stateMachineId);
		when(stateMachineService.startEmailTask(eq(stateMachineId), anyString(), anyString(), anyString(), anyString(),
				anyString()))
			.thenReturn(true);

		CreateTaskRequest request = new CreateTaskRequest();
		request.setTaskId(taskId);
		request.setTitle("测试任务");
		request.setDescription("测试描述");
		request.setTargetEmail("test@example.com");
		request.setSubject("测试主题");
		request.setBody("测试内容");

		mockMvc
			.perform(post("/api/email-coordinator/tasks").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.stateMachineId").value(stateMachineId))
			.andExpect(jsonPath("$.taskId").value(taskId))
			.andExpect(jsonPath("$.message").value("邮件协调任务创建成功"));
	}

	@Test
	void testCreateTaskFailure() throws Exception {
		// 测试创建任务失败
		String taskId = "test-task-002";
		String stateMachineId = "sm-002";

		when(stateMachineService.createStateMachine(taskId)).thenReturn(stateMachineId);
		when(stateMachineService.startEmailTask(eq(stateMachineId), anyString(), anyString(), anyString(), anyString(),
				anyString()))
			.thenReturn(false);

		CreateTaskRequest request = new CreateTaskRequest();
		request.setTaskId(taskId);
		request.setTitle("失败测试任务");
		request.setTargetEmail("fail@example.com");

		mockMvc
			.perform(post("/api/email-coordinator/tasks").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.message").value("邮件协调任务创建失败"));
	}

	@Test
	void testGetTaskStatus() throws Exception {
		// 测试获取任务状态
		String taskId = "test-task-003";
		String stateMachineId = "sm-003";

		Map<String, Object> summary = new HashMap<>();
		summary.put("taskId", taskId);
		summary.put("currentState", "POLLING");
		summary.put("pollCount", 5);
		summary.put("sentEmailCount", 1);

		when(stateMachineService.findStateMachineIdByTaskId(taskId)).thenReturn(stateMachineId);
		when(stateMachineService.getStateMachineSummary(stateMachineId)).thenReturn(summary);

		mockMvc.perform(get("/api/email-coordinator/tasks/{taskId}/status", taskId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.taskId").value(taskId))
			.andExpect(jsonPath("$.currentState").value("POLLING"))
			.andExpect(jsonPath("$.pollCount").value(5))
			.andExpect(jsonPath("$.sentEmailCount").value(1));
	}

	@Test
	void testGetTaskStatusNotFound() throws Exception {
		// 测试获取不存在的任务状态
		String taskId = "non-existent-task";

		when(stateMachineService.findStateMachineIdByTaskId(taskId)).thenReturn(null);

		mockMvc.perform(get("/api/email-coordinator/tasks/{taskId}/status", taskId)).andExpect(status().isNotFound());
	}

	@Test
	void testGetActiveTasks() throws Exception {
		// 测试获取活跃任务列表
		Map<String, Object> task1 = new HashMap<>();
		task1.put("taskId", "task-001");
		task1.put("currentState", "POLLING");

		Map<String, Object> task2 = new HashMap<>();
		task2.put("taskId", "task-002");
		task2.put("currentState", "EMAIL_SENT");

		List<String> activeStateMachineIds = List.of("sm-001", "sm-002");
		when(stateMachineService.getActiveStateMachineIds()).thenReturn(activeStateMachineIds);
		when(stateMachineService.getStateMachineSummary("sm-001")).thenReturn(task1);
		when(stateMachineService.getStateMachineSummary("sm-002")).thenReturn(task2);

		mockMvc.perform(get("/api/email-coordinator/tasks/active"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$").isArray())
			.andExpect(jsonPath("$[0].taskId").value("task-001"))
			.andExpect(jsonPath("$[1].taskId").value("task-002"));
	}

	@Test
	void testTriggerEventSuccess() throws Exception {
		// 测试触发事件成功
		String taskId = "test-task-004";
		String stateMachineId = "sm-004";
		String event = "SEND_EMAIL";

		when(stateMachineService.findStateMachineIdByTaskId(taskId)).thenReturn(stateMachineId);
		when(stateMachineService.sendEvent(stateMachineId, io.eeaters.statemachine.state.Events.SEND_EMAIL))
			.thenReturn(true);

		TriggerEventRequest request = new TriggerEventRequest();
		request.setEvent(event);

		mockMvc
			.perform(
					post("/api/email-coordinator/tasks/{taskId}/events", taskId).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.taskId").value(taskId))
			.andExpect(jsonPath("$.event").value(event))
			.andExpect(jsonPath("$.message").value("事件触发成功"));
	}

	@Test
	void testTriggerEventFailure() throws Exception {
		// 测试触发事件失败
		String taskId = "test-task-005";
		String stateMachineId = "sm-005";
		String event = "INVALID_EVENT";

		when(stateMachineService.findStateMachineIdByTaskId(taskId)).thenReturn(stateMachineId);
		when(stateMachineService.sendEvent(stateMachineId, io.eeaters.statemachine.state.Events.CREATE_TASK))
			.thenReturn(false);

		TriggerEventRequest request = new TriggerEventRequest();
		request.setEvent(event);

		mockMvc
			.perform(
					post("/api/email-coordinator/tasks/{taskId}/events", taskId).contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.message").value("事件触发失败"));
	}

	@Test
	void testResetTaskSuccess() throws Exception {
		// 测试重置任务成功
		String taskId = "test-task-006";
		String stateMachineId = "sm-006";

		when(stateMachineService.findStateMachineIdByTaskId(taskId)).thenReturn(stateMachineId);
		when(stateMachineService.resetStateMachine(stateMachineId)).thenReturn(true);

		mockMvc.perform(post("/api/email-coordinator/tasks/{taskId}/reset", taskId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.taskId").value(taskId))
			.andExpect(jsonPath("$.message").value("任务重置成功"));
	}

	@Test
	void testCancelTaskSuccess() throws Exception {
		// 测试取消任务成功
		String taskId = "test-task-007";
		String stateMachineId = "sm-007";

		when(stateMachineService.findStateMachineIdByTaskId(taskId)).thenReturn(stateMachineId);
		when(stateMachineService.destroyStateMachine(stateMachineId)).thenReturn(true);

		mockMvc.perform(delete("/api/email-coordinator/tasks/{taskId}", taskId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.taskId").value(taskId))
			.andExpect(jsonPath("$.message").value("任务取消成功"));
	}

	@Test
	void testGetStatistics() throws Exception {
		// 测试获取统计信息
		TaskService.TaskStatistics stats = new TaskService.TaskStatistics(10, 7, 2, 1);

		when(taskService.getTaskStatistics()).thenReturn(stats);
		when(stateMachineService.getActiveStateMachineCount()).thenReturn(1);

		mockMvc.perform(get("/api/email-coordinator/statistics"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.activeStateMachines").value(1))
			.andExpect(jsonPath("$.totalTasks").value(10))
			.andExpect(jsonPath("$.completedTasks").value(7))
			.andExpect(jsonPath("$.failedTasks").value(2))
			.andExpect(jsonPath("$.runningTasks").value(1))
			.andExpect(jsonPath("$.successRate").value(0.7));
	}

	@Test
	void testHealthCheck() throws Exception {
		// 测试健康检查
		when(stateMachineService.getActiveStateMachineCount()).thenReturn(3);

		mockMvc.perform(get("/api/email-coordinator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"))
			.andExpect(jsonPath("$.activeStateMachines").value(3))
			.andExpect(jsonPath("$.timestamp").exists());
	}

}