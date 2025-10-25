package io.eeaters.statemachine.state.actions;

import io.eeaters.statemachine.model.CoordinatorContext;
import io.eeaters.statemachine.model.EmailResponse;
import io.eeaters.statemachine.service.TaskService;
import io.eeaters.statemachine.agent.CoordinatorAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * 处理响应动作
 *
 * 在状态转换 POLLING -> RESPONSE_RECEIVED 时执行 负责分析邮件响应并准备后续的状态转换决策
 *
 * @author eearters
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessResponseAction
		implements Action<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> {

	private final TaskService taskService;

	private final CoordinatorAgent coordinatorAgent;

	@Override
	public void execute(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		log.info("执行处理响应动作");

		try {
			// 获取协调上下文
			CoordinatorContext coordinatorContext = getCoordinatorContext(context);
			if (coordinatorContext == null) {
				throw new IllegalStateException("协调上下文不存在");
			}

			// 获取当前响应
			EmailResponse currentResponse = coordinatorContext.getCurrentResponse();
			if (currentResponse == null) {
				throw new IllegalStateException("没有找到邮件响应");
			}

			// 更新上下文状态
			coordinatorAgent.updateContextState(coordinatorContext,
					io.eeaters.statemachine.state.States.RESPONSE_RECEIVED);

			// 分析响应质量
			ResponseAnalysisResult analysisResult = analyzeResponse(currentResponse);

			// 设置决策变量用于后续的状态转换
			context.getExtendedState().getVariables().put("responseType", currentResponse.getResponseType());
			context.getExtendedState().getVariables().put("responseConfidence", currentResponse.getConfidence());
			context.getExtendedState().getVariables().put("responseQuality", analysisResult.quality());
			context.getExtendedState().getVariables().put("needsFollowUp", analysisResult.needsFollowUp());
			context.getExtendedState().getVariables().put("currentAction", "PROCESS_RESPONSE");

			// 更新上下文扩展属性
			coordinatorContext.setExtendedAttribute("responseProcessedTime", java.time.LocalDateTime.now());
			coordinatorContext.setExtendedAttribute("responseAnalysis", analysisResult);

			// 记录响应处理日志
			log.info("邮件响应处理完成: taskId={}, responseType={}, confidence={}, quality={}, needsFollowUp={}",
					currentResponse.getTaskId(), currentResponse.getResponseType(), currentResponse.getConfidence(),
					analysisResult.quality(), analysisResult.needsFollowUp());

			// 根据响应类型设置不同的处理逻辑
			switch (currentResponse.getResponseType()) {
				case AGREE:
					handleAgreeResponse(coordinatorContext, currentResponse);
					break;
				case DISAGREE:
					handleDisagreeResponse(coordinatorContext, currentResponse);
					break;
				case UNCLEAR:
					handleUnclearResponse(coordinatorContext, currentResponse);
					break;
				case NEED_INFO:
					handleNeedInfoResponse(coordinatorContext, currentResponse);
					break;
				case NOT_RELATED:
					handleNotRelatedResponse(coordinatorContext, currentResponse);
					break;
			}

		}
		catch (Exception e) {
			log.error("处理响应动作执行失败: {}", e.getMessage(), e);

			// 更新上下文错误信息
			CoordinatorContext coordinatorContext = getCoordinatorContext(context);
			if (coordinatorContext != null) {
				coordinatorAgent.handleTaskError(coordinatorContext, "处理响应失败: " + e.getMessage());
			}

			// 设置状态机错误信息
			context.getExtendedState().getVariables().put("error", e.getMessage());
			context.getExtendedState().getVariables().put("currentAction", "PROCESS_RESPONSE_FAILED");

			throw new RuntimeException("处理响应失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 分析响应质量
	 */
	private ResponseAnalysisResult analyzeResponse(EmailResponse response) {
		ResponseQuality quality;
		boolean needsFollowUp = false;

		// 基于响应类型和置信度判断质量
		if (response.isPositiveResponse() && response.hasHighConfidence(0.8)) {
			quality = ResponseQuality.HIGH_QUALITY_POSITIVE;
			needsFollowUp = false;
		}
		else if (response.isNegativeResponse() && response.hasHighConfidence(0.8)) {
			quality = ResponseQuality.HIGH_QUALITY_NEGATIVE;
			needsFollowUp = false;
		}
		else if (response.isPositiveResponse() || response.isNegativeResponse()) {
			quality = ResponseQuality.MEDIUM_QUALITY;
			needsFollowUp = true;
		}
		else if (EmailResponse.ResponseType.UNCLEAR.equals(response.getResponseType())) {
			quality = ResponseQuality.LOW_QUALITY;
			needsFollowUp = true;
		}
		else {
			quality = ResponseQuality.UNKNOWN;
			needsFollowUp = true;
		}

		return new ResponseAnalysisResult(quality, needsFollowUp, response.getAnalysisNote());
	}

	/**
	 * 处理同意响应
	 */
	private void handleAgreeResponse(CoordinatorContext coordinatorContext, EmailResponse response) {
		log.info("处理同意响应: taskId={}, confidence={}", response.getTaskId(), response.getConfidence());

		// 标记响应为已处理
		taskService.markResponseAsProcessed(response.getResponseId());

		// 设置上下文变量
		coordinatorContext.setDecisionVariable("finalDecision", "AGREE");
		coordinatorContext.setDecisionVariable("taskCompleted", true);

		// 记录成功信息
		coordinatorContext.setExtendedAttribute("successReason", "收到同意响应");
		coordinatorContext.setExtendedAttribute("successTime", java.time.LocalDateTime.now());
	}

	/**
	 * 处理不同意响应
	 */
	private void handleDisagreeResponse(CoordinatorContext coordinatorContext, EmailResponse response) {
		log.info("处理不同意响应: taskId={}, confidence={}", response.getTaskId(), response.getConfidence());

		// 标记响应为已处理
		taskService.markResponseAsProcessed(response.getResponseId());

		// 设置上下文变量
		coordinatorContext.setDecisionVariable("finalDecision", "DISAGREE");
		coordinatorContext.setDecisionVariable("taskCompleted", true);

		// 记录完成信息
		coordinatorContext.setExtendedAttribute("completionReason", "收到不同意响应");
		coordinatorContext.setExtendedAttribute("completionTime", java.time.LocalDateTime.now());
	}

	/**
	 * 处理不明确响应
	 */
	private void handleUnclearResponse(CoordinatorContext coordinatorContext, EmailResponse response) {
		log.info("处理不明确响应: taskId={}, confidence={}", response.getTaskId(), response.getConfidence());

		// 设置上下文变量
		coordinatorContext.setDecisionVariable("finalDecision", "UNCLEAR");
		coordinatorContext.setDecisionVariable("needsFollowUp", true);

		// 记录处理信息
		coordinatorContext.setExtendedAttribute("unclearResponseReason", "响应内容不明确，需要跟进");
		coordinatorContext.setExtendedAttribute("followUpRequired", true);
	}

	/**
	 * 处理需要更多信息响应
	 */
	private void handleNeedInfoResponse(CoordinatorContext coordinatorContext, EmailResponse response) {
		log.info("处理需要更多信息响应: taskId={}", response.getTaskId());

		// 设置上下文变量
		coordinatorContext.setDecisionVariable("finalDecision", "NEED_INFO");
		coordinatorContext.setDecisionVariable("needsFollowUp", true);

		// 记录处理信息
		coordinatorContext.setExtendedAttribute("needInfoReason", "用户需要更多信息");
		coordinatorContext.setExtendedAttribute("followUpRequired", true);
	}

	/**
	 * 处理不相关响应
	 */
	private void handleNotRelatedResponse(CoordinatorContext coordinatorContext, EmailResponse response) {
		log.info("处理不相关响应: taskId={}", response.getTaskId());

		// 设置上下文变量
		coordinatorContext.setDecisionVariable("finalDecision", "NOT_RELATED");
		coordinatorContext.setDecisionVariable("continuePolling", true);

		// 记录处理信息
		coordinatorContext.setExtendedAttribute("notRelatedReason", "响应与任务不相关");
		coordinatorContext.setExtendedAttribute("ignoreResponse", true);
	}

	/**
	 * 从上下文中获取协调上下文
	 */
	private CoordinatorContext getCoordinatorContext(
			StateContext<io.eeaters.statemachine.state.States, io.eeaters.statemachine.state.Events> context) {
		return (CoordinatorContext) context.getExtendedState().getVariables().get("coordinatorContext");
	}

	/**
	 * 响应分析结果
	 */
	private record ResponseAnalysisResult(ResponseQuality quality, boolean needsFollowUp, String description) {
	}

	/**
	 * 响应质量枚举
	 */
	private enum ResponseQuality {

		HIGH_QUALITY_POSITIVE, // 高质量积极响应
		HIGH_QUALITY_NEGATIVE, // 高质量消极响应
		MEDIUM_QUALITY, // 中等质量
		LOW_QUALITY, // 低质量
		UNKNOWN // 未知

	}

}