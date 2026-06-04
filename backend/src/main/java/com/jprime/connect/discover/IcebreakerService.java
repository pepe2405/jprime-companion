package com.jprime.connect.discover;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IcebreakerService {
    public String icebreaker(List<MatchingService.SharedTalkDto> talks, List<String> interests, List<String> goals) {
        if (!talks.isEmpty()) {
            var talk = talks.getFirst();
            if (talk.reason().contains("want to discuss")) {
                return "I saw we both want to discuss " + talk.title() + ". What did you think about it?";
            }
            if (talk.reason().contains("missed")) {
                return "I missed " + talk.title() + ". Was it worth catching up on later?";
            }
            return "What was your biggest takeaway from " + talk.title() + "?";
        }
        if (!interests.isEmpty()) {
            return "How are you using " + interests.getFirst() + " in your work?";
        }
        if (!goals.isEmpty()) {
            return "I saw we are both looking for " + goals.getFirst() + ". Want to compare notes?";
        }
        return "What has been your favorite jPrime session so far?";
    }
}
