package com.jprime.connect.meeting;

import com.jprime.connect.common.ApiException;
import com.jprime.connect.common.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import static com.jprime.connect.common.SqlSupport.dateTime;

@RestController
@RequestMapping("/api")
public class MeetingController {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider currentUser;

    public MeetingController(JdbcTemplate jdbc, CurrentUserProvider currentUser) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
    }

    @GetMapping("/meetings")
    List<MeetingSummaryDto> meetings() {
        UUID userId = currentUser.id();
        return jdbc.query("""
                select mt.id, mt.match_id, mt.title, mt.location, mt.start_time, mt.end_time, mt.note, mt.topic,
                       mt.status, mt.created_by_user_id, u.id other_user_id, u.full_name, u.role_title, u.company
                from meetings mt
                join matches m on m.id = mt.match_id
                join users u on u.id = case when m.user1_id = ? then m.user2_id else m.user1_id end
                where m.user1_id = ? or m.user2_id = ?
                order by mt.start_time, mt.created_at
                """, (rs, n) -> new MeetingSummaryDto(
                rs.getObject("id", UUID.class),
                rs.getObject("match_id", UUID.class),
                rs.getString("title"),
                rs.getString("location"),
                dateTime(rs.getTimestamp("start_time")),
                dateTime(rs.getTimestamp("end_time")),
                rs.getString("note"),
                rs.getString("topic"),
                rs.getString("status"),
                rs.getObject("created_by_user_id", UUID.class).equals(userId),
                rs.getObject("other_user_id", UUID.class),
                rs.getString("full_name"),
                rs.getString("role_title"),
                rs.getString("company")
        ), userId, userId, userId);
    }

    @PostMapping("/matches/{matchId}/meetings")
    MeetingDto create(@PathVariable UUID matchId, @Valid @RequestBody MeetingRequest request) {
        UUID userId = currentUser.id();
        requireMatchMember(matchId, userId);
        validateTimes(request.startTime(), request.endTime());
        UUID id = jdbc.queryForObject("""
                insert into meetings (match_id, title, location, start_time, end_time, note, topic, created_by_user_id)
                values (?, ?, ?, ?, ?, ?, ?, ?) returning id
                """, UUID.class, matchId, request.title(), request.location(), request.startTime(), request.endTime(),
                request.note(), request.topic(), userId);
        return getMeeting(id);
    }

    @GetMapping("/matches/{matchId}/meetings")
    MeetingDto byMatch(@PathVariable UUID matchId) {
        requireMatchMember(matchId, currentUser.id());
        return jdbc.query("select id from meetings where match_id = ? order by created_at desc limit 1", rs -> {
            if (!rs.next()) return null;
            return getMeeting(rs.getObject("id", UUID.class));
        }, matchId);
    }

    @PutMapping("/meetings/{meetingId}")
    MeetingDto update(@PathVariable UUID meetingId, @Valid @RequestBody MeetingRequest request) {
        UUID userId = currentUser.id();
        UUID matchId = matchIdForMeeting(meetingId);
        requireMatchMember(matchId, userId);
        validateTimes(request.startTime(), request.endTime());
        jdbc.update("""
                update meetings set title = ?, location = ?, start_time = ?, end_time = ?, note = ?, topic = ?, updated_at = now()
                where id = ?
                """, request.title(), request.location(), request.startTime(), request.endTime(), request.note(), request.topic(), meetingId);
        return getMeeting(meetingId);
    }

    @PatchMapping("/meetings/{meetingId}/status")
    MeetingDto updateStatus(@PathVariable UUID meetingId, @Valid @RequestBody MeetingStatusRequest request) {
        UUID userId = currentUser.id();
        UUID matchId = matchIdForMeeting(meetingId);
        requireMatchMember(matchId, userId);
        if (!List.of("PENDING", "ACCEPTED", "DECLINED").contains(request.status())) {
            throw ApiException.badRequest("Meeting status must be PENDING, ACCEPTED, or DECLINED");
        }
        jdbc.update("""
                update meetings set status = ?, responded_by_user_id = ?, responded_at = now(), updated_at = now()
                where id = ?
                """, request.status(), userId, meetingId);
        return getMeeting(meetingId);
    }

    @DeleteMapping("/meetings/{meetingId}")
    void delete(@PathVariable UUID meetingId) {
        UUID matchId = matchIdForMeeting(meetingId);
        requireMatchMember(matchId, currentUser.id());
        jdbc.update("delete from meetings where id = ?", meetingId);
    }

    @GetMapping("/meetings/{meetingId}/ics")
    ResponseEntity<String> ics(@PathVariable UUID meetingId) {
        UUID matchId = matchIdForMeeting(meetingId);
        requireMatchMember(matchId, currentUser.id());
        MeetingDto meeting = getMeeting(meetingId);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
        String body = """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//jPrime Connect//Meeting//EN
                BEGIN:VEVENT
                UID:%s@jprime-connect
                DTSTAMP:%s
                DTSTART:%s
                DTEND:%s
                SUMMARY:%s
                LOCATION:%s
                DESCRIPTION:%s
                END:VEVENT
                END:VCALENDAR
                """.formatted(meeting.id(), LocalDateTime.now().format(fmt), meeting.startTime().format(fmt),
                meeting.endTime().format(fmt), meeting.title(), meeting.location(),
                meeting.topic() == null ? "" : meeting.topic());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=jprime-meeting.ics")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(body);
    }

    private MeetingDto getMeeting(UUID id) {
        return jdbc.query("select * from meetings where id = ?", rs -> {
            if (!rs.next()) throw ApiException.notFound("Meeting not found");
            return new MeetingDto(rs.getObject("id", UUID.class), rs.getObject("match_id", UUID.class),
                    rs.getString("title"), rs.getString("location"), dateTime(rs.getTimestamp("start_time")),
                    dateTime(rs.getTimestamp("end_time")), rs.getString("note"), rs.getString("topic"),
                    rs.getString("status"));
        }, id);
    }

    private UUID matchIdForMeeting(UUID meetingId) {
        return jdbc.query("select match_id from meetings where id = ?", rs -> {
            if (!rs.next()) throw ApiException.notFound("Meeting not found");
            return rs.getObject("match_id", UUID.class);
        }, meetingId);
    }

    private void requireMatchMember(UUID matchId, UUID userId) {
        Integer count = jdbc.queryForObject("""
                select count(*) from matches where id = ? and (user1_id = ? or user2_id = ?)
                """, Integer.class, matchId, userId, userId);
        if (count == null || count == 0) throw ApiException.forbidden("You do not belong to this match");
    }

    private void validateTimes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw ApiException.badRequest("Meeting start time must be before end time");
        }
    }

    public record MeetingRequest(String title, String location, LocalDateTime startTime, LocalDateTime endTime,
                                 String note, String topic) {}
    public record MeetingStatusRequest(String status) {}
    public record MeetingDto(UUID id, UUID matchId, String title, String location, LocalDateTime startTime,
                             LocalDateTime endTime, String note, String topic, String status) {}
    public record MeetingSummaryDto(UUID id, UUID matchId, String title, String location, LocalDateTime startTime,
                                   LocalDateTime endTime, String note, String topic, String status,
                                   boolean createdByCurrentUser, UUID otherUserId, String otherUserName,
                                   String otherUserRoleTitle, String otherUserCompany) {}
}
