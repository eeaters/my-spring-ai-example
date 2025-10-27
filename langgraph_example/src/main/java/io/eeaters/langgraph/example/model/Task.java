package io.eeaters.langgraph.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {

	@Default
	private String id = UUID.randomUUID().toString();

	private String title;

	private LocalDateTime scheduledTime;

	private String location;

	@Default
	private TaskStatus status = TaskStatus.CREATED;

	@Default
	private Party currentParty = Party.TRAILER_COMPANY;

	@Default
	private Party nextParty = Party.WAREHOUSE;

	@Default
	private LocalDateTime createdAt = LocalDateTime.now();

	@Default
	private LocalDateTime updatedAt = LocalDateTime.now();

	private String emailThreadId;

	private String finalConfirmedTime;

	public void updateStatus(TaskStatus newStatus) {
		this.status = newStatus;
		this.updatedAt = LocalDateTime.now();
	}

	@Override
	public String toString() {
		return "Task{" + "id='" + id + '\'' + ", title='" + title + '\'' + ", scheduledTime=" + scheduledTime
				+ ", location='" + location + '\'' + ", status=" + status + ", currentParty=" + currentParty
				+ ", nextParty=" + nextParty + '}';
	}

}