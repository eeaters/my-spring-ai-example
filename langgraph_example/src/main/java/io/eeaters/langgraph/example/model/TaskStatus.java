package io.eeaters.langgraph.example.model;

import lombok.Getter;

@Getter
public enum TaskStatus {

	CREATED("已创建"),
    AUTO_REPLY_SENT("自动回复已发送"),
    WAITING_FIRST_REPLY("等待首次回复"),
    FIRST_REPLY_RECEIVED("收到首次回复"),
	CONFIRMATION_SENT("确认邮件已发送"),
    WAITING_SECOND_REPLY("等待二次回复"),
    SECOND_REPLY_RECEIVED("收到二次回复"),
	FINAL_AGREEMENT_SENT("最终同意邮件已发送"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

	private final String description;

	TaskStatus(String description) {
		this.description = description;
	}

}