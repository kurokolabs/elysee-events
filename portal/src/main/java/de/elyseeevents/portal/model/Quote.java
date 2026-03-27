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

    private String servicePeriodFrom;
    private String servicePeriodTo;
    private String introText;
    private Double taxAmount7;
    private Double taxAmount19;
    private String recipientName;
    private String recipientCompany;
    private String recipientAddress;
    private String recipientPostalCode;
    private String recipientCity;
    private String recipientEmail;

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

    public String getServicePeriodFrom() { return servicePeriodFrom; }
    public void setServicePeriodFrom(String v) { this.servicePeriodFrom = v; }
    public String getServicePeriodTo() { return servicePeriodTo; }
    public void setServicePeriodTo(String v) { this.servicePeriodTo = v; }
    public String getIntroText() { return introText; }
    public void setIntroText(String v) { this.introText = v; }
    public Double getTaxAmount7() { return taxAmount7 != null ? taxAmount7 : 0.0; }
    public void setTaxAmount7(Double v) { this.taxAmount7 = v; }
    public Double getTaxAmount19() { return taxAmount19 != null ? taxAmount19 : 0.0; }
    public void setTaxAmount19(Double v) { this.taxAmount19 = v; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String v) { this.recipientName = v; }
    public String getRecipientCompany() { return recipientCompany; }
    public void setRecipientCompany(String v) { this.recipientCompany = v; }
    public String getRecipientAddress() { return recipientAddress; }
    public void setRecipientAddress(String v) { this.recipientAddress = v; }
    public String getRecipientPostalCode() { return recipientPostalCode; }
    public void setRecipientPostalCode(String v) { this.recipientPostalCode = v; }
    public String getRecipientCity() { return recipientCity; }
    public void setRecipientCity(String v) { this.recipientCity = v; }
    public String getRecipientEmail() { return recipientEmail; }
    public void setRecipientEmail(String v) { this.recipientEmail = v; }
    public boolean isStandaloneQuote() { return customerId == null; }
    public String getDisplayName() {
        if (customerName != null && !customerName.isBlank()) return customerName;
        if (recipientName != null && !recipientName.isBlank()) return recipientName;
        if (recipientCompany != null && !recipientCompany.isBlank()) return recipientCompany;
        return "Unbekannt";
    }

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
