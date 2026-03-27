package de.elyseeevents.portal.model;

public class QuoteItem {
    private Long id;
    private Long quoteId;
    private String description;
    private Double quantity;
    private Double unitPrice;
    private Double total;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getQuoteId() { return quoteId; }
    public void setQuoteId(Long quoteId) { this.quoteId = quoteId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    private String taxType;
    public String getTaxType() { return taxType != null ? taxType : "GETRAENKE"; }
    public void setTaxType(String taxType) { this.taxType = taxType; }
    public String getTaxTypeLabel() {
        return switch (getTaxType()) {
            case "ESSEN" -> "Essen (7%)";
            case "SAALMIETE" -> "Saalmiete (19%)";
            case "BUEFFET" -> "Büffet (75/25)";
            default -> "Getränke (19%)";
        };
    }
}
