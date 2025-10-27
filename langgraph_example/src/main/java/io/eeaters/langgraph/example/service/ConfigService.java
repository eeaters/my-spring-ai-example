package io.eeaters.langgraph.example.service;

import io.eeaters.langgraph.example.config.EmailConfig;
import io.eeaters.langgraph.example.config.WorkflowConfig;
import io.eeaters.langgraph.example.model.Party;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConfigService {

	private static final Logger logger = LoggerFactory.getLogger(ConfigService.class);

	@Autowired
	private WorkflowConfig workflowConfig;

	@Autowired
	private EmailConfig emailConfig;

	public String getEmailForParty(Party party) {
		return switch (party) {
			case TRAILER_COMPANY -> workflowConfig.getTrailerCompanyEmail();
			case WAREHOUSE -> workflowConfig.getWarehouseEmail();
		};
	}

	public Party getFirstParty() {
		return workflowConfig.getFirstParty();
	}

	public Party getSecondParty() {
		return workflowConfig.getSecondParty();
	}

	public int getMaxRetryAttempts() {
		return workflowConfig.getMaxRetryAttempts();
	}

	public long getRetryDelayMinutes() {
		return workflowConfig.getRetryDelayMinutes();
	}

	public long getEmailTimeoutMinutes() {
		return workflowConfig.getEmailTimeoutMinutes();
	}

	public boolean isDebugEnabled() {
		return workflowConfig.isEnableDebug();
	}

	public EmailConfig getEmailConfig() {
		return emailConfig;
	}

	public WorkflowConfig getWorkflowConfig() {
		return workflowConfig;
	}

	public boolean validateConfiguration() {
		boolean isValid = true;

		if (workflowConfig.getTrailerCompanyEmail() == null
				|| workflowConfig.getTrailerCompanyEmail().trim().isEmpty()) {
			logger.error("Trailer company email is not configured");
			isValid = false;
		}

		if (workflowConfig.getWarehouseEmail() == null || workflowConfig.getWarehouseEmail().trim().isEmpty()) {
			logger.error("Warehouse email is not configured");
			isValid = false;
		}

		if (emailConfig.getHost() == null || emailConfig.getHost().trim().isEmpty()) {
			logger.error("Email host is not configured");
			isValid = false;
		}

		if (emailConfig.getUsername() == null || emailConfig.getUsername().trim().isEmpty()) {
			logger.error("Email username is not configured");
			isValid = false;
		}

		if (emailConfig.getPassword() == null || emailConfig.getPassword().trim().isEmpty()) {
			logger.error("Email password is not configured");
			isValid = false;
		}

		if (emailConfig.getFromAddress() == null || emailConfig.getFromAddress().trim().isEmpty()) {
			logger.error("From email address is not configured");
			isValid = false;
		}

		if (!isValid) {
			logger.error("Configuration validation failed");
		}
		else {
			logger.info("Configuration validation passed");
		}

		return isValid;
	}

}