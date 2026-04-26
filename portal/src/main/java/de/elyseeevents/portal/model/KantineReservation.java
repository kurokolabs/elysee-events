package de.elyseeevents.portal.model;

public class KantineReservation {
    private Long id;
    private Long customerId;
    private String name;
    private int seatCount;
    private String reservationDate;
    private String status = "OFFEN";
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSeatCount() { return seatCount; }
    public void setSeatCount(int seatCount) { this.seatCount = seatCount; }

    public String getReservationDate() { return reservationDate; }
    public void setReservationDate(String reservationDate) { this.reservationDate = reservationDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getStatusLabel() {
        return switch (status == null ? "" : status) {
            case "OFFEN" -> "Offen";
            case "BESTAETIGT" -> "Bestätigt";
            case "STORNIERT" -> "Storniert";
            default -> status;
        };
    }
}
