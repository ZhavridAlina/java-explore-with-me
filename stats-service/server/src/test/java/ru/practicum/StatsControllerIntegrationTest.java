package ru.practicum;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.dto.DateTimeFormat;
import ru.practicum.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatsControllerIntegrationTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DateTimeFormat.PATTERN);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void saveHitAndGetStats() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        EndpointHitDto firstHit = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .timestamp(now)
                .build();
        EndpointHitDto secondHit = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.2")
                .timestamp(now)
                .build();

        mockMvc.perform(post("/hit")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(firstHit)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/hit")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(secondHit)))
                .andExpect(status().isCreated());

        String start = now.minusHours(1).format(FORMATTER);
        String end = now.plusHours(1).format(FORMATTER);

        mockMvc.perform(get("/stats")
                        .param("start", start)
                        .param("end", end)
                        .param("uris", "/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].app").value("ewm-main-service"))
                .andExpect(jsonPath("$[0].uri").value("/events/1"))
                .andExpect(jsonPath("$[0].hits").value(2));

        mockMvc.perform(get("/stats")
                        .param("start", start)
                        .param("end", end)
                        .param("uris", "/events/1")
                        .param("unique", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].hits").value(2));
    }
}
