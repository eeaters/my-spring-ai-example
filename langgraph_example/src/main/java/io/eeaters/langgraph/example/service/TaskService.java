package io.eeaters.langgraph.example.service;

import io.eeaters.langgraph.example.model.Task;
import io.eeaters.langgraph.example.model.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TaskService {

	private static final Logger logger = LoggerFactory.getLogger(TaskService.class);

	private final Map<String, Task> taskStore = new ConcurrentHashMap<>();

	public Task saveTask(Task task) {
		String taskId = task.getId();
		taskStore.put(taskId, task);
		logger.info("Task saved: {}", taskId);

		return task;
	}

	public Task getTask(String taskId) {
		return taskStore.get(taskId);
	}

	public List<Task> getAllTasks() {
		return new ArrayList<>(taskStore.values());
	}

	public List<Task> getTasksByStatus(TaskStatus status) {
		return taskStore.values().stream().filter(task -> task.getStatus() == status).toList();
	}

	public Task updateTaskStatus(String taskId, TaskStatus newStatus) {
		Task task = taskStore.get(taskId);
		if (task != null) {
			task.updateStatus(newStatus);
			taskStore.put(taskId, task);
			logger.info("Task {} status updated to: {}", taskId, newStatus);
			return task;
		}
		return null;
	}

	public void deleteTask(String taskId) {
		Task removed = taskStore.remove(taskId);
		if (removed != null) {
			logger.info("Task deleted: {}", taskId);
		}
	}

	public Task updateTask(String taskId, Task updatedTask) {
		Task existingTask = taskStore.get(taskId);
		if (existingTask != null) {
			taskStore.put(taskId, updatedTask);
			logger.info("Task updated: {}", taskId);
			return updatedTask;
		}
		return null;
	}

	public boolean exists(String taskId) {
		return taskStore.containsKey(taskId);
	}

	public long getTaskCount() {
		return taskStore.size();
	}

	public void clearAllTasks() {
		taskStore.clear();
		logger.info("All tasks cleared");
	}

}