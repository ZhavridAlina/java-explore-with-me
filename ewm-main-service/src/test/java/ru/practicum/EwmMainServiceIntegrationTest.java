package ru.practicum;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.practicum.category.CategoryDto;
import ru.practicum.category.NewCategoryDto;
import ru.practicum.compilation.CompilationDto;
import ru.practicum.compilation.NewCompilationDto;
import ru.practicum.dto.DateTimeFormat;
import ru.practicum.event.EventFullDto;
import ru.practicum.event.EventShortDto;
import ru.practicum.event.EventState;
import ru.practicum.event.Location;
import ru.practicum.event.NewEventDto;
import ru.practicum.event.StateActionAdmin;
import ru.practicum.event.UpdateEventAdminRequest;
import ru.practicum.request.EventRequestStatusUpdateRequest;
import ru.practicum.request.EventRequestStatusUpdateResult;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.request.RequestStatus;
import ru.practicum.user.NewUserRequest;
import ru.practicum.user.UserDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EwmMainServiceIntegrationTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DateTimeFormat.PATTERN);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullEventLifecycle() throws Exception {
        UserDto initiator = createUser("Пётр Иванов", "petr" + System.nanoTime() + "@mail.ru");
        UserDto requester = createUser("Анна Смирнова", "anna" + System.nanoTime() + "@mail.ru");
        CategoryDto category = createCategory("Концерты" + System.nanoTime());

        NewEventDto newEvent = new NewEventDto();
        newEvent.setTitle("Летний фестиваль");
        newEvent.setAnnotation("Открытый воздух, музыка и еда на любой вкус для всей семьи");
        newEvent.setDescription("Большой летний фестиваль с концертами, едой и активностями для всей семьи и детей");
        newEvent.setCategory(category.getId());
        newEvent.setEventDate(LocalDateTime.now().plusHours(5));
        newEvent.setLocation(new Location(55.75f, 37.61f));
        newEvent.setPaid(true);
        newEvent.setParticipantLimit(1);
        newEvent.setRequestModeration(true);

        MvcResult createResult = mockMvc.perform(post("/users/{userId}/events", initiator.getId())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value(EventState.PENDING.name()))
                .andReturn();
        EventFullDto created = readDto(createResult, EventFullDto.class);

        // admin publishes the event
        UpdateEventAdminRequest publishRequest = new UpdateEventAdminRequest();
        publishRequest.setStateAction(StateActionAdmin.PUBLISH_EVENT);
        mockMvc.perform(patch("/admin/events/{eventId}", created.getId())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(publishRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value(EventState.PUBLISHED.name()));

        // publishing again must fail with exact message
        mockMvc.perform(patch("/admin/events/{eventId}", created.getId())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(publishRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Cannot publish the event because it's not in the right state: PUBLISHED"));

        // public list contains the published event
        mockMvc.perform(get("/events")
                        .param("text", "фестиваль"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(created.getId()));

        // public single event
        mockMvc.perform(get("/events/{id}", created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(created.getId()))
                .andExpect(jsonPath("$.confirmedRequests").value(0));

        // requester submits a participation request -> PENDING (moderation on, limit 1)
        MvcResult requestResult = mockMvc.perform(post("/users/{userId}/requests", requester.getId())
                        .param("eventId", created.getId().toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(RequestStatus.PENDING.name()))
                .andReturn();
        ParticipationRequestDto request = readDto(requestResult, ParticipationRequestDto.class);

        // event owner sees the pending request
        mockMvc.perform(get("/users/{userId}/events/{eventId}/requests", initiator.getId(), created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(request.getId()));

        // owner confirms it
        EventRequestStatusUpdateRequest confirmRequest = new EventRequestStatusUpdateRequest();
        confirmRequest.setRequestIds(List.of(request.getId()));
        confirmRequest.setStatus(RequestStatus.CONFIRMED);
        MvcResult confirmResult = mockMvc.perform(
                        patch("/users/{userId}/events/{eventId}/requests", initiator.getId(), created.getId())
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andReturn();
        EventRequestStatusUpdateResult updateResult = readDto(confirmResult, EventRequestStatusUpdateResult.class);
        assertThat(updateResult.getConfirmedRequests()).hasSize(1);
        assertThat(updateResult.getRejectedRequests()).isEmpty();

        // a third user cannot join a full event
        UserDto thirdUser = createUser("Олег Кузнецов", "oleg" + System.nanoTime() + "@mail.ru");
        mockMvc.perform(post("/users/{userId}/requests", thirdUser.getId())
                        .param("eventId", created.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The participant limit has been reached"));

        // event now reports 1 confirmed request
        mockMvc.perform(get("/users/{userId}/events/{eventId}", initiator.getId(), created.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmedRequests").value(1));

        // build a compilation containing the event
        NewCompilationDto newCompilation = new NewCompilationDto();
        newCompilation.setTitle("Летние события " + System.nanoTime());
        newCompilation.setPinned(true);
        newCompilation.setEvents(Set.of(created.getId()));
        MvcResult compilationResult = mockMvc.perform(post("/admin/compilations")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newCompilation)))
                .andExpect(status().isCreated())
                .andReturn();
        CompilationDto compilation = readDto(compilationResult, CompilationDto.class);
        assertThat(compilation.getEvents()).extracting(EventShortDto::getId).containsExactly(created.getId());
        assertThat(compilation.getEvents().get(0).getConfirmedRequests()).isEqualTo(1);

        mockMvc.perform(get("/compilations").param("pinned", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(compilation.getId()));

        // category still referenced by the event -> cannot be deleted
        mockMvc.perform(delete("/admin/categories/{catId}", category.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The category is not empty"));
    }

    @Test
    void creatingEventTooSoonIsRejected() throws Exception {
        UserDto initiator = createUser("Мария Орлова", "maria" + System.nanoTime() + "@mail.ru");
        CategoryDto category = createCategory("Спорт" + System.nanoTime());

        NewEventDto tooSoon = new NewEventDto();
        tooSoon.setTitle("Быстрый забег");
        tooSoon.setAnnotation("Забег стартует совсем скоро, приходите заранее, чтобы успеть размяться");
        tooSoon.setDescription("Забег стартует совсем скоро, приходите заранее, чтобы успеть размяться перед стартом");
        tooSoon.setCategory(category.getId());
        tooSoon.setEventDate(LocalDateTime.now().plusHours(1));
        tooSoon.setLocation(new Location(1f, 1f));

        mockMvc.perform(post("/users/{userId}/events", initiator.getId())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(tooSoon)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Event date must be at least 2 hours from now"));
    }

    private UserDto createUser(String name, String email) throws Exception {
        NewUserRequest request = new NewUserRequest(email, name);
        MvcResult result = mockMvc.perform(post("/admin/users")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return readDto(result, UserDto.class);
    }

    private CategoryDto createCategory(String name) throws Exception {
        NewCategoryDto request = new NewCategoryDto();
        request.setName(name);
        MvcResult result = mockMvc.perform(post("/admin/categories")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return readDto(result, CategoryDto.class);
    }

    private <T> T readDto(MvcResult result, Class<T> type) throws Exception {
        return objectMapper.readValue(result.getResponse().getContentAsString(), type);
    }
}
