package com.jprime.connect.forum;

import com.jprime.connect.common.ApiException;
import com.jprime.connect.common.CurrentUserProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.jprime.connect.common.SqlSupport.dateTime;

@RestController
@RequestMapping("/api/forum")
public class ForumController {
    private final JdbcTemplate jdbc;
    private final CurrentUserProvider currentUser;

    public ForumController(JdbcTemplate jdbc, CurrentUserProvider currentUser) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
    }

    @GetMapping("/topics")
    List<ForumTopicDto> topics() {
        return jdbc.query("""
                select ft.id, ft.title, ft.body, ft.category, ft.created_at,
                       u.id author_id, u.full_name author_name, u.role_title author_role_title, u.company author_company,
                       count(fc.id) comment_count
                from forum_topics ft
                join users u on u.id = ft.created_by_user_id
                left join forum_comments fc on fc.topic_id = ft.id
                group by ft.id, ft.title, ft.body, ft.category, ft.created_at, u.id, u.full_name, u.role_title, u.company
                order by ft.created_at desc
                """, (rs, n) -> new ForumTopicDto(
                rs.getObject("id", UUID.class),
                rs.getString("title"),
                rs.getString("body"),
                rs.getString("category"),
                dateTime(rs.getTimestamp("created_at")),
                rs.getObject("author_id", UUID.class),
                rs.getString("author_name"),
                rs.getString("author_role_title"),
                rs.getString("author_company"),
                rs.getInt("comment_count")
        ));
    }

    @PostMapping("/topics")
    ForumTopicDto createTopic(@RequestBody ForumTopicRequest request) {
        requireText(request.title(), "Topic title is required");
        requireText(request.body(), "Topic body is required");
        UUID userId = currentUser.id();
        UUID id = jdbc.queryForObject("""
                insert into forum_topics (title, body, category, created_by_user_id)
                values (?, ?, ?, ?)
                returning id
                """, UUID.class, request.title().trim(), request.body().trim(), category(request.category()), userId);
        return topic(id);
    }

    @GetMapping("/topics/{topicId}/comments")
    List<ForumCommentDto> comments(@PathVariable UUID topicId) {
        ensureTopic(topicId);
        return jdbc.query("""
                select fc.id, fc.topic_id, fc.body, fc.created_at,
                       u.id author_id, u.full_name author_name, u.role_title author_role_title, u.company author_company
                from forum_comments fc
                join users u on u.id = fc.created_by_user_id
                where fc.topic_id = ?
                order by fc.created_at
                """, (rs, n) -> new ForumCommentDto(
                rs.getObject("id", UUID.class),
                rs.getObject("topic_id", UUID.class),
                rs.getString("body"),
                dateTime(rs.getTimestamp("created_at")),
                rs.getObject("author_id", UUID.class),
                rs.getString("author_name"),
                rs.getString("author_role_title"),
                rs.getString("author_company")
        ), topicId);
    }

    @PostMapping("/topics/{topicId}/comments")
    ForumCommentDto createComment(@PathVariable UUID topicId, @RequestBody ForumCommentRequest request) {
        ensureTopic(topicId);
        requireText(request.body(), "Comment is required");
        UUID id = jdbc.queryForObject("""
                insert into forum_comments (topic_id, body, created_by_user_id)
                values (?, ?, ?)
                returning id
                """, UUID.class, topicId, request.body().trim(), currentUser.id());
        return comment(id);
    }

    private ForumTopicDto topic(UUID id) {
        return jdbc.query("""
                select ft.id, ft.title, ft.body, ft.category, ft.created_at,
                       u.id author_id, u.full_name author_name, u.role_title author_role_title, u.company author_company,
                       count(fc.id) comment_count
                from forum_topics ft
                join users u on u.id = ft.created_by_user_id
                left join forum_comments fc on fc.topic_id = ft.id
                where ft.id = ?
                group by ft.id, ft.title, ft.body, ft.category, ft.created_at, u.id, u.full_name, u.role_title, u.company
                """, rs -> {
            if (!rs.next()) throw ApiException.notFound("Forum topic not found");
            return new ForumTopicDto(rs.getObject("id", UUID.class), rs.getString("title"), rs.getString("body"),
                    rs.getString("category"), dateTime(rs.getTimestamp("created_at")), rs.getObject("author_id", UUID.class),
                    rs.getString("author_name"), rs.getString("author_role_title"), rs.getString("author_company"),
                    rs.getInt("comment_count"));
        }, id);
    }

    private ForumCommentDto comment(UUID id) {
        return jdbc.query("""
                select fc.id, fc.topic_id, fc.body, fc.created_at,
                       u.id author_id, u.full_name author_name, u.role_title author_role_title, u.company author_company
                from forum_comments fc
                join users u on u.id = fc.created_by_user_id
                where fc.id = ?
                """, rs -> {
            if (!rs.next()) throw ApiException.notFound("Forum comment not found");
            return new ForumCommentDto(rs.getObject("id", UUID.class), rs.getObject("topic_id", UUID.class),
                    rs.getString("body"), dateTime(rs.getTimestamp("created_at")), rs.getObject("author_id", UUID.class),
                    rs.getString("author_name"), rs.getString("author_role_title"), rs.getString("author_company"));
        }, id);
    }

    private void ensureTopic(UUID topicId) {
        Integer count = jdbc.queryForObject("select count(*) from forum_topics where id = ?", Integer.class, topicId);
        if (count == null || count == 0) throw ApiException.notFound("Forum topic not found");
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) throw ApiException.badRequest(message);
    }

    private String category(String value) {
        return value == null || value.isBlank() ? "General" : value.trim();
    }

    public record ForumTopicRequest(String title, String body, String category) {}
    public record ForumCommentRequest(String body) {}
    public record ForumTopicDto(UUID id, String title, String body, String category, LocalDateTime createdAt,
                                UUID authorId, String authorName, String authorRoleTitle, String authorCompany,
                                int commentCount) {}
    public record ForumCommentDto(UUID id, UUID topicId, String body, LocalDateTime createdAt,
                                  UUID authorId, String authorName, String authorRoleTitle, String authorCompany) {}
}
