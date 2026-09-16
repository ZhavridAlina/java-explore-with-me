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
public class EventFullDto {

    private Long id;

    private String title;

    private String annotation;

    private String description;

    private CategoryDto category;

    private UserShortDto initiator;

    private Location location;

    private boolean paid;

    private int participantLimit;

    private boolean requestModeration;

    private EventState state;

    @JsonFormat(pattern = DateTimeFormat.PATTERN)
    private LocalDateTime createdOn;

    @JsonFormat(pattern = DateTimeFormat.PATTERN)
    private LocalDateTime publishedOn;

    @JsonFormat(pattern = DateTimeFormat.PATTERN)
    private LocalDateTime eventDate;

    private long confirmedRequests;

    private long views;

    private int likes;

    private int dislikes;

    private int rating;
}
