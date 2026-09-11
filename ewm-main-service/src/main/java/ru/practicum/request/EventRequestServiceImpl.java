package ru.practicum.request;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.Event;
import ru.practicum.event.EventRepository;
import ru.practicum.event.EventState;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.user.User;
import ru.practicum.user.UserService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventRequestServiceImpl implements EventRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final EventRepository eventRepository;
    private final UserService userService;

    @Override
    @Transactional
    public ParticipationRequestDto createRequest(Long userId, Long eventId) {
        User requester = userService.getUserOrThrow(userId);
        Event event = eventRepository.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Initiator of the event cannot add a request to participate in it");
        }
        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Cannot participate in an unpublished event");
        }
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            throw new ConflictException("Request already exists");
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        int limit = event.getParticipantLimit();
        if (limit != 0 && confirmedCount >= limit) {
            throw new ConflictException("The participant limit has been reached");
        }

        RequestStatus status = (!event.isRequestModeration() || limit == 0)
                ? RequestStatus.CONFIRMED
                : RequestStatus.PENDING;

        ParticipationRequest request = ParticipationRequest.builder()
                .event(event)
                .requester(requester)
                .status(status)
                .created(LocalDateTime.now())
                .build();

        return RequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getOwnRequests(Long userId) {
        userService.getUserOrThrow(userId);
        return requestRepository.findByRequesterId(userId).stream().map(RequestMapper::toDto).toList();
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        ParticipationRequest request = requestRepository.findByIdAndRequesterId(requestId, userId)
                .orElseThrow(() -> new NotFoundException("Request with id=" + requestId + " was not found"));
        request.setStatus(RequestStatus.CANCELED);
        return RequestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getEventParticipants(Long userId, Long eventId) {
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));
        return requestRepository.findByEventIdWithEventAndRequester(event.getId()).stream()
                .map(RequestMapper::toDto).toList();
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult changeRequestStatus(Long userId, Long eventId,
                                                                EventRequestStatusUpdateRequest updateRequest) {
        Event event = eventRepository.findByIdForUpdate(eventId)
                .filter(e -> e.getInitiator().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        List<ParticipationRequest> requests =
                requestRepository.findByIdInAndEventId(updateRequest.getRequestIds(), eventId);
        for (ParticipationRequest request : requests) {
            if (request.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING");
            }
        }

        List<ParticipationRequestDto> confirmed = new ArrayList<>();
        List<ParticipationRequestDto> rejected = new ArrayList<>();

        if (updateRequest.getStatus() == RequestStatus.REJECTED) {
            requests.forEach(request -> request.setStatus(RequestStatus.REJECTED));
            requestRepository.saveAll(requests);
            requests.forEach(request -> rejected.add(RequestMapper.toDto(request)));
            return new EventRequestStatusUpdateResult(confirmed, rejected);
        }

        long confirmedCount = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        int limit = event.getParticipantLimit();

        for (ParticipationRequest request : requests) {
            if (limit != 0 && confirmedCount >= limit) {
                throw new ConflictException("The participant limit has been reached");
            }
            request.setStatus(RequestStatus.CONFIRMED);
            confirmed.add(RequestMapper.toDto(request));
            confirmedCount++;
        }
        requestRepository.saveAll(requests);

        if (limit != 0 && confirmedCount >= limit) {
            List<ParticipationRequest> stillPending = requestRepository.findByEventId(eventId).stream()
                    .filter(request -> request.getStatus() == RequestStatus.PENDING)
                    .toList();
            stillPending.forEach(request -> request.setStatus(RequestStatus.REJECTED));
            requestRepository.saveAll(stillPending);
            stillPending.forEach(request -> rejected.add(RequestMapper.toDto(request)));
        }

        return new EventRequestStatusUpdateResult(confirmed, rejected);
    }
}
