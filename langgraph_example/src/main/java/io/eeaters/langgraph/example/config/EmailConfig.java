package io.eeaters.langgraph.example.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "email")
public class EmailConfig {

	private String host;

	private int port;

	private String username;

	private String password;

	private String protocol = "imap";

	private boolean ssl = true;

	private String inboxFolder = "INBOX";

	private int pollingIntervalSeconds = 60;

	private String fromAddress;

	private String fromName;

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public boolean isSsl() {
		return ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public String getInboxFolder() {
		return inboxFolder;
	}

	public void setInboxFolder(String inboxFolder) {
		this.inboxFolder = inboxFolder;
	}

	public int getPollingIntervalSeconds() {
		return pollingIntervalSeconds;
	}

	public void setPollingIntervalSeconds(int pollingIntervalSeconds) {
		this.pollingIntervalSeconds = pollingIntervalSeconds;
	}

	public String getFromAddress() {
		return fromAddress;
	}

	public void setFromAddress(String fromAddress) {
		this.fromAddress = fromAddress;
	}

	public String getFromName() {
		return fromName;
	}

	public void setFromName(String fromName) {
		this.fromName = fromName;
	}

	public boolean isEnableDebug() {
		return false;
	}

}