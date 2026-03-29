package de.elyseeevents.portal.model;

public enum BookingType {
    CATERING("Catering"),
    HOCHZEIT("Hochzeit"),
    CORPORATE("Corporate");

    private final String label;

    BookingType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
