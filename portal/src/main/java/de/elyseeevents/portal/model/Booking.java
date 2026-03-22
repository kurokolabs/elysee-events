package de.elyseeevents.portal.model;

public class Booking {
    private Long id;
    private Long customerId;
    private String bookingType;
    private String status;
    private String eventDate;
    private String eventTimeSlot;
    private Integer guestCount;
    private Double budget;
    private String menuSelection;
    private String specialRequests;
    private String adminNotes;
    private String deliveryAddress;
    private String cateringPackage;
    private String foodOption;
    private String foodSubOption;
    private String cuisineStyle;
    private String createdAt;
    private String updatedAt;

    // Transient fields for display
    private String customerName;
    private String customerCompany;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getBookingType() { return bookingType; }
    public void setBookingType(String bookingType) { this.bookingType = bookingType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    public String getEventTimeSlot() { return eventTimeSlot; }
    public void setEventTimeSlot(String eventTimeSlot) { this.eventTimeSlot = eventTimeSlot; }

    public Integer getGuestCount() { return guestCount; }
    public void setGuestCount(Integer guestCount) { this.guestCount = guestCount; }

    public Double getBudget() { return budget; }
    public void setBudget(Double budget) { this.budget = budget; }

    public String getMenuSelection() { return menuSelection; }
    public void setMenuSelection(String menuSelection) { this.menuSelection = menuSelection; }

    public String getSpecialRequests() { return specialRequests; }
    public void setSpecialRequests(String specialRequests) { this.specialRequests = specialRequests; }

    public String getAdminNotes() { return adminNotes; }
    public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getCateringPackage() { return cateringPackage; }
    public void setCateringPackage(String cateringPackage) { this.cateringPackage = cateringPackage; }

    public String getFoodOption() { return foodOption; }
    public void setFoodOption(String foodOption) { this.foodOption = foodOption; }

    public String getFoodSubOption() { return foodSubOption; }
    public void setFoodSubOption(String foodSubOption) { this.foodSubOption = foodSubOption; }

    public String getCuisineStyle() { return cuisineStyle; }
    public void setCuisineStyle(String cuisineStyle) { this.cuisineStyle = cuisineStyle; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerCompany() { return customerCompany; }
    public void setCustomerCompany(String customerCompany) { this.customerCompany = customerCompany; }

    public BookingType getBookingTypeEnum() {
        if (bookingType == null) return BookingType.KANTINE;
        try { return BookingType.valueOf(bookingType); }
        catch (IllegalArgumentException e) { return BookingType.KANTINE; }
    }

    public BookingStatus getStatusEnum() {
        if (status == null) return BookingStatus.ANFRAGE;
        try { return BookingStatus.valueOf(status); }
        catch (IllegalArgumentException e) { return BookingStatus.ANFRAGE; }
    }
}
