package io.eeaters.langgraph.example.config;

import io.eeaters.langgraph.example.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

	@Autowired
	private ConfigService configService;

	@Bean
	public JavaMailSender javaMailSender() {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(configService.getEmailConfig().getHost());
		mailSender.setPort(configService.getEmailConfig().getPort());
		mailSender.setUsername(configService.getEmailConfig().getUsername());
		mailSender.setPassword(configService.getEmailConfig().getPassword());

		Properties props = mailSender.getJavaMailProperties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.debug", configService.isDebugEnabled());

		return mailSender;
	}

}