package de.elyseeevents.portal.model;

public class Invoice {
    private Long id;
    private Long bookingId;
    private Long customerId;
    private String invoiceNumber;
    private Double amount;
    private Double taxRate;
    private Double taxAmount;
    private Double total;
    private String status;
    private String dueDate;
    private String paidDate;
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
    private String bookingType;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

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

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getPaidDate() { return paidDate; }
    public void setPaidDate(String paidDate) { this.paidDate = paidDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getBookingType() { return bookingType; }
    public void setBookingType(String bookingType) { this.bookingType = bookingType; }

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

    public boolean isStandaloneInvoice() { return customerId == null; }

    public String getDisplayName() {
        if (customerName != null && !customerName.isBlank()) return customerName;
        if (recipientName != null && !recipientName.isBlank()) return recipientName;
        if (recipientCompany != null && !recipientCompany.isBlank()) return recipientCompany;
        return "Unbekannt";
    }

    public String getBookingTypeLabel() {
        if (bookingType == null) return "-";
        try { return de.elyseeevents.portal.model.BookingType.valueOf(bookingType).getLabel(); }
        catch (IllegalArgumentException e) { return bookingType; }
    }

    public String getStatusLabel() {
        return switch (status) {
            case "OFFEN" -> "Offen";
            case "BEZAHLT" -> "Bezahlt";
            case "STORNIERT" -> "Storniert";
            default -> status;
        };
    }
}
