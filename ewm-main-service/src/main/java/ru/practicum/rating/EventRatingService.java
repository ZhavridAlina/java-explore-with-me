package ru.practicum.rating;

public interface EventRatingService {

    EventRatingDto getRating(Long userId, Long eventId);

    EventRatingDto like(Long userId, Long eventId);

    EventRatingDto dislike(Long userId, Long eventId);

    EventRatingDto removeVote(Long userId, Long eventId);

    void removeVoteAdmin(Long eventId, Long userId);
}
