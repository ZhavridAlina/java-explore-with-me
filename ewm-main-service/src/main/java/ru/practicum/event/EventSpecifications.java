package ru.practicum.event;

import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.request.ParticipationRequest;
import ru.practicum.request.RequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public final class EventSpecifications {

    private EventSpecifications() {
    }

    public static Specification<Event> hasState(EventState state) {
        return (root, query, cb) -> cb.equal(root.get("state"), state);
    }

    public static Specification<Event> hasStates(List<EventState> states) {
        return (root, query, cb) -> root.get("state").in(states);
    }

    public static Specification<Event> hasInitiators(List<Long> userIds) {
        return (root, query, cb) -> root.get("initiator").get("id").in(userIds);
    }

    public static Specification<Event> hasCategories(List<Long> categoryIds) {
        return (root, query, cb) -> root.get("category").get("id").in(categoryIds);
    }

    public static Specification<Event> isPaid(Boolean paid) {
        return (root, query, cb) -> cb.equal(root.get("paid"), paid);
    }

    public static Specification<Event> eventDateAfter(LocalDateTime start) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("eventDate"), start);
    }

    public static Specification<Event> eventDateBefore(LocalDateTime end) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("eventDate"), end);
    }

    public static Specification<Event> textSearch(String text) {
        return (root, query, cb) -> {
            String pattern = "%" + text.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("annotation")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    public static Specification<Event> onlyAvailable() {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<ParticipationRequest> requestRoot = subquery.from(ParticipationRequest.class);
            subquery.select(cb.count(requestRoot))
                    .where(cb.equal(requestRoot.get("event"), root),
                            cb.equal(requestRoot.get("status"), RequestStatus.CONFIRMED));
            return cb.or(
                    cb.equal(root.get("participantLimit"), 0),
                    cb.lessThan(subquery, root.get("participantLimit"))
            );
        };
    }
}
