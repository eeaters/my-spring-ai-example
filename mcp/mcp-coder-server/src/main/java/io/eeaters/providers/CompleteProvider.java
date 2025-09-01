package io.eeaters.providers;

import io.eeaters.repository.UserRepository;
import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpComplete;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CompleteProvider {

    @Autowired
    private UserRepository userRepository;


    @McpComplete(uri = "user-name://{username}")
    public List<String> nameComplete(String name) {
        List<UserRepository.User> users = userRepository.findUserByUserNameLike(name);
        return users.stream().map(UserRepository.User::name).toList();
    }

    @McpComplete(uri = "user-email")
    public List<String> emailComplete(McpSchema.CompleteRequest request) {
        String value = request.argument().value();
        List<UserRepository.User> users = userRepository.findUserByEmailLike(value);
        return users.stream().map(UserRepository.User::name).toList();
    }


}
