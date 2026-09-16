package ru.practicum.event;

import ru.practicum.category.Category;
import ru.practicum.category.CategoryMapper;
import ru.practicum.user.User;
import ru.practicum.user.UserMapper;

import java.time.LocalDateTime;

public final class EventMapper {

    private EventMapper() {
    }

    public static Event toEntity(NewEventDto dto, Category category, User initiator, LocalDateTime now) {
        return Event.builder()
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .category(category)
                .initiator(initiator)
                .location(dto.getLocation())
                .paid(dto.isPaid())
                .participantLimit(dto.getParticipantLimit())
                .requestModeration(dto.isRequestModerationOrDefault())
                .state(EventState.PENDING)
                .createdOn(now)
                .eventDate(dto.getEventDate())
                .build();
    }

    public static EventFullDto toFullDto(Event event, long confirmedRequests, long views) {
        return EventFullDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .description(event.getDescription())
                .category(CategoryMapper.toDto(event.getCategory()))
                .initiator(UserMapper.toShortDto(event.getInitiator()))
                .location(event.getLocation())
                .paid(event.isPaid())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.isRequestModeration())
                .state(event.getState())
                .createdOn(event.getCreatedOn())
                .publishedOn(event.getPublishedOn())
                .eventDate(event.getEventDate())
                .confirmedRequests(confirmedRequests)
                .views(views)
                .likes(event.getLikesCount())
                .dislikes(event.getDislikesCount())
                .rating(event.getRating())
                .build();
    }

    public static EventShortDto toShortDto(Event event, long confirmedRequests, long views) {
        return EventShortDto.builder()
                .id(event.getId())
                .title(event.getTitle())
                .annotation(event.getAnnotation())
                .category(CategoryMapper.toDto(event.getCategory()))
                .initiator(UserMapper.toShortDto(event.getInitiator()))
                .paid(event.isPaid())
                .eventDate(event.getEventDate())
                .confirmedRequests(confirmedRequests)
                .views(views)
                .likes(event.getLikesCount())
                .dislikes(event.getDislikesCount())
                .rating(event.getRating())
                .build();
    }
}
