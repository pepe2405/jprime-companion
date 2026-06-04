package com.jprime.connect.talk;

import com.jprime.connect.common.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.jprime.connect.common.SqlSupport.*;

@RestController
@RequestMapping("/api")
public class TalkController {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider currentUser;

    public TalkController(JdbcTemplate jdbc, CurrentUserProvider currentUser) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
    }

    @GetMapping("/interests")
    List<LookupDto> interests() {
        return jdbc.query("select id, name from interests order by name",
                (rs, n) -> new LookupDto(rs.getObject("id", UUID.class), rs.getString("name")));
    }

    @GetMapping("/goals")
    List<LookupDto> goals() {
        return jdbc.query("select id, name from looking_for_goals order by name",
                (rs, n) -> new LookupDto(rs.getObject("id", UUID.class), rs.getString("name")));
    }

    @GetMapping("/talks")
    List<TalkDto> talks(@RequestParam(required = false) String hall, @RequestParam(required = false) String tag) {
        String sql = "select * from talks where 1 = 1";
        java.util.ArrayList<Object> params = new java.util.ArrayList<>();
        if (hall != null && !hall.isBlank()) {
            sql += " and hall = ?";
            params.add(hall);
        }
        if (tag != null && !tag.isBlank()) {
            sql += " and ? = any(tags)";
            params.add(tag);
        }
        sql += " order by start_time";
        return jdbc.query(sql, (rs, n) -> new TalkDto(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("speaker"),
                rs.getString("description"),
                rs.getString("hall"),
                dateTime(rs.getTimestamp("start_time")),
                dateTime(rs.getTimestamp("end_time")),
                textArray(rs.getArray("tags")),
                stats(rs.getObject("id", UUID.class)),
                rs.getObject("official_id", Integer.class),
                rs.getString("talk_level"),
                rs.getBoolean("beginner_friendly"),
                rs.getString("format"),
                rs.getString("audience"),
                textArray(rs.getArray("takeaways")),
                rs.getString("source_url")
        ), params.toArray());
    }

    @GetMapping("/talks/{talkId}")
    TalkDto talk(@PathVariable UUID talkId) {
        return jdbc.queryForObject("select * from talks where id = ?", (rs, n) -> new TalkDto(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("speaker"),
                rs.getString("description"),
                rs.getString("hall"),
                dateTime(rs.getTimestamp("start_time")),
                dateTime(rs.getTimestamp("end_time")),
                textArray(rs.getArray("tags")),
                stats(talkId),
                rs.getObject("official_id", Integer.class),
                rs.getString("talk_level"),
                rs.getBoolean("beginner_friendly"),
                rs.getString("format"),
                rs.getString("audience"),
                textArray(rs.getArray("takeaways")),
                rs.getString("source_url")
        ), talkId);
    }

    @GetMapping("/talks/{talkId}/stats")
    TalkStatsDto statsEndpoint(@PathVariable UUID talkId) {
        return stats(talkId);
    }

    @GetMapping("/talk-discussions")
    List<TalkDiscussionDto> discussionRooms() {
        return jdbc.query("""
                select t.id, t.title, t.speaker, t.hall, t.start_time, count(uts.user_id) attendee_count
                from talks t
                join user_talk_selections uts on uts.talk_id = t.id and uts.status = 'WANT_TO_DISCUSS'
                group by t.id, t.title, t.speaker, t.hall, t.start_time
                order by attendee_count desc, t.start_time, t.title
                """, (rs, n) -> {
            UUID talkId = rs.getObject("id", UUID.class);
            return new TalkDiscussionDto(talkId, rs.getString("title"), rs.getString("speaker"),
                    rs.getString("hall"), dateTime(rs.getTimestamp("start_time")), rs.getInt("attendee_count"),
                    discussionAttendees(talkId));
        });
    }

    @GetMapping("/users/me/talk-selections")
    List<TalkSelectionDto> mySelections() {
        UUID userId = currentUser.id();
        return jdbc.query("""
                select t.id, t.title, array_agg(uts.status order by uts.status) statuses
                from talks t join user_talk_selections uts on uts.talk_id = t.id
                where uts.user_id = ?
                group by t.id, t.title
                order by t.title
                """, (rs, n) -> new TalkSelectionDto(rs.getObject("id", UUID.class), rs.getString("title"),
                textArray(rs.getArray("statuses"))), userId);
    }

    @PutMapping("/users/me/talk-selections")
    @Transactional
    List<TalkSelectionDto> updateSelections(@Valid @RequestBody UpdateTalkSelectionsRequest request) {
        UUID userId = currentUser.id();
        jdbc.update("delete from user_talk_selections where user_id = ?", userId);
        for (SelectionInput selection : request.selections()) {
            for (String status : strings(selection.statuses())) {
                jdbc.update("insert into user_talk_selections (user_id, talk_id, status) values (?, ?, ?)",
                        userId, selection.talkId(), status);
            }
        }
        return mySelections();
    }

    private TalkStatsDto stats(UUID talkId) {
        return jdbc.query("""
                select
                count(*) filter (where status = 'INTERESTED') interested,
                count(*) filter (where status = 'ATTENDED') attended,
                count(*) filter (where status = 'WANT_TO_DISCUSS') discuss,
                count(*) filter (where status = 'MISSED_WANT_RECAP') missed
                from user_talk_selections where talk_id = ?
                """, rs -> {
            rs.next();
            return new TalkStatsDto(talkId, rs.getInt("interested"), rs.getInt("attended"), rs.getInt("discuss"), rs.getInt("missed"));
        }, talkId);
    }

    private List<DiscussionAttendeeDto> discussionAttendees(UUID talkId) {
        return jdbc.query("""
                select u.id, u.public_id, u.full_name, u.role_title, u.company, u.bio,
                       exists (
                         select 1 from matches m
                         where (m.user1_id = ? and m.user2_id = u.id) or (m.user2_id = ? and m.user1_id = u.id)
                       ) matched
                from users u
                join user_talk_selections uts on uts.user_id = u.id and uts.talk_id = ? and uts.status = 'WANT_TO_DISCUSS'
                where u.profile_completed = true
                order by matched desc, u.full_name
                """, (rs, n) -> new DiscussionAttendeeDto(rs.getObject("id", UUID.class),
                rs.getString("public_id"), rs.getString("full_name"), rs.getString("role_title"),
                rs.getString("company"), rs.getString("bio"), rs.getBoolean("matched")),
                currentUser.id(), currentUser.id(), talkId);
    }

    public record LookupDto(UUID id, String name) {}
    public record TalkStatsDto(UUID talkId, int interestedCount, int attendedCount, int wantToDiscussCount, int missedWantRecapCount) {}
    public record TalkDto(UUID id, String title, String speaker, String description, String hall, LocalDateTime startTime,
                          LocalDateTime endTime, List<String> tags, TalkStatsDto stats, Integer officialId,
                          String talkLevel, boolean beginnerFriendly, String format, String audience,
                          List<String> takeaways, String sourceUrl) {}
    public record TalkSelectionDto(UUID talkId, String title, List<String> statuses) {}
    public record TalkDiscussionDto(UUID talkId, String title, String speaker, String hall, LocalDateTime startTime,
                                    int attendeeCount, List<DiscussionAttendeeDto> attendees) {}
    public record DiscussionAttendeeDto(UUID userId, String publicId, String fullName, String roleTitle,
                                        String company, String bio, boolean matched) {}
    public record SelectionInput(UUID talkId, List<String> statuses) {}
    public record UpdateTalkSelectionsRequest(List<SelectionInput> selections) {}
}
