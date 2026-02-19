package io.gocklkatz.helloopenapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "app.security")
public class UserConfig {

    private List<UserEntry> users = new ArrayList<>();

    public record UserEntry(String username, String password) {}

    public List<UserEntry> getUsers() {
        return users;
    }

    public void setUsers(List<UserEntry> users) {
        this.users = users;
    }
}
