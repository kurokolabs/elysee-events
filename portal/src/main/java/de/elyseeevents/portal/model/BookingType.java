package de.elyseeevents.portal.model;

public enum BookingType {
    KANTINE("Kantine"),
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
