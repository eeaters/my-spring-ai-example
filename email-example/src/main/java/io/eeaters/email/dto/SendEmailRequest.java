package io.eeaters.email.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendEmailRequest {

    private String host;
    private Integer port;
    private String username;
    private String password;
    private String protocol;

    private String to;
    private String cc;
    private String subject;
    private String content;
    private String replyTo;
    private String messageId;
}