package de.elyseeevents.portal.model;

public enum BookingStatus {
    ANFRAGE("Anfrage", "#C9A84C"),
    BESTAETIGT("Bestätigt", "#2E7D32"),
    IN_PLANUNG("In Planung", "#1565C0"),
    ABGESCHLOSSEN("Abgeschlossen", "#6B6560"),
    STORNIERT("Storniert", "#C62828");

    private final String label;
    private final String color;

    BookingStatus(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public String getColor() {
        return color;
    }
}
