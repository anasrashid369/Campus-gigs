package com.campusgigs.service;

import com.campusgigs.dto.GigDto;
import com.campusgigs.dto.GigFilter;

import java.util.List;

/** Searching the open gigs as a freelancer (F22). */
public interface GigSearchService {

    /** Longest allowed search keyword, in characters. */
    int MAX_KEYWORD_LENGTH = 100;

    /**
     * Finds the open gigs that match the filter and orders them by the chosen ranking.
     * A null filter means "everything, newest first".
     *
     * @throws com.campusgigs.exception.ValidationException if the caller is not an active freelancer, the keyword is too
     *         long, or a budget limit is negative or the minimum is above the maximum
     */
    List<GigDto> search(long freelancerId, GigFilter filter);
}
