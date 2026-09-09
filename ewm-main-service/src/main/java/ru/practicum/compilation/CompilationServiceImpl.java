package ru.practicum.compilation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.event.Event;
import ru.practicum.event.EventMapper;
import ru.practicum.event.EventService;
import ru.practicum.event.EventShortDto;
import ru.practicum.event.EventViewsService;
import ru.practicum.exception.NotFoundException;
import ru.practicum.request.ParticipationRequestRepository;
import ru.practicum.util.OffsetPageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventService eventService;
    private final ParticipationRequestRepository requestRepository;
    private final EventViewsService eventViewsService;

    @Override
    @Transactional
    public CompilationDto createCompilation(NewCompilationDto request) {
        Set<Event> events = resolveEvents(request.getEvents());
        Compilation saved = compilationRepository.save(CompilationMapper.toEntity(request, events));
        return toDto(saved);
    }

    @Override
    @Transactional
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequest request) {
        Compilation compilation = getCompilationOrThrow(compId);
        if (request.getEvents() != null) {
            compilation.setEvents(resolveEvents(request.getEvents()));
        }
        if (request.getPinned() != null) {
            compilation.setPinned(request.getPinned());
        }
        if (request.getTitle() != null) {
            compilation.setTitle(request.getTitle());
        }
        return toDto(compilationRepository.save(compilation));
    }

    @Override
    @Transactional
    public void deleteCompilation(Long compId) {
        Compilation compilation = getCompilationOrThrow(compId);
        compilationRepository.delete(compilation);
    }

    @Override
    public List<CompilationDto> getCompilations(Boolean pinned, int from, int size) {
        OffsetPageRequest pageable = OffsetPageRequest.of(from, size);
        List<Compilation> compilations = pinned == null
                ? compilationRepository.findAllBy(pageable)
                : compilationRepository.findByPinned(pinned, pageable);
        return compilations.stream().map(this::toDto).toList();
    }

    @Override
    public CompilationDto getCompilation(Long compId) {
        return toDto(getCompilationOrThrow(compId));
    }

    private Compilation getCompilationOrThrow(Long compId) {
        return compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation with id=" + compId + " was not found"));
    }

    private Set<Event> resolveEvents(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new HashSet<>();
        }
        return eventIds.stream().map(eventService::getEventOrThrow).collect(Collectors.toSet());
    }

    private CompilationDto toDto(Compilation compilation) {
        List<Long> ids = compilation.getEvents().stream().map(Event::getId).toList();
        Map<Long, Long> confirmed = ids.isEmpty() ? Map.of()
                : requestRepository.countConfirmedByEventIds(ids).stream()
                        .collect(Collectors.toMap(ParticipationRequestRepository.EventIdCount::getEventId,
                                ParticipationRequestRepository.EventIdCount::getTotal));
        Map<Long, Long> views = eventViewsService.getViews(ids);

        List<EventShortDto> eventDtos = compilation.getEvents().stream()
                .map(e -> EventMapper.toShortDto(e, confirmed.getOrDefault(e.getId(), 0L),
                        views.getOrDefault(e.getId(), 0L)))
                .toList();

        return CompilationMapper.toDto(compilation, eventDtos);
    }
}
