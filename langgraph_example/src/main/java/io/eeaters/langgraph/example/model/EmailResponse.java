package io.eeaters.langgraph.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {

	private String id;

	private String from;

	private String to;

	private String subject;

	private String content;

	private LocalDateTime receivedAt;

	private String emailThreadId;

	private boolean isPositive;

	private LocalDateTime proposedTime;

	private String sender;

	@Override
	public String toString() {
		return "EmailResponse{" + "id='" + id + '\'' + ", from='" + from + '\'' + ", subject='" + subject + '\''
				+ ", isPositive=" + isPositive + ", proposedTime=" + proposedTime + '}';
	}

}