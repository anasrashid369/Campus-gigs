package com.campusgigs.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * What a client enters when posting or editing a gig. The budget is in whole tokens.
 *
 * @param skillIds the skills the gig asks for; may be empty
 */
public record GigRequest(String title, String description, long categoryId, long budget,
                         LocalDate deadline, List<Long> skillIds) {

    public GigRequest {
        skillIds = skillIds == null ? List.of() : List.copyOf(skillIds);
    }
}
