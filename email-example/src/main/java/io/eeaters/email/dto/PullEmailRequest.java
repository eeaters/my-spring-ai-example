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
public class PullEmailRequest {

    private String host;
    private Integer port;
    private String username;
    private String password;
    private String protocol;
    private LocalDateTime since;
    private Integer maxCount;
}