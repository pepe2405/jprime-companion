package com.jprime.connect.match;

import com.jprime.connect.common.ApiException;
import com.jprime.connect.common.CurrentUserProvider;
import com.jprime.connect.discover.MatchingService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.jprime.connect.common.SqlSupport.dateTime;

@RestController
@RequestMapping("/api/matches")
public class MatchController {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider currentUser;
    private final MatchingService matchingService;

    public MatchController(JdbcTemplate jdbc, CurrentUserProvider currentUser, MatchingService matchingService) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
        this.matchingService = matchingService;
    }

    @GetMapping
    List<MatchSummaryDto> matches() {
        UUID userId = currentUser.id();
        return jdbc.query("""
                select m.id, m.created_at, u.id other_id, u.full_name, u.role_title, u.company
                from matches m
                join users u on u.id = case when m.user1_id = ? then m.user2_id else m.user1_id end
                where m.user1_id = ? or m.user2_id = ?
                order by m.created_at desc
                """, (rs, n) -> {
            var candidate = matchingService.candidate(userId, rs.getObject("other_id", UUID.class));
            return new MatchSummaryDto(rs.getObject("id", UUID.class), dateTime(rs.getTimestamp("created_at")),
                    rs.getObject("other_id", UUID.class), rs.getString("full_name"), rs.getString("role_title"),
                    rs.getString("company"), candidate.sharedInterests(), candidate.sharedTalks(), candidate.icebreaker(),
                    meetingStatus(rs.getObject("id", UUID.class)));
        }, userId, userId, userId);
    }

    @GetMapping("/{matchId}")
    MatchDetailDto detail(@PathVariable UUID matchId) {
        UUID userId = currentUser.id();
        UUID otherId = otherUser(matchId, userId);
        var candidate = matchingService.candidate(userId, otherId);
        var user = jdbc.query("select id, full_name, role_title, company, bio, linkedin_url, github_url, email from users where id = ?", rs -> {
            rs.next();
            return new MatchedUserDto(rs.getObject("id", UUID.class), rs.getString("full_name"), rs.getString("role_title"),
                    rs.getString("company"), rs.getString("bio"), rs.getString("linkedin_url"), rs.getString("github_url"), rs.getString("email"));
        }, otherId);
        LocalDateTime matchedAt = jdbc.queryForObject("select created_at from matches where id = ?", Timestamp.class, matchId).toLocalDateTime();
        return new MatchDetailDto(matchId, matchedAt, user, candidate.sharedInterests(), candidate.sharedGoals(),
                candidate.sharedTalks(), List.of(candidate.icebreaker(), "Are you planning to attend another session on this topic?"),
                meeting(matchId));
    }

    private String meetingStatus(UUID matchId) {
        Integer count = jdbc.queryForObject("select count(*) from meetings where match_id = ?", Integer.class, matchId);
        return count != null && count > 0 ? "Planned" : "Not planned";
    }

    private MeetingDto meeting(UUID matchId) {
        return jdbc.query("select * from meetings where match_id = ? order by created_at desc limit 1", rs -> {
            if (!rs.next()) return null;
            return new MeetingDto(rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("location"),
                    dateTime(rs.getTimestamp("start_time")), dateTime(rs.getTimestamp("end_time")),
                    rs.getString("note"), rs.getString("topic"));
        }, matchId);
    }

    private UUID otherUser(UUID matchId, UUID userId) {
        return jdbc.query("select user1_id, user2_id from matches where id = ?", rs -> {
            if (!rs.next()) throw ApiException.notFound("Match not found");
            UUID user1 = rs.getObject("user1_id", UUID.class);
            UUID user2 = rs.getObject("user2_id", UUID.class);
            if (!user1.equals(userId) && !user2.equals(userId)) throw ApiException.forbidden("You do not belong to this match");
            return user1.equals(userId) ? user2 : user1;
        }, matchId);
    }

    public record MatchSummaryDto(UUID matchId, LocalDateTime matchedAt, UUID otherUserId, String fullName,
                                  String roleTitle, String company, List<String> sharedInterests,
                                  List<MatchingService.SharedTalkDto> sharedTalks, String icebreaker, String meetingStatus) {}
    public record MatchedUserDto(UUID id, String fullName, String roleTitle, String company, String bio,
                                 String linkedinUrl, String githubUrl, String email) {}
    public record MeetingDto(UUID id, String title, String location, LocalDateTime startTime, LocalDateTime endTime,
                             String note, String topic) {}
    public record MatchDetailDto(UUID matchId, LocalDateTime matchedAt, MatchedUserDto otherUser,
                                 List<String> sharedInterests, List<String> sharedGoals,
                                 List<MatchingService.SharedTalkDto> sharedTalks, List<String> icebreakers,
                                 MeetingDto meeting) {}
}
