package com.jprime.connect.swipe;

import com.jprime.connect.common.ApiException;
import com.jprime.connect.common.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/swipes")
public class SwipeController {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider currentUser;

    public SwipeController(JdbcTemplate jdbc, CurrentUserProvider currentUser) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
    }

    @PostMapping
    @Transactional
    SwipeResponse swipe(@Valid @RequestBody SwipeRequest request) {
        UUID from = currentUser.id();
        UUID to = request.toUserId();
        if (from.equals(to)) {
            throw ApiException.badRequest("You cannot connect with yourself");
        }
        jdbc.update("""
                insert into swipes (from_user_id, to_user_id, action)
                values (?, ?, ?)
                on conflict (from_user_id, to_user_id) do update set action = excluded.action, updated_at = now()
                """, from, to, request.action());
        if ("MAYBE_LATER".equals(request.action())) {
            jdbc.update("""
                    insert into saved_candidates (user_id, saved_user_id) values (?, ?)
                    on conflict (user_id, saved_user_id) do nothing
                    """, from, to);
        }
        if (!"CONNECT".equals(request.action())) {
            return new SwipeResponse(false, null);
        }
        Integer reciprocal = jdbc.queryForObject("""
                select count(*) from swipes where from_user_id = ? and to_user_id = ? and action = 'CONNECT'
                """, Integer.class, to, from);
        if (reciprocal == null || reciprocal == 0) {
            return new SwipeResponse(false, null);
        }
        UUID user1 = from.toString().compareTo(to.toString()) < 0 ? from : to;
        UUID user2 = user1.equals(from) ? to : from;
        UUID matchId = jdbc.queryForObject("""
                insert into matches (user1_id, user2_id) values (?, ?)
                on conflict (user1_id, user2_id) do update set user1_id = excluded.user1_id
                returning id
                """, UUID.class, user1, user2);
        return new SwipeResponse(true, matchId);
    }

    public record SwipeRequest(UUID toUserId, String action) {}
    public record SwipeResponse(boolean matched, UUID matchId) {}
}
