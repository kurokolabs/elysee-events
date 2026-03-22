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
