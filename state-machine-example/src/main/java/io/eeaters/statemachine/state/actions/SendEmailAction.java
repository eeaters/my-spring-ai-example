package io.eeaters.statemachine.state.actions;

import io.eeaters.statemachine.config.EmailConfig;
import io.eeaters.statemachine.model.CoordinatorContext;
import io.eeaters.statemachine.service.EmailService;
import io.eeaters.statemachine.agent.CoordinatorAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * 发送邮件动作
 *
 * 在状态转换 TASK_CREATED -> EMAIL_SENT 或 RESPONSE_RECEIVED -> SECOND_EMAIL_SENT 时执行
 * 负责发送邮件给目标邮箱
 *
 * @author eeaters
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SendEmailAction
		implements Action<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> {

	private final EmailService emailService;

	private final CoordinatorAgent coordinatorAgent;

	private final EmailConfig emailConfig;

	@Override
	public void execute(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		log.info("执行发送邮件动作");

		try {
			// 获取协调上下文
			CoordinatorContext coordinatorContext = getCoordinatorContext(context);
			if (coordinatorContext == null || coordinatorContext.getCurrentTask() == null) {
				throw new IllegalStateException("协调上下文或任务不存在");
			}

			// 判断是发送初始邮件还是跟进邮件
			boolean isFollowUpEmail = isFollowUpEmail(context, coordinatorContext);

			// 准备邮件内容
			String subject = isFollowUpEmail ? coordinatorContext.getCurrentTask().getFollowUpSubject()
					: coordinatorContext.getCurrentTask().getInitialSubject();

			String body = isFollowUpEmail ? coordinatorContext.getCurrentTask().getFollowUpBody()
					: coordinatorContext.getCurrentTask().getInitialBody();

			// 如果使用模板配置
			if (subject == null || body == null) {
				if (isFollowUpEmail) {
					subject = emailConfig.getTemplates().getFollowUpEmail().getSubject();
					body = emailConfig.getTemplates().getFollowUpEmail().getBody();
				}
				else {
					subject = emailConfig.getTemplates().getInitialEmail().getSubject();
					body = emailConfig.getTemplates().getInitialEmail().getBody();
				}
			}

			// 发送邮件
			boolean success = emailService.sendSimpleEmail(coordinatorContext.getCurrentTask().getTargetEmail(),
					subject, body);

			if (success) {
				// 更新上下文
				coordinatorAgent.updateContextState(coordinatorContext,
						io.eeaters.statemachine.state.States.EMAIL_SENT);
				coordinatorContext.incrementSentEmailCount();
				coordinatorContext.setExtendedAttribute("lastEmailSentTime", java.time.LocalDateTime.now());
				coordinatorContext.setExtendedAttribute("emailType", isFollowUpEmail ? "FOLLOW_UP" : "INITIAL");

				// 更新状态机变量
				context.getExtendedState().getVariables().put("currentAction", "SEND_EMAIL_SUCCESS");
				context.getExtendedState().getVariables().put("lastEmailTime", java.time.LocalDateTime.now());

				log.info("邮件发送成功: taskId={}, targetEmail={}, type={}", coordinatorContext.getCurrentTaskId(),
						coordinatorContext.getCurrentTask().getTargetEmail(), isFollowUpEmail ? "跟进邮件" : "初始邮件");

			}
			else {
				throw new RuntimeException("邮件发送失败");
			}

		}
		catch (Exception e) {
			log.error("发送邮件动作执行失败: {}", e.getMessage(), e);

			// 更新上下文错误信息
			CoordinatorContext coordinatorContext = getCoordinatorContext(context);
			if (coordinatorContext != null) {
				coordinatorAgent.handleTaskError(coordinatorContext, "邮件发送失败: " + e.getMessage());
			}

			// 设置状态机错误信息
			context.getExtendedState().getVariables().put("error", e.getMessage());
			context.getExtendedState().getVariables().put("currentAction", "SEND_EMAIL_FAILED");

			throw new RuntimeException("发送邮件失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 判断是否为跟进邮件
	 */
	private boolean isFollowUpEmail(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context,
			CoordinatorContext coordinatorContext) {
		// 如果已经发送过邮件，则为跟进邮件
		return coordinatorContext.getSentEmailCount() != null && coordinatorContext.getSentEmailCount() > 0;
	}

	/**
	 * 从上下文中获取协调上下文
	 */
	private CoordinatorContext getCoordinatorContext(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		return (CoordinatorContext) context.getExtendedState().getVariables().get("coordinatorContext");
	}

}