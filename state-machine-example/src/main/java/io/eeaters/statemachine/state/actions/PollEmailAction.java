package io.eeaters.statemachine.state.actions;

import io.eeaters.statemachine.model.CoordinatorContext;
import io.eeaters.statemachine.model.EmailResponse;
import io.eeaters.statemachine.service.EmailService;
import io.eeaters.statemachine.service.TaskService;
import io.eeaters.statemachine.agent.CoordinatorAgent;
import io.eeaters.statemachine.agent.ResponseAnalyzerAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 轮询邮件动作
 *
 * 在状态转换 EMAIL_SENT -> POLLING 或 SECOND_EMAIL_SENT -> POLLING 时执行 负责检查邮箱新邮件并分析响应
 *
 * @author eeaters
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PollEmailAction
		implements Action<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> {

	private final EmailService emailService;

	private final TaskService taskService;

	private final CoordinatorAgent coordinatorAgent;

	private final ResponseAnalyzerAgent responseAnalyzerAgent;

	@Override
	public void execute(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		log.info("执行轮询邮件动作");

		try {
			// 获取协调上下文
			CoordinatorContext coordinatorContext = getCoordinatorContext(context);
			if (coordinatorContext == null) {
				throw new IllegalStateException("协调上下文不存在");
			}

			// 更新上下文状态
			coordinatorAgent.updateContextState(coordinatorContext, io.eeaters.statemachine.state.States.POLLING);
			coordinatorContext.incrementPollCount();

			// 获取轮询起始时间（上次检查时间）
			LocalDateTime lastCheckTime = getLastCheckTime(coordinatorContext);

			// 检查邮箱新邮件
			List<EmailService.EmailMessage> newEmails = emailService.checkNewEmails(lastCheckTime);

			log.info("轮询邮件完成: taskId={}, pollCount={}, newEmailsCount={}", coordinatorContext.getCurrentTaskId(),
					coordinatorContext.getPollCount(), newEmails.size());

			boolean hasNewResponse = false;

			// 处理新邮件
			for (EmailService.EmailMessage emailMessage : newEmails) {
				if (isTargetEmail(emailMessage, coordinatorContext)) {
					// 分析邮件响应
					EmailResponse response = responseAnalyzerAgent.analyzeResponse(
							coordinatorContext.getCurrentTaskId(), emailMessage.getSubject(), emailMessage.getContent(),
							emailMessage.getFrom(), emailMessage.getTo());

					// 保存响应
					taskService.saveResponse(response);

					// 设置到上下文
					coordinatorAgent.setResponseToContext(coordinatorContext, response);

					// 更新最后检查时间
					coordinatorContext.setExtendedAttribute("lastCheckTime", LocalDateTime.now());

					hasNewResponse = true;
					log.info("发现新的邮件响应: taskId={}, responseType={}, confidence={}", response.getTaskId(),
							response.getResponseType(), response.getConfidence());

					// 只处理第一个相关的响应，其他响应后续处理
					break;
				}
			}

			// 更新状态机变量
			if (hasNewResponse) {
				context.getExtendedState().getVariables().put("hasNewResponse", true);
				context.getExtendedState().getVariables().put("currentAction", "POLL_EMAIL_FOUND_RESPONSE");
			}
			else {
				context.getExtendedState().getVariables().put("hasNewResponse", false);
				context.getExtendedState().getVariables().put("currentAction", "POLL_EMAIL_NO_RESPONSE");

				// 设置下次轮询的延迟
				scheduleNextPoll(context, coordinatorContext);
			}

			log.info("轮询邮件动作执行完成: taskId={}, hasNewResponse={}", coordinatorContext.getCurrentTaskId(), hasNewResponse);

		}
		catch (Exception e) {
			log.error("轮询邮件动作执行失败: {}", e.getMessage(), e);

			// 更新上下文错误信息
			CoordinatorContext coordinatorContext = getCoordinatorContext(context);
			if (coordinatorContext != null) {
				coordinatorAgent.handleTaskError(coordinatorContext, "轮询邮件失败: " + e.getMessage());
			}

			// 设置状态机错误信息
			context.getExtendedState().getVariables().put("error", e.getMessage());
			context.getExtendedState().getVariables().put("currentAction", "POLL_EMAIL_FAILED");

			throw new RuntimeException("轮询邮件失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 获取上次检查时间
	 */
	private LocalDateTime getLastCheckTime(CoordinatorContext coordinatorContext) {
		LocalDateTime lastCheckTime = coordinatorContext.getExtendedAttribute("lastCheckTime");
		if (lastCheckTime == null) {
			// 如果没有上次检查时间，使用任务创建时间或流程开始时间
			lastCheckTime = coordinatorContext.getCurrentTask() != null
					? coordinatorContext.getCurrentTask().getCreatedAt() : coordinatorContext.getStartTime();
		}
		return lastCheckTime;
	}

	/**
	 * 判断是否为目标邮件
	 */
	private boolean isTargetEmail(EmailService.EmailMessage emailMessage, CoordinatorContext coordinatorContext) {
		// 检查发件人是否为目标邮箱
		String targetEmail = coordinatorContext.getCurrentTask().getTargetEmail();
		String fromEmail = emailMessage.getFrom();

		// 简单的邮箱匹配（实际项目中可能需要更复杂的逻辑）
		boolean isFromTarget = fromEmail != null && fromEmail.contains(targetEmail);

		// 检查邮件主题是否相关
		String subject = emailMessage.getSubject();
		boolean isRelatedSubject = subject != null && (subject.contains("协调") || subject.contains("任务")
				|| subject.toLowerCase().contains("coordinator") || subject.toLowerCase().contains("task"));

		log.debug("邮件匹配检查: from={}, to={}, subject={}, isFromTarget={}, isRelatedSubject={}", fromEmail,
				emailMessage.getTo(), subject, isFromTarget, isRelatedSubject);

		return isFromTarget || isRelatedSubject;
	}

	/**
	 * 安排下次轮询
	 */
	private void scheduleNextPoll(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context,
			CoordinatorContext coordinatorContext) {
		// 获取轮询间隔
		Long pollInterval = coordinatorContext.getDecisionVariable("pollInterval");
		if (pollInterval == null) {
			pollInterval = 30000L; // 默认30秒
		}

		// 设置下次轮询时间（这个逻辑通常由调度器实现，这里只是记录）
		LocalDateTime nextPollTime = LocalDateTime.now().plusSeconds(pollInterval / 1000);
		coordinatorContext.setExtendedAttribute("nextPollTime", nextPollTime);

		log.debug("已安排下次轮询: taskId={}, nextPollTime={}", coordinatorContext.getCurrentTaskId(), nextPollTime);
	}

	/**
	 * 从上下文中获取协调上下文
	 */
	private CoordinatorContext getCoordinatorContext(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		return (CoordinatorContext) context.getExtendedState().getVariables().get("coordinatorContext");
	}

}