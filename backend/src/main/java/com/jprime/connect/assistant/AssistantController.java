package com.jprime.connect.assistant;

import com.jprime.connect.common.CurrentUserProvider;
import com.jprime.connect.discover.MatchingService;
import jakarta.validation.Valid;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private static final Set<String> LOGISTICS_TERMS = Set.of(
            "toilet", "bathroom", "restroom", "wc", "coffee", "food", "lunch", "water", "wifi",
            "parking", "entrance", "exit", "cloakroom", "wardrobe", "registration", "where", "lost"
    );
    private static final Set<String> TALK_INTENT_TERMS = Set.of(
            "recommend", "suggest", "talk", "talks", "lecture", "lectures", "session", "sessions",
            "agenda", "attend", "speaker", "speakers", "discuss", "recap", "java", "spring",
            "kotlin", "jvm", "ai", "agents", "architecture", "security", "cloud", "frontend",
            "devops", "testing", "microservices", "database", "databases", "performance"
    );

    private final JdbcTemplate jdbc;
    private final CurrentUserProvider currentUser;
    private final MatchingService matchingService;

    public AssistantController(JdbcTemplate jdbc, CurrentUserProvider currentUser, MatchingService matchingService) {
        this.jdbc = jdbc;
        this.currentUser = currentUser;
        this.matchingService = matchingService;
    }

    @PostMapping("/recommend-talks")
    RecommendTalksResponse recommendTalks(@Valid @RequestBody RecommendTalksRequest request) {
        UUID userId = currentUser.id();
        Set<String> promptTerms = tokenize(request.prompt());
        if (!promptTerms.isEmpty() && promptTerms.stream().anyMatch(LOGISTICS_TERMS::contains)) {
            return new RecommendTalksResponse(
                    "That sounds like event logistics rather than a lecture recommendation. I do not have the venue map yet, so please follow the signs at the venue or ask the jPrime staff desk for the nearest toilet, coffee area, or hall.",
                    List.of(),
                    List.of("venue", "logistics")
            );
        }
        if (!promptTerms.isEmpty() && promptTerms.stream().noneMatch(TALK_INTENT_TERMS::contains)) {
            return new RecommendTalksResponse(
                    "I can help with talk recommendations, agenda questions, networking prompts, and simple event logistics. Try asking about a technology, speaker, session topic, or who you want to meet.",
                    List.of(),
                    List.of("agenda", "networking")
            );
        }
        Set<String> terms = new HashSet<>(promptTerms);
        terms.addAll(jdbc.queryForList("""
                select lower(i.name) from interests i
                join user_interests ui on ui.interest_id = i.id
                where ui.user_id = ?
                """, String.class, userId));
        List<TalkRecommendationDto> recommendations = jdbc.query("select * from talks order by start_time", (rs, n) -> {
            List<String> tags = Arrays.asList((String[]) rs.getArray("tags").getArray());
            String haystack = (rs.getString("title") + " " + rs.getString("description") + " " + String.join(" ", tags)).toLowerCase();
            int score = 0;
            List<String> matches = new ArrayList<>();
            for (String term : terms) {
                if (haystack.contains(term)) {
                    score += 20;
                    matches.add(term);
                }
            }
            for (String tag : tags) {
                if (terms.contains(tag.toLowerCase())) score += 30;
            }
            String reason = matches.isEmpty() ? "A useful jPrime session for broad conference networking."
                    : "Matches your profile or prompt around " + String.join(", ", matches.stream().distinct().limit(3).toList()) + ".";
            return new TalkRecommendationDto(rs.getObject("id", UUID.class), rs.getString("title"), reason, Math.min(100, score));
        }).stream().filter(r -> r.score() > 0).sorted(Comparator.comparingInt(TalkRecommendationDto::score).reversed()).limit(5).toList();
        if (recommendations.isEmpty()) {
            if (!promptTerms.isEmpty()) {
                return new RecommendTalksResponse(
                        "I could not find a strong talk match for that prompt. Try mentioning a topic like Java, Spring, AI agents, JVM performance, architecture, or security.",
                        List.of(),
                        terms.stream().filter(t -> t.length() > 3).limit(5).toList()
                );
            }
            recommendations = jdbc.query("select * from talks order by start_time limit 3", (rs, n) ->
                    new TalkRecommendationDto(rs.getObject("id", UUID.class), rs.getString("title"),
                            "A strong default pick for meeting other jPrime attendees.", 50));
        }
        return new RecommendTalksResponse("Based on your interests and prompt, these talks look most relevant.", recommendations,
                terms.stream().filter(t -> t.length() > 3).limit(5).toList());
    }

    private Set<String> tokenize(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return Set.of();
        }
        return new HashSet<>(Arrays.stream(prompt.toLowerCase().split("[^a-z0-9+]+"))
                .filter(s -> s.length() > 2)
                .toList());
    }

    @PostMapping("/icebreaker")
    IcebreakerResponse icebreaker(@Valid @RequestBody IcebreakerRequest request) {
        UUID userId = currentUser.id();
        var candidate = matchingService.candidate(userId, request.targetUserId());
        return new IcebreakerResponse(List.of(candidate.icebreaker(), "What session are you most excited to discuss next?"));
    }

    public record RecommendTalksRequest(String prompt, List<UUID> interestIds) {}
    public record RecommendTalksResponse(String message, List<TalkRecommendationDto> recommendations, List<String> suggestedProfileTags) {}
    public record TalkRecommendationDto(UUID talkId, String title, String reason, int score) {}
    public record IcebreakerRequest(UUID targetUserId) {}
    public record IcebreakerResponse(List<String> icebreakers) {}
}
