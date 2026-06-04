package com.jprime.connect.talk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jprime.connect.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/official-agenda")
public class OfficialAgendaController {
    private final List<OfficialSessionDto> sessions;

    public OfficialAgendaController(ObjectMapper objectMapper) {
        this.sessions = loadSnapshot(objectMapper);
    }

    @GetMapping
    List<OfficialSessionDto> agenda() {
        return sessions;
    }

    private List<OfficialSessionDto> loadSnapshot(ObjectMapper objectMapper) {
        try (var input = new ClassPathResource("data/official-agenda.json").getInputStream()) {
            return objectMapper.readValue(input, new TypeReference<>() {});
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load local jPrime agenda snapshot");
        }
    }

    public record OfficialSessionDto(String id, int officialId, String hall, String title, String speaker,
                                     String description, LocalDateTime startTime, LocalDateTime endTime,
                                     String talkLevel, boolean beginnerFriendly, String format, String sourceUrl) {}
}
