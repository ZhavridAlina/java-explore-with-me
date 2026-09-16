package ru.practicum.rating;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/events/{eventId}/rating")
@RequiredArgsConstructor
public class AdminEventRatingController {

    private final EventRatingService eventRatingService;

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeVote(@PathVariable Long eventId, @PathVariable Long userId) {
        eventRatingService.removeVoteAdmin(eventId, userId);
    }
}
