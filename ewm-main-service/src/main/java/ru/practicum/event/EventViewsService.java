package ru.practicum.event;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Records hits to the stats service and reads back accumulated view counts for events.
 * A fixed epoch floor is used as the {@code start} of every stats query since
 * {@link StatsClient#getStats} takes a single range for a whole batch of uris.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventViewsService {

    private static final String APP_NAME = "ewm-main-service";
    private static final LocalDateTime STATS_START = LocalDateTime.of(2000, 1, 1, 0, 0, 0);

    private final StatsClient statsClient;

    public long recordHitAndGetView(HttpServletRequest request, Long eventId) {
        return recordHitAndGetViews(request, List.of(eventId)).getOrDefault(eventId, 0L);
    }

    public Map<Long, Long> recordHitAndGetViews(HttpServletRequest request, List<Long> eventIds) {
        LocalDateTime now = LocalDateTime.now();
        saveHit(request, now);
        return getViewsBatch(eventIds, now);
    }

    public Map<Long, Long> getViews(List<Long> eventIds) {
        return getViewsBatch(eventIds, LocalDateTime.now());
    }

    private void saveHit(HttpServletRequest request, LocalDateTime timestamp) {
        EndpointHitDto hit = EndpointHitDto.builder()
                .app(APP_NAME)
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(timestamp)
                .build();
        statsClient.saveHit(hit);
    }

    private Map<Long, Long> getViewsBatch(List<Long> eventIds, LocalDateTime now) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        List<String> uris = eventIds.stream().map(id -> "/events/" + id).toList();
        try {
            List<ViewStatsDto> stats = statsClient.getStats(STATS_START, now, uris, true);
            return stats.stream().collect(Collectors.toMap(
                    dto -> Long.parseLong(dto.getUri().substring(dto.getUri().lastIndexOf('/') + 1)),
                    ViewStatsDto::getHits));
        } catch (RestClientException e) {
            log.warn("Failed to fetch views from stats server", e);
            return Map.of();
        }
    }
}
