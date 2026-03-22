package de.elyseeevents.portal.model;

public class Quote {
    private Long id;
    private Long customerId;
    private Long bookingId;
    private String quoteNumber;
    private Double amount;
    private Double taxRate;
    private Double taxAmount;
    private Double total;
    private String status;
    private String validUntil;
    private String notes;
    private String createdAt;

    // Transient
    private String customerName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public String getQuoteNumber() { return quoteNumber; }
    public void setQuoteNumber(String quoteNumber) { this.quoteNumber = quoteNumber; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Double getTaxRate() { return taxRate; }
    public void setTaxRate(Double taxRate) { this.taxRate = taxRate; }

    public Double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(Double taxAmount) { this.taxAmount = taxAmount; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getValidUntil() { return validUntil; }
    public void setValidUntil(String validUntil) { this.validUntil = validUntil; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getStatusLabel() {
        if (status == null) return "-";
        return switch (status) {
            case "OFFEN" -> "Offen";
            case "ANGENOMMEN" -> "Angenommen";
            case "ABGELEHNT" -> "Abgelehnt";
            case "ABGELAUFEN" -> "Abgelaufen";
            default -> status;
        };
    }
}
