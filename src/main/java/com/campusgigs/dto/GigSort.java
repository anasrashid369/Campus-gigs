package com.campusgigs.dto;

/** How the open gigs are ordered for a freelancer. The label is what a combo box shows. */
public enum GigSort {
    NEWEST("Newest first"),
    BUDGET_HIGH("Highest budget"),
    DEADLINE_SOON("Deadline soonest"),
    SKILL_MATCH("Best skill match");

    private final String label;

    GigSort(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
