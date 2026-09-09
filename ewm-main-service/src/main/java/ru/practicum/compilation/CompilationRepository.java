package ru.practicum.compilation;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompilationRepository extends JpaRepository<Compilation, Long> {

    @EntityGraph(attributePaths = "events")
    Optional<Compilation> findById(Long id);

    @EntityGraph(attributePaths = "events")
    List<Compilation> findByPinned(boolean pinned, Pageable pageable);

    @EntityGraph(attributePaths = "events")
    List<Compilation> findAllBy(Pageable pageable);
}
