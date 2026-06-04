package com.jprime.connect.common;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class CurrentUserProvider {
    private final JdbcTemplate jdbc;

    public CurrentUserProvider(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID id() {
        String email = email();
        return jdbc.query("select id from users where email = ?", rs -> {
            if (!rs.next()) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "User not found");
            }
            return rs.getObject("id", UUID.class);
        }, email);
    }

    public String email() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return auth.getName();
    }
}
