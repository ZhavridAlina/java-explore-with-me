package ru.practicum.rating;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRatingDto {

    private Long eventId;

    private int likes;

    private int dislikes;

    private int rating;

    private Boolean myVote;
}
