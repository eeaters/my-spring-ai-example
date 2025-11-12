package io.eeaters.email.client;

import io.eeaters.email.dto.EmailMessage;
import io.eeaters.email.dto.PullEmailRequest;
import io.eeaters.email.dto.SendEmailRequest;

import java.util.List;

public interface EmailClient {

    List<EmailMessage> pullEmail(PullEmailRequest request) throws Exception;

    void sendEmail(SendEmailRequest request) throws Exception;
}