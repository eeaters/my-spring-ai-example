package io.eeaters.statemachine.config;

import io.eeaters.statemachine.model.CoordinatorContext;
import io.eeaters.statemachine.state.Events;
import io.eeaters.statemachine.state.States;
import io.eeaters.statemachine.state.actions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.guard.Guard;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

import java.util.EnumSet;

/**
 * Spring State Machine 配置类
 *
 * 实现类似LangGraph的conditionEdge功能： - Guard: 提供条件判断逻辑 - Choice: 提供多路分支路由 - Extended State:
 * 在状态机上下文中传递数据
 *
 * @author eeaters
 * @version 1.0.0
 */
@Slf4j
@Configuration
@EnableStateMachineFactory
public class StateMachineConfig extends StateMachineConfigurerAdapter<States, Events> {

	// Action Bean注入
	private final CreateTaskAction createTaskAction;

	private final SendEmailAction sendEmailAction;

	private final PollEmailAction pollEmailAction;

	private final ProcessResponseAction processResponseAction;

	private final CompleteTaskAction completeTaskAction;

	public StateMachineConfig(CreateTaskAction createTaskAction, SendEmailAction sendEmailAction,
			PollEmailAction pollEmailAction, ProcessResponseAction processResponseAction,
			CompleteTaskAction completeTaskAction) {
		this.createTaskAction = createTaskAction;
		this.sendEmailAction = sendEmailAction;
		this.pollEmailAction = pollEmailAction;
		this.processResponseAction = processResponseAction;
		this.completeTaskAction = completeTaskAction;
	}

	@Override
	public void configure(StateMachineStateConfigurer<States, Events> states) throws Exception {
		states.withStates()
			.initial(States.IDLE)
			.states(EnumSet.allOf(States.class))
			.choice(States.RESPONSE_RECEIVED)
			.junction(States.POLLING);
	}

	@Override
	public void configure(StateMachineTransitionConfigurer<States, Events> transitions) throws Exception {
		transitions
			// 基础状态转换
			.withExternal()
			.source(States.IDLE)
			.target(States.TASK_CREATED)
			.event(Events.CREATE_TASK)
			.action(createTaskAction)
			.and()
			.withExternal()
			.source(States.TASK_CREATED)
			.target(States.EMAIL_SENT)
			.event(Events.SEND_EMAIL)
			.action(sendEmailAction)
			.and()
			.withExternal()
			.source(States.EMAIL_SENT)
			.target(States.POLLING)
			.event(Events.START_POLLING)
			.action(pollEmailAction)
			.and()
			.withExternal()
			.source(States.SECOND_EMAIL_SENT)
			.target(States.POLLING)
			.event(Events.START_POLLING)
			.action(pollEmailAction)
			.and()
			.withExternal()
			.source(States.POLLING)
			.target(States.RESPONSE_RECEIVED)
			.event(Events.RECEIVE_RESPONSE)
			.action(processResponseAction)
			.and()
			.withExternal()
			.source(States.TASK_CREATED)
			.target(States.FAILED)
			.event(Events.TASK_FAILED)
			.and()
			.withExternal()
			.source(States.EMAIL_SENT)
			.target(States.FAILED)
			.event(Events.TASK_FAILED)
			.and()
			.withExternal()
			.source(States.POLLING)
			.target(States.FAILED)
			.event(Events.TASK_FAILED)
			.and()
			.withExternal()
			.source(States.RESPONSE_RECEIVED)
			.target(States.FAILED)
			.event(Events.TASK_FAILED)
			.and()
			.withExternal()
			.source(States.SECOND_EMAIL_SENT)
			.target(States.FAILED)
			.event(Events.TASK_FAILED)
			.and()
			.withExternal()
			.source(States.COMPLETED)
			.target(States.IDLE)
			.event(Events.RESET)
			.and()
			.withExternal()
			.source(States.FAILED)
			.target(States.IDLE)
			.event(Events.RESET)

			// Choice状态 - 响应处理分支
			.and()
			.withChoice()
			.source(States.RESPONSE_RECEIVED)
			.first(States.COMPLETED, responseAgreeGuard())
			.then(States.SECOND_EMAIL_SENT, responseDisagreeGuard())
			.last(States.POLLING)

			// Junction状态 - 轮询分支
			.and()
			.withJunction()
			.source(States.POLLING)
			.first(States.POLLING, continuePollingGuard())
			.then(States.FAILED, timeoutGuard())
			.last(States.POLLING);
	}

