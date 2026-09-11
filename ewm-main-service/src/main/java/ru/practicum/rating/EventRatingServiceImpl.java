package ru.practicum.rating;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.event.EventState;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.ParticipationRequestRepository;
import ru.practicum.request.RequestStatus;
import ru.practicum.user.User;
import ru.practicum.user.UserService;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventRatingServiceImpl implements EventRatingService {

    private final EventRepository eventRepository;
    private final EventLikeRepository eventLikeRepository;
    private final ParticipationRequestRepository requestRepository;
    private final UserService userService;

    @Override
    public EventRatingDto getRating(Long userId, Long eventId) {
        userService.getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);
        Boolean myVote = eventLikeRepository.findByEventIdAndUserId(eventId, userId)
                .map(EventLike::isLike)
                .orElse(null);
        return toDto(event, myVote);
    }

    @Override
    @Transactional
    public EventRatingDto like(Long userId, Long eventId) {
        return vote(userId, eventId, true);
    }

    @Override
    @Transactional
    public EventRatingDto dislike(Long userId, Long eventId) {
        return vote(userId, eventId, false);
    }

    @Override
    @Transactional
    public EventRatingDto removeVote(Long userId, Long eventId) {
        userService.getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);
        EventLike vote = eventLikeRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Rating from user with id=" + userId + " for event with id=" + eventId + " was not found"));
        applyRemoval(event, vote);
        return toDto(event, null);
    }

    @Override
    @Transactional
    public void removeVoteAdmin(Long eventId, Long userId) {
        Event event = getEventOrThrow(eventId);
        EventLike vote = eventLikeRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Rating from user with id=" + userId + " for event with id=" + eventId + " was not found"));
        applyRemoval(event, vote);
    }

    private EventRatingDto vote(Long userId, Long eventId, boolean isLike) {
        User user = userService.getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Only published events can be rated");
        }
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator cannot rate their own event");
        }
        if (!requestRepository.existsByEventIdAndRequesterIdAndStatus(eventId, userId, RequestStatus.CONFIRMED)) {
            throw new ConflictException("Only users with a confirmed participation request can rate the event");
        }

        Optional<EventLike> existing = eventLikeRepository.findByEventIdAndUserId(eventId, userId);
        if (existing.isPresent()) {
            EventLike existingVote = existing.get();
            if (existingVote.isLike() != isLike) {
                if (isLike) {
                    event.setLikesCount(event.getLikesCount() + 1);
                    event.setDislikesCount(event.getDislikesCount() - 1);
                } else {
                    event.setLikesCount(event.getLikesCount() - 1);
                    event.setDislikesCount(event.getDislikesCount() + 1);
                }
                existingVote.setLike(isLike);
                eventLikeRepository.save(existingVote);
            }
        } else {
            if (isLike) {
                event.setLikesCount(event.getLikesCount() + 1);
            } else {
                event.setDislikesCount(event.getDislikesCount() + 1);
            }
            eventLikeRepository.save(EventLike.builder()
                    .event(event)
                    .user(user)
                    .like(isLike)
                    .created(LocalDateTime.now())
                    .build());
        }
        event.setRating(event.getLikesCount() - event.getDislikesCount());
        eventRepository.save(event);
        return toDto(event, isLike);
    }

    private void applyRemoval(Event event, EventLike vote) {
        if (vote.isLike()) {
            event.setLikesCount(event.getLikesCount() - 1);
        } else {
            event.setDislikesCount(event.getDislikesCount() - 1);
        }
        event.setRating(event.getLikesCount() - event.getDislikesCount());
        eventLikeRepository.delete(vote);
        eventRepository.save(event);
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private EventRatingDto toDto(Event event, Boolean myVote) {
        return EventRatingDto.builder()
                .eventId(event.getId())
                .likes(event.getLikesCount())
                .dislikes(event.getDislikesCount())
                .rating(event.getRating())
                .myVote(myVote)
                .build();
    }
}
