package ru.practicum.rating;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventLikeRepository extends JpaRepository<EventLike, Long> {

    Optional<EventLike> findByEventIdAndUserId(Long eventId, Long userId);
}
