package com.jprime.connect.discover;

import com.jprime.connect.common.CurrentUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/discover")
public class DiscoverController {
    private final MatchingService matchingService;
    private final CurrentUserProvider currentUser;

    public DiscoverController(MatchingService matchingService, CurrentUserProvider currentUser) {
        this.matchingService = matchingService;
        this.currentUser = currentUser;
    }

    @GetMapping
    List<MatchingService.MatchCandidateDto> discover() {
        return matchingService.candidates(currentUser.id());
    }
}
