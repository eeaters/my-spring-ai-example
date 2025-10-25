package io.eeaters.statemachine.controller;

import io.eeaters.statemachine.state.StateMachineService;
import io.eeaters.statemachine.state.States;
import io.eeaters.statemachine.service.TaskService;
import io.eeaters.statemachine.model.CoordinatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 邮件协调器 REST API 控制器
 *
 * 提供HTTP接口来操作邮件协调状态机 支持创建任务、查询状态、获取报告等功能
 *
 * @author eeaters
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/email-coordinator")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EmailCoordinatorController {

	private final StateMachineService stateMachineService;

	private final TaskService taskService;

	/**
	 * 创建并启动邮件协调任务
	 */
	@PostMapping("/tasks")
	public ResponseEntity<Map<String, Object>> createTask(@RequestBody CreateTaskRequest request) {
		log.info("创建邮件协调任务: {}", request);

		try {
			// 创建状态机
			String stateMachineId = stateMachineService.createStateMachine(request.getTaskId());

			// 启动任务
			boolean success = stateMachineService.startEmailTask(stateMachineId, request.getTitle(),
					request.getDescription(), request.getTargetEmail(), request.getSubject(), request.getBody());

			Map<String, Object> response = new HashMap<>();
			response.put("success", success);
			response.put("stateMachineId", stateMachineId);
			response.put("taskId", request.getTaskId());

			if (success) {
				response.put("message", "邮件协调任务创建成功");
				return ResponseEntity.ok(response);
			}
			else {
				response.put("message", "邮件协调任务创建失败");
				return ResponseEntity.badRequest().body(response);
			}

		}
		catch (Exception e) {
			log.error("创建任务失败: {}", e.getMessage(), e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "创建任务失败: " + e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * 获取任务状态
	 */
	@GetMapping("/tasks/{taskId}/status")
	public ResponseEntity<Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
		log.info("查询任务状态: taskId={}", taskId);

		try {
			// 查找状态机ID（简化实现，实际项目中应该维护taskId到stateMachineId的映射）
			String stateMachineId = findStateMachineIdByTaskId(taskId);
			if (stateMachineId == null) {
				return ResponseEntity.notFound().build();
			}

			// 获取状态机摘要
			Map<String, Object> summary = stateMachineService.getStateMachineSummary(stateMachineId);
			if (summary == null) {
				return ResponseEntity.notFound().build();
			}

			return ResponseEntity.ok(summary);

		}
		catch (Exception e) {
			log.error("查询任务状态失败: taskId={}, error={}", taskId, e.getMessage(), e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * 获取所有活跃任务
	 */
	@GetMapping("/tasks/active")
	public ResponseEntity<List<Map<String, Object>>> getActiveTasks() {
		log.info("查询所有活跃任务");

		try {
			List<Map<String, Object>> activeTasks = new ArrayList<>();

			for (String stateMachineId : stateMachineService.getActiveStateMachineIds()) {
				Map<String, Object> summary = stateMachineService.getStateMachineSummary(stateMachineId);
				if (summary != null) {
					activeTasks.add(summary);
				}
			}

			return ResponseEntity.ok(activeTasks);

		}
		catch (Exception e) {
			log.error("查询活跃任务失败: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * 手动触发事件
	 */
	@PostMapping("/tasks/{taskId}/events")
	public ResponseEntity<Map<String, Object>> triggerEvent(@PathVariable String taskId,
			@RequestBody TriggerEventRequest request) {
		log.info("触发事件: taskId={}, event={}", taskId, request.getEvent());

		try {
			String stateMachineId = findStateMachineIdByTaskId(taskId);
			if (stateMachineId == null) {
				return ResponseEntity.notFound().build();
			}

			boolean success = stateMachineService.sendEvent(stateMachineId,
					io.eeaters.statemachine.state.Events.valueOf(request.getEvent().toUpperCase()));

			Map<String, Object> response = new HashMap<>();
			response.put("success", success);
			response.put("taskId", taskId);
			response.put("event", request.getEvent());

			if (success) {
				response.put("message", "事件触发成功");
				return ResponseEntity.ok(response);
			}
			else {
				response.put("message", "事件触发失败");
				return ResponseEntity.badRequest().body(response);
			}

		}
		catch (Exception e) {
			log.error("触发事件失败: taskId={}, event={}, error={}", taskId, request.getEvent(), e.getMessage(), e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "事件触发失败: " + e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * 重置任务
	 */
	@PostMapping("/tasks/{taskId}/reset")
	public ResponseEntity<Map<String, Object>> resetTask(@PathVariable String taskId) {
		log.info("重置任务: taskId={}", taskId);

		try {
			String stateMachineId = findStateMachineIdByTaskId(taskId);
			if (stateMachineId == null) {
				return ResponseEntity.notFound().build();
			}

			boolean success = stateMachineService.resetStateMachine(stateMachineId);

			Map<String, Object> response = new HashMap<>();
			response.put("success", success);
			response.put("taskId", taskId);

			if (success) {
				response.put("message", "任务重置成功");
				return ResponseEntity.ok(response);
			}
			else {
				response.put("message", "任务重置失败");
				return ResponseEntity.badRequest().body(response);
			}

		}
		catch (Exception e) {
			log.error("重置任务失败: taskId={}, error={}", taskId, e.getMessage(), e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "任务重置失败: " + e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * 取消任务
	 */
	@DeleteMapping("/tasks/{taskId}")
	public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String taskId) {
		log.info("取消任务: taskId={}", taskId);

		try {
			String stateMachineId = findStateMachineIdByTaskId(taskId);
			if (stateMachineId == null) {
				return ResponseEntity.notFound().build();
			}

			boolean success = stateMachineService.destroyStateMachine(stateMachineId);

			Map<String, Object> response = new HashMap<>();
			response.put("success", success);
			response.put("taskId", taskId);

			if (success) {
				response.put("message", "任务取消成功");
				return ResponseEntity.ok(response);
			}
			else {
				response.put("message", "任务取消失败");
				return ResponseEntity.badRequest().body(response);
			}

		}
		catch (Exception e) {
			log.error("取消任务失败: taskId={}, error={}", taskId, e.getMessage(), e);
			Map<String, Object> errorResponse = new HashMap<>();
			errorResponse.put("success", false);
			errorResponse.put("message", "任务取消失败: " + e.getMessage());
			return ResponseEntity.internalServerError().body(errorResponse);
		}
	}

	/**
	 * 获取系统统计信息
	 */
	@GetMapping("/statistics")
	public ResponseEntity<Map<String, Object>> getStatistics() {
		log.info("查询系统统计信息");

		try {
			TaskService.TaskStatistics taskStats = taskService.getTaskStatistics();

			Map<String, Object> statistics = new HashMap<>();
			statistics.put("activeStateMachines", stateMachineService.getActiveStateMachineCount());
			statistics.put("totalTasks", taskStats.getTotalTasks());
			statistics.put("completedTasks", taskStats.getCompletedTasks());
			statistics.put("failedTasks", taskStats.getFailedTasks());
			statistics.put("runningTasks", taskStats.getRunningTasks());
			statistics.put("successRate", taskStats.getSuccessRate());

			return ResponseEntity.ok(statistics);

		}
		catch (Exception e) {
			log.error("查询统计信息失败: {}", e.getMessage(), e);
			return ResponseEntity.internalServerError().build();
		}
	}

	/**
	 * 健康检查
	 */
	@GetMapping("/health")
	public ResponseEntity<Map<String, Object>> healthCheck() {
		Map<String, Object> health = new HashMap<>();
		health.put("status", "UP");
		health.put("timestamp", System.currentTimeMillis());
		health.put("activeStateMachines", stateMachineService.getActiveStateMachineCount());
		return ResponseEntity.ok(health);
	}

	/**
	 * 查找状态机ID（简化实现）
	 */
	private String findStateMachineIdByTaskId(String taskId) {
		// 简化实现：遍历所有状态机，查找匹配的任务ID
		for (String stateMachineId : stateMachineService.getActiveStateMachineIds()) {
			CoordinatorContext context = stateMachineService.getCoordinatorContext(stateMachineId);
			if (context != null && taskId.equals(context.getCurrentTaskId())) {
				return stateMachineId;
			}
		}
		return null;
	}

	/**
	 * 创建任务请求
	 */
	public static class CreateTaskRequest {

		private String taskId;

		private String title;

		private String description;

		private String targetEmail;

		private String subject;

		private String body;

		// Getters and Setters
		public String getTaskId() {
			return taskId;
		}

		public void setTaskId(String taskId) {
			this.taskId = taskId;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}

		public String getTargetEmail() {
			return targetEmail;
		}

		public void setTargetEmail(String targetEmail) {
			this.targetEmail = targetEmail;
		}

		public String getSubject() {
			return subject;
		}

		public void setSubject(String subject) {
			this.subject = subject;
		}

		public String getBody() {
			return body;
		}

		public void setBody(String body) {
			this.body = body;
		}

		@Override
		public String toString() {
			return String.format("CreateTaskRequest{taskId='%s', title='%s', targetEmail='%s'}", taskId, title,
					targetEmail);
		}

	}

	/**
	 * 触发事件请求
	 */
	public static class TriggerEventRequest {

		private String event;

		public String getEvent() {
			return event;
		}

		public void setEvent(String event) {
			this.event = event;
		}

	}

}