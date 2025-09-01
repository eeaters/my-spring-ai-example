package io.eeaters.providers;

import io.eeaters.repository.UserRepository;
import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ResourceProvider {

    @Autowired
    private UserRepository userRepository;


    @McpResource(uri = "user-profile://{username}")
    public List<McpSchema.TextResourceContents> getUserEmail(String username) {
        List<UserRepository.User> userList = userRepository.findUserByUserNameLike(username);

        return userList.stream()
                .map(user -> {
                    String email = user.email();
                    String name = user.name();

                    return new McpSchema.TextResourceContents("user-profile://" + name,
                            "text/plain",
                            email);
                }).toList();
    }

}
