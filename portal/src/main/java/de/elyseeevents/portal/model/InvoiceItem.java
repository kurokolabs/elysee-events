package de.elyseeevents.portal.model;

public class InvoiceItem {
    private Long id;
    private Long invoiceId;
    private String description;
    private Double quantity;
    private Double unitPrice;
    private Double total;
    private String taxType; // ESSEN (7%), GETRAENKE (19%), BUEFFET (75% 7% + 25% 19%)

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getInvoiceId() { return invoiceId; }
    public void setInvoiceId(Long invoiceId) { this.invoiceId = invoiceId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getQuantity() { return quantity; }
    public void setQuantity(Double quantity) { this.quantity = quantity; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }

    public String getTaxType() { return taxType != null ? taxType : "GETRAENKE"; }
    public void setTaxType(String taxType) { this.taxType = taxType; }

    public String getTaxTypeLabel() {
        return switch (getTaxType()) {
            case "ESSEN" -> "7%";
            case "SAALMIETE" -> "19%";
            case "BUEFFET" -> "75/25";
            default -> "19%";
        };
    }
}
