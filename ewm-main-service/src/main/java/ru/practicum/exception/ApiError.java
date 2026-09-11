package ru.practicum.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import ru.practicum.dto.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class ApiError {

    private final String status;

    private final String reason;

    private final String message;

    @JsonFormat(pattern = DateTimeFormat.PATTERN)
    private final LocalDateTime timestamp;
}
