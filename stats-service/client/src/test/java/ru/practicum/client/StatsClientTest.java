package ru.practicum.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StatsClientTest {

    private static final String SERVER_URL = "http://localhost:9090";

    private final StatsClient statsClient = new StatsClient(SERVER_URL, new RestTemplateBuilder());
    private final RestTemplate restTemplate =
            (RestTemplate) ReflectionTestUtils.getField(statsClient, "restTemplate");
    private final MockRestServiceServer mockServer = MockRestServiceServer.createServer(restTemplate);

    @Test
    void saveHitSendsPostRequest() {
        EndpointHitDto hit = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        mockServer.expect(requestTo(SERVER_URL + "/hit"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(org.springframework.http.HttpStatus.CREATED));

        statsClient.saveHit(hit);

        mockServer.verify();
    }

    @Test
    void saveHitDoesNotThrowWhenStatsServerFails() {
        EndpointHitDto hit = EndpointHitDto.builder()
                .app("ewm-main-service")
                .uri("/events/1")
                .ip("192.163.0.1")
                .timestamp(LocalDateTime.now())
                .build();

        mockServer.expect(requestTo(SERVER_URL + "/hit"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withServerError());

        assertThatCode(() -> statsClient.saveHit(hit)).doesNotThrowAnyException();

        mockServer.verify();
    }

    @Test
    void getStatsSendsGetRequestAndParsesResponse() {
        LocalDateTime start = LocalDateTime.of(2020, 5, 5, 0, 0, 0);
        LocalDateTime end = LocalDateTime.of(2035, 5, 5, 0, 0, 0);

        mockServer.expect(requestTo(SERVER_URL
                        + "/stats?start=2020-05-05%2000:00:00&end=2035-05-05%2000:00:00&unique=false&uris=/events/1"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(
                        "[{\"app\":\"ewm-main-service\",\"uri\":\"/events/1\",\"hits\":2}]",
                        MediaType.APPLICATION_JSON));

        List<ViewStatsDto> stats = statsClient.getStats(start, end, List.of("/events/1"), false);

        assertThat(stats).hasSize(1);
        assertThat(stats.get(0).getHits()).isEqualTo(2L);
    }
}
