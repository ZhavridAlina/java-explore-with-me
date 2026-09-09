package ru.practicum.event;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.Category;
import ru.practicum.category.CategoryService;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.ParticipationRequestRepository;
import ru.practicum.request.RequestStatus;
import ru.practicum.user.User;
import ru.practicum.user.UserService;
import ru.practicum.util.OffsetPageRequest;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final ParticipationRequestRepository requestRepository;
    private final CategoryService categoryService;
    private final UserService userService;
    private final EventViewsService eventViewsService;

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        User initiator = userService.getUserOrThrow(userId);
        Category category = categoryService.getCategoryOrThrow(dto.getCategory());

        LocalDateTime now = LocalDateTime.now();
        if (dto.getEventDate().isBefore(now.plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours from now");
        }

        Event event = eventRepository.save(EventMapper.toEntity(dto, category, initiator, now));
        return EventMapper.toFullDto(event, 0, 0);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        userService.getUserOrThrow(userId);
        List<Event> events = eventRepository.findByInitiatorId(userId,
                OffsetPageRequest.of(from, size, Sort.by(Sort.Direction.DESC, "eventDate")));
        return toShortDtos(events);
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return toFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getState() == EventState.PUBLISHED) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        LocalDateTime effectiveEventDate = dto.getEventDate() != null ? dto.getEventDate() : event.getEventDate();
        if (effectiveEventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("Event date must be at least 2 hours from now");
        }

        applyCommonFields(event, dto.getAnnotation(), dto.getCategory(), dto.getDescription(), dto.getEventDate(),
                dto.getLocation(), dto.getPaid(), dto.getParticipantLimit(), dto.getRequestModeration(),
                dto.getTitle());

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
                default -> throw new IllegalStateException("Unexpected state action: " + dto.getStateAction());
            }
        }

        return toFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventFullDto> searchEventsAdmin(List<Long> users, List<EventState> states, List<Long> categories,
                                                 LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                 int from, int size) {
        Specification<Event> spec = Specification.where(null);
        if (users != null && !users.isEmpty()) {
            spec = spec.and(EventSpecifications.hasInitiators(users));
        }
        if (states != null && !states.isEmpty()) {
            spec = spec.and(EventSpecifications.hasStates(states));
        }
        if (categories != null && !categories.isEmpty()) {
            spec = spec.and(EventSpecifications.hasCategories(categories));
        }
        if (rangeStart != null) {
            spec = spec.and(EventSpecifications.eventDateAfter(rangeStart));
        }
        if (rangeEnd != null) {
            spec = spec.and(EventSpecifications.eventDateBefore(rangeEnd));
        }

        Page<Event> page = eventRepository.findAll(spec,
                OffsetPageRequest.of(from, size, Sort.by(Sort.Direction.DESC, "eventDate")));
        return toFullDtos(page.getContent());
    }

    @Override
    @Transactional
    public EventFullDto updateEventAdmin(Long eventId, UpdateEventAdminRequest dto) {
        Event event = getEventOrThrow(eventId);

        LocalDateTime effectiveEventDate = dto.getEventDate() != null ? dto.getEventDate() : event.getEventDate();

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case PUBLISH_EVENT -> {
                    if (event.getState() != EventState.PENDING) {
                        throw new ConflictException(
                                "Cannot publish the event because it's not in the right state: " + event.getState());
                    }
                    if (effectiveEventDate.isBefore(LocalDateTime.now().plusHours(1))) {
                        throw new ConflictException("Event date must be at least 1 hour after the publication date");
                    }
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                }
                case REJECT_EVENT -> {
                    if (event.getState() == EventState.PUBLISHED) {
                        throw new ConflictException(
                                "Cannot reject the event because it's not in the right state: " + event.getState());
                    }
                    event.setState(EventState.CANCELED);
                }
                default -> throw new IllegalStateException("Unexpected state action: " + dto.getStateAction());
            }
        }

        applyCommonFields(event, dto.getAnnotation(), dto.getCategory(), dto.getDescription(), dto.getEventDate(),
                dto.getLocation(), dto.getPaid(), dto.getParticipantLimit(), dto.getRequestModeration(),
                dto.getTitle());

        return toFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventShortDto> searchEventsPublic(String text, List<Long> categories, Boolean paid,
                                                   LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                                   boolean onlyAvailable, EventSort sort, int from, int size,
                                                   HttpServletRequest request) {
        Specification<Event> spec = Specification.where(EventSpecifications.hasState(EventState.PUBLISHED));
        if (text != null && !text.isBlank()) {
            spec = spec.and(EventSpecifications.textSearch(text));
        }
        if (categories != null && !categories.isEmpty()) {
            spec = spec.and(EventSpecifications.hasCategories(categories));
        }
        if (paid != null) {
            spec = spec.and(EventSpecifications.isPaid(paid));
        }
        if (rangeStart == null && rangeEnd == null) {
            spec = spec.and(EventSpecifications.eventDateAfter(LocalDateTime.now()));
        } else {
            if (rangeStart != null) {
                spec = spec.and(EventSpecifications.eventDateAfter(rangeStart));
            }
            if (rangeEnd != null) {
                spec = spec.and(EventSpecifications.eventDateBefore(rangeEnd));
            }
        }
        if (onlyAvailable) {
            spec = spec.and(EventSpecifications.onlyAvailable());
        }

        List<Event> pageEvents;
        Map<Long, Long> views;

        if (sort == EventSort.VIEWS) {
            // Views live outside the DB, so correct top-by-views pagination requires
            // fetching every matching row, sorting in memory, then slicing the page.
            // Acceptable for this assignment's data volumes; would need a materialized
            // view-count column to scale further.
            List<Event> all = eventRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));
            List<Long> allIds = all.stream().map(Event::getId).toList();
            views = eventViewsService.recordHitAndGetViews(request, allIds);
            List<Event> sorted = all.stream()
                    .sorted(Comparator.<Event>comparingLong(e -> views.getOrDefault(e.getId(), 0L)).reversed())
                    .toList();
            int fromIdx = Math.min(from, sorted.size());
            int toIdx = Math.min(from + size, sorted.size());
            pageEvents = sorted.subList(fromIdx, toIdx);
        } else {
            Page<Event> page = eventRepository.findAll(spec,
                    OffsetPageRequest.of(from, size, Sort.by(Sort.Direction.ASC, "eventDate")));
            pageEvents = page.getContent();
            views = eventViewsService.recordHitAndGetViews(request, pageEvents.stream().map(Event::getId).toList());
        }

        Map<Long, Long> confirmed = confirmedCounts(pageEvents.stream().map(Event::getId).toList());
        return pageEvents.stream()
                .map(e -> EventMapper.toShortDto(e, confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    @Override
    public EventFullDto getPublishedEvent(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .filter(e -> e.getState() == EventState.PUBLISHED)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        long views = eventViewsService.recordHitAndGetView(request, eventId);
        return EventMapper.toFullDto(event, confirmedRequests, views);
    }

    @Override
    public Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
    }

    private void applyCommonFields(Event event, String annotation, Long categoryId, String description,
                                    LocalDateTime eventDate, Location location, Boolean paid,
                                    Integer participantLimit, Boolean requestModeration, String title) {
        if (annotation != null) {
            event.setAnnotation(annotation);
        }
        if (categoryId != null) {
            event.setCategory(categoryService.getCategoryOrThrow(categoryId));
        }
        if (description != null) {
            event.setDescription(description);
        }
        if (eventDate != null) {
            event.setEventDate(eventDate);
        }
        if (location != null) {
            event.setLocation(location);
        }
        if (paid != null) {
            event.setPaid(paid);
        }
        if (participantLimit != null) {
            event.setParticipantLimit(participantLimit);
        }
        if (requestModeration != null) {
            event.setRequestModeration(requestModeration);
        }
        if (title != null) {
            event.setTitle(title);
        }
    }

    private EventFullDto toFullDto(Event event) {
        long confirmedRequests = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        long views = eventViewsService.getViews(List.of(event.getId())).getOrDefault(event.getId(), 0L);
        return EventMapper.toFullDto(event, confirmedRequests, views);
    }

    private List<EventFullDto> toFullDtos(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmed = confirmedCounts(ids);
        Map<Long, Long> views = eventViewsService.getViews(ids);
        return events.stream()
                .map(e -> EventMapper.toFullDto(e, confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private List<EventShortDto> toShortDtos(List<Event> events) {
        List<Long> ids = events.stream().map(Event::getId).toList();
        Map<Long, Long> confirmed = confirmedCounts(ids);
        Map<Long, Long> views = eventViewsService.getViews(ids);
        return events.stream()
                .map(e -> EventMapper.toShortDto(e, confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();
    }

    private Map<Long, Long> confirmedCounts(List<Long> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return requestRepository.countConfirmedByEventIds(eventIds).stream()
                .collect(Collectors.toMap(ParticipationRequestRepository.EventIdCount::getEventId,
                        ParticipationRequestRepository.EventIdCount::getTotal));
    }
}
