package io.eeaters.langgraph.example.config;

import io.eeaters.langgraph.example.model.Party;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "workflow")
public class WorkflowConfig {

	private Party firstParty = Party.TRAILER_COMPANY;

	private Party secondParty = Party.WAREHOUSE;

	private String trailerCompanyEmail;

	private String warehouseEmail;

	private int maxRetryAttempts = 3;

	private long retryDelayMinutes = 5;

	private long emailTimeoutMinutes = 30;

	private boolean enableDebug = false;

	public Party getFirstParty() {
		return firstParty;
	}

	public void setFirstParty(Party firstParty) {
		this.firstParty = firstParty;
	}

	public Party getSecondParty() {
		return secondParty;
	}

	public void setSecondParty(Party secondParty) {
		this.secondParty = secondParty;
	}

	public String getTrailerCompanyEmail() {
		return trailerCompanyEmail;
	}

	public void setTrailerCompanyEmail(String trailerCompanyEmail) {
		this.trailerCompanyEmail = trailerCompanyEmail;
	}

	public String getWarehouseEmail() {
		return warehouseEmail;
	}

	public void setWarehouseEmail(String warehouseEmail) {
		this.warehouseEmail = warehouseEmail;
	}

	public int getMaxRetryAttempts() {
		return maxRetryAttempts;
	}

	public void setMaxRetryAttempts(int maxRetryAttempts) {
		this.maxRetryAttempts = maxRetryAttempts;
	}

	public long getRetryDelayMinutes() {
		return retryDelayMinutes;
	}

	public void setRetryDelayMinutes(long retryDelayMinutes) {
		this.retryDelayMinutes = retryDelayMinutes;
	}

	public long getEmailTimeoutMinutes() {
		return emailTimeoutMinutes;
	}

	public void setEmailTimeoutMinutes(long emailTimeoutMinutes) {
		this.emailTimeoutMinutes = emailTimeoutMinutes;
	}

	public boolean isEnableDebug() {
		return enableDebug;
	}

	public void setEnableDebug(boolean enableDebug) {
		this.enableDebug = enableDebug;
	}

}