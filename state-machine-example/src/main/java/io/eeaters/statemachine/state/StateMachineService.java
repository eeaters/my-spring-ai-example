package io.eeaters.statemachine.state;

import io.eeaters.statemachine.model.CoordinatorContext;
import io.eeaters.statemachine.agent.CoordinatorAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 状态机服务类
 *
 * 负责状态机的创建、管理和执行 提供统一的接口来操作状态机
 *
 * @author eeaters
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StateMachineService {

	private final StateMachineFactory<States, Events> stateMachineFactory;

	private final CoordinatorAgent coordinatorAgent;

	// 存储状态机实例
	private final Map<String, StateMachine<States, Events>> stateMachineRegistry = new ConcurrentHashMap<>();

	/**
	 * 创建新的状态机实例
	 * @param taskId 任务ID
	 * @return 状态机实例ID
	 */
	public String createStateMachine(String taskId) {
		log.info("创建状态机实例: taskId={}", taskId);

		try {
			// 创建协调上下文
			CoordinatorContext context = coordinatorAgent.createCoordinatorContext(taskId);

			// 创建状态机实例
			StateMachine<States, Events> stateMachine = stateMachineFactory.getStateMachine();

			// 设置状态机ID
			String stateMachineId = UUID.randomUUID().toString();
			stateMachine.getExtendedState().getVariables().put("stateMachineId", stateMachineId);
			stateMachine.getExtendedState().getVariables().put("coordinatorContext", context);

			// 注册状态机
			stateMachineRegistry.put(stateMachineId, stateMachine);

			// 启动状态机
			stateMachine.start();

			log.info("状态机实例创建成功: taskId={}, stateMachineId={}", taskId, stateMachineId);
			return stateMachineId;

		}
		catch (Exception e) {
			log.error("创建状态机实例失败: taskId={}, error={}", taskId, e.getMessage(), e);
			throw new RuntimeException("创建状态机失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 启动邮件协调任务
	 * @param stateMachineId 状态机ID
	 * @param title 任务标题
	 * @param description 任务描述
	 * @param targetEmail 目标邮箱
	 * @param subject 邮件主题
	 * @param body 邮件内容
	 * @return true如果启动成功
	 */
	public boolean startEmailTask(String stateMachineId, String title, String description, String targetEmail,
			String subject, String body) {
		log.info("启动邮件协调任务: stateMachineId={}, targetEmail={}", stateMachineId, targetEmail);

		try {
			StateMachine<States, Events> stateMachine = getStateMachine(stateMachineId);
			if (stateMachine == null) {
				throw new IllegalArgumentException("状态机不存在: " + stateMachineId);
			}

			// 设置任务参数到状态机上下文
			stateMachine.getExtendedState().getVariables().put("title", title);
			stateMachine.getExtendedState().getVariables().put("description", description);
			stateMachine.getExtendedState().getVariables().put("targetEmail", targetEmail);
			stateMachine.getExtendedState().getVariables().put("subject", subject);
			stateMachine.getExtendedState().getVariables().put("body", body);

			// 触发创建任务事件
			boolean eventSent = stateMachine.sendEvent(Events.CREATE_TASK);

			if (eventSent) {
				log.info("邮件协调任务启动成功: stateMachineId={}", stateMachineId);
			}
			else {
				log.warn("邮件协调任务启动失败，事件未被接受: stateMachineId={}", stateMachineId);
			}

			return eventSent;

		}
		catch (Exception e) {
			log.error("启动邮件协调任务失败: stateMachineId={}, error={}", stateMachineId, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 发送事件到状态机
	 * @param stateMachineId 状态机ID
	 * @param event 事件
	 * @return true如果事件发送成功
	 */
	public boolean sendEvent(String stateMachineId, Events event) {
		log.info("发送事件到状态机: stateMachineId={}, event={}", stateMachineId, event);

		try {
			StateMachine<States, Events> stateMachine = getStateMachine(stateMachineId);
			if (stateMachine == null) {
				throw new IllegalArgumentException("状态机不存在: " + stateMachineId);
			}

			boolean eventSent = stateMachine.sendEvent(event);
			log.info("事件发送结果: stateMachineId={}, event={}, sent={}", stateMachineId, event, eventSent);

			return eventSent;

		}
		catch (Exception e) {
			log.error("发送事件失败: stateMachineId={}, event={}, error={}", stateMachineId, event, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 获取状态机当前状态
	 * @param stateMachineId 状态机ID
	 * @return 当前状态
	 */
	public States getCurrentState(String stateMachineId) {
		StateMachine<States, Events> stateMachine = getStateMachine(stateMachineId);
		return stateMachine != null ? stateMachine.getState().getId() : null;
	}

	/**
	 * 获取协调上下文
	 * @param stateMachineId 状态机ID
	 * @return 协调上下文
	 */
	public CoordinatorContext getCoordinatorContext(String stateMachineId) {
		StateMachine<States, Events> stateMachine = getStateMachine(stateMachineId);
		if (stateMachine != null) {
			return (CoordinatorContext) stateMachine.getExtendedState().getVariables().get("coordinatorContext");
		}
		return null;
	}

	/**
	 * 获取状态机摘要信息
	 * @param stateMachineId 状态机ID
	 * @return 摘要信息
	 */
	public Map<String, Object> getStateMachineSummary(String stateMachineId) {
		StateMachine<States, Events> stateMachine = getStateMachine(stateMachineId);
		if (stateMachine == null) {
			return null;
		}

		CoordinatorContext context = getCoordinatorContext(stateMachineId);
		Map<String, Object> summary = coordinatorAgent.getContextSummary(context);

		summary.put("stateMachineId", stateMachineId);
		summary.put("currentState", stateMachine.getState().getId());
		summary.put("isComplete", stateMachine.isComplete());
		summary.put("extendedStateVariables", stateMachine.getExtendedState().getVariables());

		return summary;
	}

	/**
	 * 停止并销毁状态机
	 * @param stateMachineId 状态机ID
	 * @return true如果销毁成功
	 */
	public boolean destroyStateMachine(String stateMachineId) {
		log.info("销毁状态机: stateMachineId={}", stateMachineId);

		try {
			StateMachine<States, Events> stateMachine = stateMachineRegistry.remove(stateMachineId);
			if (stateMachine != null) {
				stateMachine.stop();
				log.info("状态机销毁成功: stateMachineId={}", stateMachineId);
				return true;
			}
			else {
				log.warn("状态机不存在，无需销毁: stateMachineId={}", stateMachineId);
				return false;
			}

		}
		catch (Exception e) {
			log.error("销毁状态机失败: stateMachineId={}, error={}", stateMachineId, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 获取所有活跃的状态机ID
	 * @return 状态机ID列表
	 */
	public java.util.Set<String> getActiveStateMachineIds() {
		return stateMachineRegistry.keySet();
	}

	/**
	 * 获取活跃状态机数量
	 * @return 活跃状态机数量
	 */
	public int getActiveStateMachineCount() {
		return stateMachineRegistry.size();
	}

	/**
	 * 清理所有状态机
	 */
	public void cleanupAllStateMachines() {
		log.info("清理所有状态机实例，当前数量: {}", stateMachineRegistry.size());

		stateMachineRegistry.keySet().forEach(this::destroyStateMachine);

		log.info("状态机清理完成");
	}

	/**
	 * 重置状态机到初始状态
	 * @param stateMachineId 状态机ID
	 * @return true如果重置成功
	 */
	public boolean resetStateMachine(String stateMachineId) {
		log.info("重置状态机: stateMachineId={}", stateMachineId);

		try {
			StateMachine<States, Events> stateMachine = getStateMachine(stateMachineId);
			if (stateMachine == null) {
				return false;
			}

			// 停止状态机
			stateMachine.stop();

			// 重置到初始状态
			stateMachine.getExtendedState().getVariables().clear();
			// 重新启动状态机，它会自动回到初始状态

			// 清理扩展状态
			CoordinatorContext context = getCoordinatorContext(stateMachineId);
			if (context != null) {
				context.clearTemporaryData();
				context.clearError();
			}

			// 重新启动状态机
			stateMachine.start();

			log.info("状态机重置成功: stateMachineId={}", stateMachineId);
			return true;

		}
		catch (Exception e) {
			log.error("重置状态机失败: stateMachineId={}, error={}", stateMachineId, e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 获取状态机实例
	 */
	private StateMachine<States, Events> getStateMachine(String stateMachineId) {
		return stateMachineRegistry.get(stateMachineId);
	}

}