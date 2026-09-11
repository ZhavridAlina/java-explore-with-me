package ru.practicum.util;

import ru.practicum.dto.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeParser {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DateTimeFormat.PATTERN);

    private DateTimeParser() {
    }

    public static LocalDateTime parse(String value) {
        return value == null ? null : LocalDateTime.parse(value, FORMATTER);
    }
}
