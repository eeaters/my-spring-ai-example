package io.eeaters.email.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessage {

    private String messageId;
    private String from;
    private String to;
    private String cc;
    private String subject;
    private String content;
    private LocalDateTime receivedDate;
    private LocalDateTime sentDate;
    private String replyTo;
    private String inReplyTo;
}