	@Override
	public void configure(StateMachineConfigurationConfigurer<States, Events> config) throws Exception {
		config.withConfiguration().autoStartup(false).listener(new StateMachineListenerAdapter<States, Events>() {
			@Override
			public void stateChanged(State<States, Events> from, State<States, Events> to) {
				log.info("状态转换: {} -> {}", from != null ? from.getId() : "INITIAL",
						to != null ? to.getId() : "UNKNOWN");
			}

			@Override
			public void eventNotAccepted(org.springframework.messaging.Message<Events> event) {
				log.warn("事件未被接受: {}", event.getPayload());
			}

			@Override
			public void transition(org.springframework.statemachine.transition.Transition<States, Events> transition) {
				log.debug("转换详情: {} -> {} (事件: {})",
						transition.getSource() != null ? transition.getSource().getId() : "INITIAL",
						transition.getTarget() != null ? transition.getTarget().getId() : "UNKNOWN",
						transition.getTrigger() != null ? transition.getTrigger().getEvent() : "UNKNOWN");
			}
		});
	}

	/**
	 * 响应同意Guard - 检查响应是否为同意
	 */
	@Bean
	public Guard<States, Events> responseAgreeGuard() {
		return new ResponseAgreeGuard();
	}

	/**
	 * 响应不同意Guard - 检查响应是否为不同意
	 */
	@Bean
	public Guard<States, Events> responseDisagreeGuard() {
		return new ResponseDisagreeGuard();
	}

	/**
	 * 继续轮询Guard - 检查是否应该继续轮询
	 */
	@Bean
	public Guard<States, Events> continuePollingGuard() {
		return new ContinuePollingGuard();
	}

	/**
	 * 超时Guard - 检查任务是否超时
	 */
	@Bean
	public Guard<States, Events> timeoutGuard() {
		return new TimeoutGuard();
	}

	/**
	 * 响应同意Guard实现
	 */
	private static class ResponseAgreeGuard implements Guard<States, Events> {

		@Override
		public boolean evaluate(StateContext<States, Events> context) {
			CoordinatorContext coordinatorContext = getCoordinatorContext(context);
			if (coordinatorContext == null || coordinatorContext.getCurrentResponse() == null) {
				return false;
			}

			boolean isAgree = coordinatorContext.getCurrentResponse().isPositiveResponse();
			log.debug("响应同意Guard判断: {}", isAgree);
			return isAgree;
		}

	}

	/**
	 * 响应不同意Guard实现
	 */
	private static class ResponseDisagreeGuard implements Guard<States, Events> {

		@Override
		public boolean evaluate(StateContext<States, Events> context) {
			CoordinatorContext coordinatorContext = getCoordinatorContext(context);
			if (coordinatorContext == null || coordinatorContext.getCurrentResponse() == null) {
				return false;
			}

			boolean isDisagree = coordinatorContext.getCurrentResponse().isNegativeResponse();
			log.debug("响应不同意Guard判断: {}", isDisagree);
			return isDisagree;
		}

	}

	/**
	 * 继续轮询Guard实现
	 */
	private static class ContinuePollingGuard implements Guard<States, Events> {

		@Override
		public boolean evaluate(StateContext<States, Events> context) {
			CoordinatorContext coordinatorContext = getCoordinatorContext(context);
			if (coordinatorContext == null) {
				return false;
			}

			// 检查是否超时
			if (coordinatorContext.getCurrentTask() != null && coordinatorContext.getCurrentTask().isTimeout()) {
				log.debug("任务已超时，停止轮询");
				return false;
			}

			// 检查轮询次数限制
			Integer maxPollCount = (Integer) context.getExtendedState().getVariables().get("maxPollCount");
			if (maxPollCount != null && coordinatorContext.getPollCount() >= maxPollCount) {
				log.debug("轮询次数达到上限: {}", maxPollCount);
				return false;
			}

			log.debug("继续轮询Guard判断: true (当前轮询次数: {})", coordinatorContext.getPollCount());
			return true;
		}

	}

	/**
	 * 超时Guard实现
	 */
	private static class TimeoutGuard implements Guard<States, Events> {

		@Override
		public boolean evaluate(StateContext<States, Events> context) {
			CoordinatorContext coordinatorContext = getCoordinatorContext(context);
			if (coordinatorContext == null) {
				return false;
			}

			boolean isTimeout = coordinatorContext.getCurrentTask() != null
					&& coordinatorContext.getCurrentTask().isTimeout();
			log.debug("超时Guard判断: {}", isTimeout);
			return isTimeout;
		}

	}

	/**
	 * 从状态上下文中获取协调器上下文
	 */
	private static CoordinatorContext getCoordinatorContext(StateContext<States, Events> context) {
		return (CoordinatorContext) context.getExtendedState().getVariables().get("coordinatorContext");
	}

}