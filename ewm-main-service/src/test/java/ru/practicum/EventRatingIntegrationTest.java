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
import ru.practicum.event.EventFullDto;
import ru.practicum.event.EventState;
import ru.practicum.event.Location;
import ru.practicum.event.NewEventDto;
import ru.practicum.event.StateActionAdmin;
import ru.practicum.event.UpdateEventAdminRequest;
import ru.practicum.request.ParticipationRequestDto;
import ru.practicum.request.RequestStatus;
import ru.practicum.user.NewUserRequest;
import ru.practicum.user.UserDto;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventRatingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fullRatingLifecycle() throws Exception {
        UserDto initiator = createUser("Николай Титов", "nikolai" + System.nanoTime() + "@mail.ru");
        UserDto attendee = createUser("Ольга Белова", "olga" + System.nanoTime() + "@mail.ru");
        UserDto stranger = createUser("Виктор Панов", "viktor" + System.nanoTime() + "@mail.ru");
        CategoryDto category = createCategory("Фестивали" + System.nanoTime());

        EventFullDto event = createAndPublishEvent(initiator, category);

        // stranger who never requested to participate cannot rate the event
        mockMvc.perform(put("/users/{userId}/events/{eventId}/rating/like", stranger.getId(), event.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Only users with a confirmed participation request can rate the event"));

        // the initiator cannot rate their own event
        confirmAttendance(initiator, attendee, event.getId());
        mockMvc.perform(put("/users/{userId}/events/{eventId}/rating/like", initiator.getId(), event.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Initiator cannot rate their own event"));

        // the confirmed attendee likes the event
        mockMvc.perform(put("/users/{userId}/events/{eventId}/rating/like", attendee.getId(), event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1))
                .andExpect(jsonPath("$.dislikes").value(0))
                .andExpect(jsonPath("$.rating").value(1))
                .andExpect(jsonPath("$.myVote").value(true));

        // rating is reflected on the public event card
        mockMvc.perform(get("/events/{id}", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(1))
                .andExpect(jsonPath("$.dislikes").value(0))
                .andExpect(jsonPath("$.rating").value(1));

        // the attendee changes their mind to a dislike
        mockMvc.perform(put("/users/{userId}/events/{eventId}/rating/dislike", attendee.getId(), event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(0))
                .andExpect(jsonPath("$.dislikes").value(1))
                .andExpect(jsonPath("$.rating").value(-1))
                .andExpect(jsonPath("$.myVote").value(false));

        // own rating can be read back
        mockMvc.perform(get("/users/{userId}/events/{eventId}/rating", attendee.getId(), event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myVote").value(false));

        // the attendee retracts the vote
        mockMvc.perform(delete("/users/{userId}/events/{eventId}/rating", attendee.getId(), event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likes").value(0))
                .andExpect(jsonPath("$.dislikes").value(0))
                .andExpect(jsonPath("$.rating").value(0));

        // removing a vote that no longer exists is a not-found
        mockMvc.perform(delete("/users/{userId}/events/{eventId}/rating", attendee.getId(), event.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanRemoveAVote() throws Exception {
        UserDto initiator = createUser("Роман Седов", "roman" + System.nanoTime() + "@mail.ru");
        UserDto attendee = createUser("Дарья Крылова", "darya" + System.nanoTime() + "@mail.ru");
        CategoryDto category = createCategory("Спектакли" + System.nanoTime());

        EventFullDto event = createAndPublishEvent(initiator, category);
        confirmAttendance(initiator, attendee, event.getId());

        mockMvc.perform(put("/users/{userId}/events/{eventId}/rating/like", attendee.getId(), event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(1));

        mockMvc.perform(delete("/admin/events/{eventId}/rating/users/{userId}", event.getId(), attendee.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/events/{id}", event.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(0));
    }

    @Test
    void eventsCanBeSortedByRating() throws Exception {
        UserDto initiator = createUser("Егор Фомин", "egor" + System.nanoTime() + "@mail.ru");
        UserDto fan = createUser("Инна Гусева", "inna" + System.nanoTime() + "@mail.ru");
        CategoryDto category = createCategory("Кино" + System.nanoTime());

        EventFullDto lowRated = createAndPublishEvent(initiator, category);
        EventFullDto highRated = createAndPublishEvent(initiator, category);

        confirmAttendance(initiator, fan, highRated.getId());
        mockMvc.perform(put("/users/{userId}/events/{eventId}/rating/like", fan.getId(), highRated.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/events")
                        .param("categories", category.getId().toString())
                        .param("sort", "RATING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(highRated.getId()))
                .andExpect(jsonPath("$[1].id").value(lowRated.getId()));
    }

    private EventFullDto createAndPublishEvent(UserDto initiator, CategoryDto category) throws Exception {
        NewEventDto newEvent = new NewEventDto();
        newEvent.setTitle("Событие " + System.nanoTime());
        newEvent.setAnnotation("Аннотация к событию, которое точно пройдёт и соберёт живой отклик зрителей");
        newEvent.setDescription("Полное описание события, которое точно пройдёт и соберёт живой отклик от зрителей");
        newEvent.setCategory(category.getId());
        newEvent.setEventDate(LocalDateTime.now().plusHours(5));
        newEvent.setLocation(new Location(55.75f, 37.61f));
        newEvent.setParticipantLimit(0);
        newEvent.setRequestModeration(false);

        MvcResult createResult = mockMvc.perform(post("/users/{userId}/events", initiator.getId())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(newEvent)))
                .andExpect(status().isCreated())
                .andReturn();
        EventFullDto created = readDto(createResult, EventFullDto.class);

        UpdateEventAdminRequest publishRequest = new UpdateEventAdminRequest();
        publishRequest.setStateAction(StateActionAdmin.PUBLISH_EVENT);
        mockMvc.perform(patch("/admin/events/{eventId}", created.getId())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(publishRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value(EventState.PUBLISHED.name()));

        return created;
    }

    private void confirmAttendance(UserDto initiator, UserDto attendee, Long eventId) throws Exception {
        MvcResult requestResult = mockMvc.perform(post("/users/{userId}/requests", attendee.getId())
                        .param("eventId", eventId.toString()))
                .andExpect(status().isCreated())
                .andReturn();
        ParticipationRequestDto request = readDto(requestResult, ParticipationRequestDto.class);
        assertThat(request.getStatus()).isEqualTo(RequestStatus.CONFIRMED);
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
