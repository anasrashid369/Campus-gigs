package com.campusgigs.model;

/** A gig category (row of GigCategories). */
public record GigCategory(long categoryId, String name, boolean active) {
}
