package ru.practicum.compilation;

import ru.practicum.event.Event;
import ru.practicum.event.EventShortDto;

import java.util.List;
import java.util.Set;

public final class CompilationMapper {

    private CompilationMapper() {
    }

    public static Compilation toEntity(NewCompilationDto dto, Set<Event> events) {
        return Compilation.builder()
                .title(dto.getTitle())
                .pinned(dto.isPinned())
                .events(events)
                .build();
    }

    public static CompilationDto toDto(Compilation compilation, List<EventShortDto> events) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .title(compilation.getTitle())
                .pinned(compilation.isPinned())
                .events(events)
                .build();
    }
}
