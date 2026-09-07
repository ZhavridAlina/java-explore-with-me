package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {

    @Query("SELECT new ru.practicum.dto.ViewStatsDto(h.app, h.uri, COUNT(h.ip)) "
            + "FROM EndpointHit h "
            + "WHERE h.timestamp BETWEEN :start AND :end "
            + "GROUP BY h.app, h.uri "
            + "ORDER BY COUNT(h.ip) DESC")
    List<ViewStatsDto> findAllHits(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new ru.practicum.dto.ViewStatsDto(h.app, h.uri, COUNT(DISTINCT h.ip)) "
            + "FROM EndpointHit h "
            + "WHERE h.timestamp BETWEEN :start AND :end "
            + "GROUP BY h.app, h.uri "
            + "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStatsDto> findAllUniqueHits(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT new ru.practicum.dto.ViewStatsDto(h.app, h.uri, COUNT(h.ip)) "
            + "FROM EndpointHit h "
            + "WHERE h.timestamp BETWEEN :start AND :end AND h.uri IN :uris "
            + "GROUP BY h.app, h.uri "
            + "ORDER BY COUNT(h.ip) DESC")
    List<ViewStatsDto> findHitsByUris(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("uris") List<String> uris);

    @Query("SELECT new ru.practicum.dto.ViewStatsDto(h.app, h.uri, COUNT(DISTINCT h.ip)) "
            + "FROM EndpointHit h "
            + "WHERE h.timestamp BETWEEN :start AND :end AND h.uri IN :uris "
            + "GROUP BY h.app, h.uri "
            + "ORDER BY COUNT(DISTINCT h.ip) DESC")
    List<ViewStatsDto> findUniqueHitsByUris(@Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end,
                                             @Param("uris") List<String> uris);
}
