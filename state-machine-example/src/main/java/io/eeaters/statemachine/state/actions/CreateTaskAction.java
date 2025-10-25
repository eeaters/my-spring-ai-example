package io.eeaters.statemachine.state.actions;

import io.eeaters.statemachine.model.CoordinatorContext;
import io.eeaters.statemachine.model.EmailTask;
import io.eeaters.statemachine.service.TaskService;
import io.eeaters.statemachine.agent.CoordinatorAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * 创建任务动作
 *
 * 在状态转换 IDLE -> TASK_CREATED 时执行 负责初始化邮件协调任务和上下文
 *
 * @author eeaters
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreateTaskAction
		implements Action<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> {

	private final TaskService taskService;

	private final CoordinatorAgent coordinatorAgent;

	@Override
	public void execute(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		log.info("执行创建任务动作");

		try {
			// 从上下文中获取任务信息
			String taskId = getTaskIdFromContext(context);
			String title = getTitleFromContext(context);
			String description = getDescriptionFromContext(context);
			String targetEmail = getTargetEmailFromContext(context);
			String subject = getSubjectFromContext(context);
			String body = getBodyFromContext(context);

			if (taskId == null || targetEmail == null || subject == null || body == null) {
				throw new IllegalArgumentException("缺少必要的任务信息");
			}

			// 创建邮件任务
			EmailTask task = taskService.createTask(title, description, targetEmail, subject, body);

			// 创建协调上下文
			CoordinatorContext coordinatorContext = coordinatorAgent.createCoordinatorContext(task.getTaskId());

			// 将上下文设置到状态机的Extended State中
			context.getExtendedState().getVariables().put("coordinatorContext", coordinatorContext);

			// 设置其他决策变量
			context.getExtendedState().getVariables().put("maxPollCount", 100);
			context.getExtendedState().getVariables().put("currentAction", "CREATE_TASK");

			log.info("创建任务动作执行成功: taskId={}, targetEmail={}", task.getTaskId(), targetEmail);

		}
		catch (Exception e) {
			log.error("创建任务动作执行失败: {}", e.getMessage(), e);

			// 设置错误信息
			context.getExtendedState().getVariables().put("error", e.getMessage());
			context.getExtendedState().getVariables().put("currentAction", "CREATE_TASK_FAILED");

			// 抛出异常以便状态机能够处理错误状态转换
			throw new RuntimeException("创建任务失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 从上下文中获取任务ID
	 */
	private String getTaskIdFromContext(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		// 尝试从消息头中获取
		Object taskId = context.getMessageHeader("taskId");
		if (taskId != null) {
			return taskId.toString();
		}

		// 尝试从Extended State中获取
		taskId = context.getExtendedState().getVariables().get("taskId");
		if (taskId != null) {
			return taskId.toString();
		}

		// 生成新的任务ID
		return java.util.UUID.randomUUID().toString();
	}

	/**
	 * 从上下文中获取任务标题
	 */
	private String getTitleFromContext(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		Object title = context.getMessageHeader("title");
		if (title != null) {
			return title.toString();
		}
		title = context.getExtendedState().getVariables().get("title");
		return title != null ? title.toString() : "邮件协调任务";
	}

	/**
	 * 从上下文中获取任务描述
	 */
	private String getDescriptionFromContext(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		Object description = context.getMessageHeader("description");
		if (description != null) {
			return description.toString();
		}
		description = context.getExtendedState().getVariables().get("description");
		return description != null ? description.toString() : "自动邮件协调任务";
	}

	/**
	 * 从上下文中获取目标邮箱
	 */
	private String getTargetEmailFromContext(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		Object targetEmail = context.getMessageHeader("targetEmail");
		if (targetEmail != null) {
			return targetEmail.toString();
		}
		return (String) context.getExtendedState().getVariables().get("targetEmail");
	}

	/**
	 * 从上下文中获取邮件主题
	 */
	private String getSubjectFromContext(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		Object subject = context.getMessageHeader("subject");
		if (subject != null) {
			return subject.toString();
		}
		subject = context.getExtendedState().getVariables().get("subject");
		return subject != null ? subject.toString() : "协调任务邀请";
	}

	/**
	 * 从上下文中获取邮件内容
	 */
	private String getBodyFromContext(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		Object body = context.getMessageHeader("body");
		if (body != null) {
			return body.toString();
		}
		body = context.getExtendedState().getVariables().get("body");
		return body != null ? body.toString() : "您好，\n\n我们邀请您参与协调任务，请回复是否同意。\n\n谢谢！";
	}

}