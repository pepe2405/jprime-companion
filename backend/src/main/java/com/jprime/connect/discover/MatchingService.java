package com.jprime.connect.discover;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MatchingService {
    private final JdbcTemplate jdbc;
    private final IcebreakerService icebreakerService;

    public MatchingService(JdbcTemplate jdbc, IcebreakerService icebreakerService) {
        this.jdbc = jdbc;
        this.icebreakerService = icebreakerService;
    }

    public List<MatchCandidateDto> candidates(UUID currentUserId) {
        List<UUID> ids = jdbc.queryForList("""
                select u.id from users u
                where u.id <> ?
                and u.profile_completed = true
                and not exists (select 1 from swipes s where s.from_user_id = ? and s.to_user_id = u.id)
                and not exists (select 1 from matches m where (m.user1_id = ? and m.user2_id = u.id) or (m.user2_id = ? and m.user1_id = u.id))
                """, UUID.class, currentUserId, currentUserId, currentUserId, currentUserId);
        return ids.stream()
                .map(id -> candidate(currentUserId, id))
                .sorted(Comparator.comparingInt(MatchCandidateDto::score).reversed())
                .toList();
    }

    public MatchCandidateDto candidate(UUID currentUserId, UUID targetUserId) {
        var base = jdbc.query("select id, public_id, full_name, role_title, company, bio from users where id = ?", rs -> {
            rs.next();
            return new BaseUser(rs.getObject("id", UUID.class), rs.getString("public_id"), rs.getString("full_name"),
                    rs.getString("role_title"), rs.getString("company"), rs.getString("bio"));
        }, targetUserId);
        List<String> sharedInterests = names("""
                select i.name from interests i
                join user_interests a on a.interest_id = i.id and a.user_id = ?
                join user_interests b on b.interest_id = i.id and b.user_id = ?
                order by i.name
                """, currentUserId, targetUserId);
        List<String> sharedGoals = names("""
                select g.name from looking_for_goals g
                join user_looking_for_goals a on a.goal_id = g.id and a.user_id = ?
                join user_looking_for_goals b on b.goal_id = g.id and b.user_id = ?
                order by g.name
                """, currentUserId, targetUserId);
        List<SharedTalkDto> sharedTalks = jdbc.query("""
                select distinct t.id, t.title,
                case
                  when a.status = 'WANT_TO_DISCUSS' and b.status = 'WANT_TO_DISCUSS' then 'You both want to discuss this talk'
                  when a.status = 'ATTENDED' and b.status = 'ATTENDED' then 'You both attended this talk'
                  when (a.status = 'MISSED_WANT_RECAP' and b.status = 'ATTENDED') or (a.status = 'ATTENDED' and b.status = 'MISSED_WANT_RECAP') then 'One of you missed it and the other attended'
                  when a.status = 'INTERESTED' and b.status = 'INTERESTED' then 'You are both interested in this talk'
                  else 'Shared agenda interest'
                end || ' · ' || to_char(t.start_time, 'Dy HH24:MI') || ' · ' || coalesce(t.hall, 'jPrime')
                reason,
                case
                  when a.status = 'WANT_TO_DISCUSS' and b.status = 'WANT_TO_DISCUSS' then 40
                  when a.status = 'ATTENDED' and b.status = 'ATTENDED' then 25
                  when (a.status = 'MISSED_WANT_RECAP' and b.status = 'ATTENDED') or (a.status = 'ATTENDED' and b.status = 'MISSED_WANT_RECAP') then 30
                  when a.status = 'INTERESTED' and b.status = 'INTERESTED' then 15
                  else 5
                end points
                from talks t
                join user_talk_selections a on a.talk_id = t.id and a.user_id = ?
                join user_talk_selections b on b.talk_id = t.id and b.user_id = ?
                order by points desc, t.title
                """, (rs, n) -> new SharedTalkDto(rs.getObject("id", UUID.class), rs.getString("title"),
                rs.getString("reason"), rs.getInt("points")), currentUserId, targetUserId);
        int score = sharedInterests.size() * 15 + sharedGoals.size() * 10 + sharedTalks.stream().mapToInt(SharedTalkDto::points).sum();
        if (similarRole(currentUserId, base.roleTitle())) score += 10;
        if (sharedGoals.contains("Technical discussion")) score += 10;
        if (sharedGoals.contains("Coffee chat")) score += 10;
        score = Math.min(100, score);
        return new MatchCandidateDto(base.id(), base.publicId(), base.fullName(), base.roleTitle(), base.company(), base.bio(),
                score, sharedInterests, sharedGoals, sharedTalks.stream().map(t -> new SharedTalkDto(t.talkId(), t.title(), t.reason(), 0)).toList(),
                icebreakerService.icebreaker(sharedTalks, sharedInterests, sharedGoals));
    }

    private List<String> names(String sql, UUID a, UUID b) {
        return jdbc.queryForList(sql, String.class, a, b);
    }

    private boolean similarRole(UUID currentUserId, String targetRole) {
        if (targetRole == null) return false;
        String currentRole = jdbc.queryForObject("select coalesce(role_title, '') from users where id = ?", String.class, currentUserId);
        return !currentRole.isBlank() && (currentRole.toLowerCase().contains("developer") && targetRole.toLowerCase().contains("developer")
                || currentRole.toLowerCase().contains("engineer") && targetRole.toLowerCase().contains("engineer")
                || currentRole.equalsIgnoreCase(targetRole));
    }

    private record BaseUser(UUID id, String publicId, String fullName, String roleTitle, String company, String bio) {}

    public record MatchCandidateDto(UUID userId, String publicId, String fullName, String roleTitle, String company, String bio,
                                    int score, List<String> sharedInterests, List<String> sharedGoals,
                                    List<SharedTalkDto> sharedTalks, String icebreaker) {}
    public record SharedTalkDto(UUID talkId, String title, String reason, int points) {}
}
