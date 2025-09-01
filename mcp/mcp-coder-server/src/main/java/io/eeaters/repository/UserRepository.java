package io.eeaters.repository;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserRepository {

    public final List<User> users = List.of(
            new User("John Smith", "john.smith@example.com"),
            new User("Emily Johnson", "emily.johnson@example.com"),
            new User("Michael Brown", "michael.brown@example.com"),
            new User("Sarah Davis", "sarah.davis@example.com"),
            new User("David Wilson", "david.wilson@example.com"),
            new User("Jessica Miller", "jessica.miller@example.com"),
            new User("Christopher Moore", "christopher.moore@example.com"),
            new User("Amanda Taylor", "amanda.taylor@example.com"),
            new User("James Anderson", "james.anderson@example.com"),
            new User("Jennifer Thomas", "jennifer.thomas@example.com"),
            new User("Robert Jackson", "robert.jackson@example.com"),
            new User("Elizabeth White", "elizabeth.white@example.com"),
            new User("William Harris", "william.harris@example.com"),
            new User("Megan Martin", "megan.martin@example.com"),
            new User("Daniel Thompson", "daniel.thompson@example.com")
    );

    public List<User> findUserByUserNameLike(String name) {
        return users.stream()
                .filter(user -> user.name().contains(name))
                .toList();
    }


    public List<User> findUserByEmailLike(String email) {
        return users.stream()
                .filter(user -> user.name().contains(email))
                .toList();
    }

    public record User(String name, String email) {

    }

}
