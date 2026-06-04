package com.jprime.connect.user;

import com.jprime.connect.common.ApiException;
import com.jprime.connect.common.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static com.jprime.connect.common.SqlSupport.uuids;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider currentUser;

    public UserController(JdbcTemplate jdbc, CurrentUserProvider currentUser) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    UserProfileDto me() {
        return profile(currentUser.id(), true);
    }

    @PutMapping("/me")
    @Transactional
    UserProfileDto update(@Valid @RequestBody UpdateProfileRequest request) {
        UUID userId = currentUser.id();
        jdbc.update("""
                update users set full_name = ?, role_title = ?, company = ?, bio = ?, linkedin_url = ?,
                github_url = ?, profile_photo_url = ?, public_profile_enabled = ?, contact_info_visible_after_match = ?,
                profile_completed = true, updated_at = now()
                where id = ?
                """, request.fullName(), request.roleTitle(), request.company(), request.bio(), request.linkedinUrl(),
                request.githubUrl(), request.profilePhotoUrl(), request.publicProfileEnabled(),
                request.contactInfoVisibleAfterMatch(), userId);
        jdbc.update("delete from user_interests where user_id = ?", userId);
        for (UUID id : uuids(request.interestIds())) {
            jdbc.update("insert into user_interests (user_id, interest_id) values (?, ?)", userId, id);
        }
        jdbc.update("delete from user_looking_for_goals where user_id = ?", userId);
        for (UUID id : uuids(request.goalIds())) {
            jdbc.update("insert into user_looking_for_goals (user_id, goal_id) values (?, ?)", userId, id);
        }
        return profile(userId, true);
    }

    @GetMapping("/public/{publicId}")
    PublicUserProfileDto publicProfile(@PathVariable String publicId) {
        UUID id = jdbc.query("select id from users where public_id = ? and public_profile_enabled = true", rs -> {
            if (!rs.next()) {
                throw ApiException.notFound("Public profile not found");
            }
            return rs.getObject("id", UUID.class);
        }, publicId);
        UserProfileDto p = profile(id, false);
        return new PublicUserProfileDto(p.id(), p.publicId(), p.fullName(), p.roleTitle(), p.company(), p.bio(),
                p.interests(), p.goals(), p.talksToDiscuss());
    }

    private UserProfileDto profile(UUID id, boolean includePrivate) {
        return jdbc.query("""
                select id, public_id, full_name, email, role_title, company, bio, linkedin_url, github_url,
                profile_photo_url, profile_completed, public_profile_enabled, contact_info_visible_after_match
                from users where id = ?
                """, rs -> {
            if (!rs.next()) {
                throw ApiException.notFound("User not found");
            }
            return new UserProfileDto(
                    rs.getObject("id", UUID.class),
                    rs.getString("public_id"),
                    rs.getString("full_name"),
                    includePrivate ? rs.getString("email") : null,
                    rs.getString("role_title"),
                    rs.getString("company"),
                    rs.getString("bio"),
                    includePrivate ? rs.getString("linkedin_url") : null,
                    includePrivate ? rs.getString("github_url") : null,
                    rs.getString("profile_photo_url"),
                    rs.getBoolean("profile_completed"),
                    rs.getBoolean("public_profile_enabled"),
                    rs.getBoolean("contact_info_visible_after_match"),
                    jdbc.queryForList("select i.name from interests i join user_interests ui on ui.interest_id = i.id where ui.user_id = ? order by i.name", String.class, id),
                    jdbc.queryForList("select g.name from looking_for_goals g join user_looking_for_goals ug on ug.goal_id = g.id where ug.user_id = ? order by g.name", String.class, id),
                    jdbc.queryForList("select t.title from talks t join user_talk_selections uts on uts.talk_id = t.id where uts.user_id = ? and uts.status = 'WANT_TO_DISCUSS' order by t.start_time", String.class, id)
            );
        }, id);
    }

    public record UserProfileDto(UUID id, String publicId, String fullName, String email, String roleTitle,
                                 String company, String bio, String linkedinUrl, String githubUrl,
                                 String profilePhotoUrl, boolean profileCompleted, boolean publicProfileEnabled,
                                 boolean contactInfoVisibleAfterMatch, List<String> interests, List<String> goals,
                                 List<String> talksToDiscuss) {}

    public record PublicUserProfileDto(UUID id, String publicId, String fullName, String roleTitle, String company,
                                       String bio, List<String> interests, List<String> goals,
                                       List<String> talksToDiscuss) {}

    public record UpdateProfileRequest(String fullName, String roleTitle, String company, String bio,
                                       String linkedinUrl, String githubUrl, String profilePhotoUrl,
                                       Boolean publicProfileEnabled, Boolean contactInfoVisibleAfterMatch,
                                       List<UUID> interestIds, List<UUID> goalIds) {
        public Boolean publicProfileEnabled() {
            return publicProfileEnabled == null || publicProfileEnabled;
        }
        public Boolean contactInfoVisibleAfterMatch() {
            return contactInfoVisibleAfterMatch == null || contactInfoVisibleAfterMatch;
        }
    }
}
