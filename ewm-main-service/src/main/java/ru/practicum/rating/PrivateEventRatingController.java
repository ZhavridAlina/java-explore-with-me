package ru.practicum.rating;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/rating")
@RequiredArgsConstructor
public class PrivateEventRatingController {

    private final EventRatingService eventRatingService;

    @GetMapping
    public EventRatingDto getRating(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventRatingService.getRating(userId, eventId);
    }

    @PutMapping("/like")
    public EventRatingDto like(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventRatingService.like(userId, eventId);
    }

    @PutMapping("/dislike")
    public EventRatingDto dislike(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventRatingService.dislike(userId, eventId);
    }

    @DeleteMapping
    public EventRatingDto removeVote(@PathVariable Long userId, @PathVariable Long eventId) {
        return eventRatingService.removeVote(userId, eventId);
    }
}
