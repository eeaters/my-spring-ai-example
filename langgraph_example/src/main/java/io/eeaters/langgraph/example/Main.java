package io.eeaters.langgraph.example;

import io.eeaters.langgraph.example.model.Party;
import io.eeaters.langgraph.example.model.Task;
import io.eeaters.langgraph.example.model.TaskStatus;
import io.eeaters.langgraph.example.service.ConfigService;
import io.eeaters.langgraph.example.service.TaskService;
import io.eeaters.langgraph.example.workflow.TaskWorkflow;
import org.bsc.langgraph4j.GraphRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Scanner;

@SpringBootApplication
@ComponentScan
@EnableScheduling
public class Main {

	private static final Logger logger = LoggerFactory.getLogger(Main.class);

	@Autowired
	private TaskWorkflow taskWorkflow;

	@Autowired
	private TaskService taskService;

	@Autowired
	private ConfigService configService;

	public static void main(String[] args) {
		SpringApplication.run(Main.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner() {
		return args -> {
			logger.info("=== LangGraph Email Workflow System Started ===");

			if (!configService.validateConfiguration()) {
				logger.error("Configuration validation failed. Please check your application.properties");
				return;
			}

			try {
				taskWorkflow.initialize();
				logger.info("Workflow initialized successfully");

				GraphRepresentation graph = taskWorkflow.getGraphRepresentation();
				logger.info("Workflow Graph:\n{}", graph.content());

				runInteractiveDemo();

			}
			catch (Exception e) {
				logger.error("Failed to initialize workflow", e);
			}
		};
	}

	private void runInteractiveDemo() {
		Scanner scanner = new Scanner(System.in);

		while (true) {
			System.out.println("\n=== LangGraph Email Workflow System ===");
			System.out.println("1. Create new task");
			System.out.println("2. View all tasks");
			System.out.println("3. View workflow graph");
			System.out.println("4. Exit");
			System.out.print("Please select an option: ");

			String choice = scanner.nextLine().trim();

			switch (choice) {
				case "1":
					createNewTask(scanner);
					break;
				case "2":
					viewAllTasks();
					break;
				case "3":
					viewWorkflowGraph();
					break;
				case "4":
					logger.info("Exiting application");
					return;
				default:
					System.out.println("Invalid option, please try again");
			}
		}
	}

	private void createNewTask(Scanner scanner) {
		try {
			System.out.print("Enter task title: ");
			String title = scanner.nextLine().trim();

			System.out.print("Enter location: ");
			String location = scanner.nextLine().trim();

			System.out.print("Enter scheduled time (yyyy-MM-dd HH:mm, e.g., 2024-12-25 14:30): ");
			String timeStr = scanner.nextLine().trim();

			LocalDateTime scheduledTime = LocalDateTime.parse(timeStr,
					java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));

			Task task = Task.builder()
				.title(title)
				.scheduledTime(scheduledTime)
				.location(location)
				.currentParty(configService.getFirstParty())
				.nextParty(configService.getSecondParty())
				.build();

			System.out.println("\nCreating task: " + task);
			System.out.println("First party: " + configService.getFirstParty());
			System.out.println("Second party: " + configService.getSecondParty());

			logger.info("Executing workflow for task: {}", task.getId());
			Optional<io.eeaters.langgraph.example.workflow.WorkflowState> result = taskWorkflow.execute(task);

			if (result.isPresent()) {
				io.eeaters.langgraph.example.workflow.WorkflowState state = result.get();
				System.out.println("Workflow execution completed:");
				System.out.println("Current step: " + state.getCurrentStep());
				System.out.println("Task status: " + state.getTask().getStatus());
				System.out.println("Completed: " + state.isCompleted());
			}
			else {
				System.out.println("Workflow execution failed or was interrupted");
			}

		}
		catch (Exception e) {
			logger.error("Failed to create task", e);
			System.out.println("Error: " + e.getMessage());
		}
	}

	private void viewAllTasks() {
		var tasks = taskService.getAllTasks();
		if (tasks.isEmpty()) {
			System.out.println("No tasks found");
			return;
		}

		System.out.println("\n=== All Tasks ===");
		for (Task task : tasks) {
			System.out.println("ID: " + task.getId());
			System.out.println("Title: " + task.getTitle());
			System.out.println("Status: " + task.getStatus());
			System.out.println("Location: " + task.getLocation());
			System.out.println("Scheduled Time: " + task.getScheduledTime());
			System.out.println("Current Party: " + task.getCurrentParty());
			System.out.println("Next Party: " + task.getNextParty());
			System.out.println("Created: " + task.getCreatedAt());
			System.out.println("---");
		}
	}

	private void viewWorkflowGraph() {
		try {
			GraphRepresentation graph = taskWorkflow.getGraphRepresentation();
			System.out.println("\n=== Workflow Graph (Mermaid) ===");
			System.out.println(graph.content());
		}
		catch (Exception e) {
			logger.error("Failed to get workflow graph", e);
			System.out.println("Error: " + e.getMessage());
		}
	}

}