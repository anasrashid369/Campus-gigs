package com.campusgigs.dto;

/**
 * What a freelancer asks for when searching open gigs. A null field means "no condition";
 * the sort defaults to newest first.
 */
public record GigFilter(String keyword, Long categoryId, Long minBudget, Long maxBudget, GigSort sort) {

    public GigFilter {
        keyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        sort = sort == null ? GigSort.NEWEST : sort;
    }

    /** No conditions, newest first. */
    public static GigFilter none() {
        return new GigFilter(null, null, null, null, GigSort.NEWEST);
    }
}
