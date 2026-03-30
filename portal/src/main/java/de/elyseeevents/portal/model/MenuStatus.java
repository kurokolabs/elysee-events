package de.elyseeevents.portal.model;

public enum MenuStatus {
    ENTWURF("Entwurf"),
    BESTAETIGT("Bestätigt"),
    VERSENDET("Versendet");

    private final String label;

    MenuStatus(String label) {
        this.label = label;
    }

    public String getLabel() { return label; }
}
