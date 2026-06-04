package com.jprime.connect.auth;

import com.jprime.connect.common.ApiException;
import com.jprime.connect.common.CurrentUserProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserProvider currentUser;
    private final SecureRandom random = new SecureRandom();

    public AuthController(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, JwtService jwtService, CurrentUserProvider currentUser) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        String publicId = publicId();
        String hash = passwordEncoder.encode(request.password());
        UUID id = jdbc.queryForObject("""
                insert into users (public_id, full_name, email, password_hash)
                values (?, ?, lower(?), ?)
                returning id
                """, UUID.class, publicId, request.fullName(), request.email(), hash);
        return responseFor(id, request.email().toLowerCase());
    }

    @PostMapping("/login")
    AuthResponse login(@Valid @RequestBody LoginRequest request) {
        var row = jdbc.query("select id, password_hash from users where email = lower(?)", rs -> {
            if (!rs.next()) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
            }
            return Map.of("id", rs.getObject("id", UUID.class), "password", rs.getString("password_hash"));
        }, request.email());
        if (!passwordEncoder.matches(request.password(), (String) row.get("password"))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        return responseFor((UUID) row.get("id"), request.email().toLowerCase());
    }

    @GetMapping("/me")
    UserDto me() {
        return user(currentUser.id(), true);
    }

    private AuthResponse responseFor(UUID id, String email) {
        return new AuthResponse(jwtService.createToken(email), user(id, true));
    }

    private UserDto user(UUID id, boolean includeEmail) {
        return jdbc.query("select id, public_id, full_name, email, profile_completed from users where id = ?", rs -> {
            if (!rs.next()) {
                throw ApiException.notFound("User not found");
            }
            return new UserDto(
                    rs.getObject("id", UUID.class),
                    rs.getString("public_id"),
                    rs.getString("full_name"),
                    includeEmail ? rs.getString("email") : null,
                    rs.getBoolean("profile_completed")
            );
        }, id);
    }

    private String publicId() {
        byte[] bytes = new byte[4];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public record RegisterRequest(@NotBlank String fullName, @Email @NotBlank String email, @Size(min = 8) String password) {}
    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token, UserDto user) {}
    public record UserDto(UUID id, String publicId, String fullName, String email, boolean profileCompleted) {}
}
