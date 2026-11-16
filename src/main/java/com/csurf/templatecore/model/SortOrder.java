package com.csurf.templatecore.model;

/**
 * Enum for specifying the sort order of DrawObj lists in display components.
 * Each sort order has a display label for potential UI use.
 */
public enum SortOrder {
    ALPHABETICAL_AZ("A-Z"),
    ALPHABETICAL_ZA("Z-A"),
    ORIGINAL_ORDER("original Template order");

    private final String displayLabel;

    SortOrder(String displayLabel) {
        this.displayLabel = displayLabel;
    }

    /**
     * Gets the display label for this sort order.
     * @return The display label
     */
    public String getDisplayLabel() {
        return displayLabel;
    }
}
