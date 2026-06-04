package com.jprime.connect.swipe;

import com.jprime.connect.common.ApiException;
import com.jprime.connect.common.CurrentUserProvider;
import com.jprime.connect.discover.MatchingService;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/swipes")
public class SwipeController {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider currentUser;
    private final MatchingService matchingService;

    public SwipeController(JdbcTemplate jdbc, CurrentUserProvider currentUser, MatchingService matchingService) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
        this.matchingService = matchingService;
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

    @GetMapping("/history")
    List<SwipeHistoryItem> history() {
        UUID from = currentUser.id();
        return jdbc.query("""
                select to_user_id, action, updated_at
                from swipes
                where from_user_id = ? and action in ('CONNECT', 'SKIP', 'MAYBE_LATER')
                order by updated_at desc
                """, (rs, rowNum) -> {
            UUID targetUserId = rs.getObject("to_user_id", UUID.class);
            return new SwipeHistoryItem(
                    rs.getString("action"),
                    rs.getObject("updated_at", LocalDateTime.class),
                    matchingService.candidate(from, targetUserId));
        }, from);
    }

    public record SwipeRequest(UUID toUserId, String action) {}
    public record SwipeResponse(boolean matched, UUID matchId) {}
    public record SwipeHistoryItem(String action, LocalDateTime updatedAt, MatchingService.MatchCandidateDto candidate) {}
}
