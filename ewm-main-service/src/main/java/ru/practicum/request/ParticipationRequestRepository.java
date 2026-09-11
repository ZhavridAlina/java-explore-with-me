package ru.practicum.request;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findByRequesterId(Long requesterId);

    List<ParticipationRequest> findByEventId(Long eventId);

    @Query("select r from ParticipationRequest r "
            + "join fetch r.event join fetch r.requester where r.event.id = :eventId")
    List<ParticipationRequest> findByEventIdWithEventAndRequester(@Param("eventId") Long eventId);

    List<ParticipationRequest> findByIdInAndEventId(List<Long> ids, Long eventId);

    Optional<ParticipationRequest> findByIdAndRequesterId(Long id, Long requesterId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    boolean existsByEventIdAndRequesterIdAndStatus(Long eventId, Long requesterId, RequestStatus status);

    long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("select r.event.id as eventId, count(r.id) as total from ParticipationRequest r "
            + "where r.status = ru.practicum.request.RequestStatus.CONFIRMED and r.event.id in :eventIds "
            + "group by r.event.id")
    List<EventIdCount> countConfirmedByEventIds(@Param("eventIds") List<Long> eventIds);

    interface EventIdCount {
        Long getEventId();

        Long getTotal();
    }
}
