package ru.practicum.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.category.CategoryDto;
import ru.practicum.dto.DateTimeFormat;
import ru.practicum.user.UserShortDto;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventShortDto {

    private Long id;

    private String title;

    private String annotation;

    private CategoryDto category;

    private UserShortDto initiator;

    private boolean paid;

    @JsonFormat(pattern = DateTimeFormat.PATTERN)
    private LocalDateTime eventDate;

    private long confirmedRequests;

    private long views;
}
