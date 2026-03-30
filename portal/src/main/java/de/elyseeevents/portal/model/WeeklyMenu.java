package de.elyseeevents.portal.model;

public class WeeklyMenu {
    private Long id;
    private String weekStart;
    private String weekEnd;
    private String monday;
    private String tuesday;
    private String wednesday;
    private String thursday;
    private String friday;
    private String mondayMeat;
    private String mondayVeg;
    private String tuesdayMeat;
    private String tuesdayVeg;
    private String wednesdayMeat;
    private String wednesdayVeg;
    private String thursdayMeat;
    private String thursdayVeg;
    private String fridayMeat;
    private String fridayVeg;
    private String mondayMeatPrice;
    private String mondayVegPrice;
    private String tuesdayMeatPrice;
    private String tuesdayVegPrice;
    private String wednesdayMeatPrice;
    private String wednesdayVegPrice;
    private String thursdayMeatPrice;
    private String thursdayVegPrice;
    private String fridayMeatPrice;
    private String fridayVegPrice;
    private String status = "ENTWURF";
    private String notes;
    private boolean sent;
    private String sentAt;
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWeekStart() { return weekStart; }
    public void setWeekStart(String weekStart) { this.weekStart = weekStart; }

    public String getWeekEnd() { return weekEnd; }
    public void setWeekEnd(String weekEnd) { this.weekEnd = weekEnd; }

    public String getMonday() { return monday; }
    public void setMonday(String monday) { this.monday = monday; }

    public String getTuesday() { return tuesday; }
    public void setTuesday(String tuesday) { this.tuesday = tuesday; }

    public String getWednesday() { return wednesday; }
    public void setWednesday(String wednesday) { this.wednesday = wednesday; }

    public String getThursday() { return thursday; }
    public void setThursday(String thursday) { this.thursday = thursday; }

    public String getFriday() { return friday; }
    public void setFriday(String friday) { this.friday = friday; }

    public String getMondayMeat() { return mondayMeat; }
    public void setMondayMeat(String mondayMeat) { this.mondayMeat = mondayMeat; }

    public String getMondayVeg() { return mondayVeg; }
    public void setMondayVeg(String mondayVeg) { this.mondayVeg = mondayVeg; }

    public String getTuesdayMeat() { return tuesdayMeat; }
    public void setTuesdayMeat(String tuesdayMeat) { this.tuesdayMeat = tuesdayMeat; }

    public String getTuesdayVeg() { return tuesdayVeg; }
    public void setTuesdayVeg(String tuesdayVeg) { this.tuesdayVeg = tuesdayVeg; }

    public String getWednesdayMeat() { return wednesdayMeat; }
    public void setWednesdayMeat(String wednesdayMeat) { this.wednesdayMeat = wednesdayMeat; }

    public String getWednesdayVeg() { return wednesdayVeg; }
    public void setWednesdayVeg(String wednesdayVeg) { this.wednesdayVeg = wednesdayVeg; }

    public String getThursdayMeat() { return thursdayMeat; }
    public void setThursdayMeat(String thursdayMeat) { this.thursdayMeat = thursdayMeat; }

    public String getThursdayVeg() { return thursdayVeg; }
    public void setThursdayVeg(String thursdayVeg) { this.thursdayVeg = thursdayVeg; }

    public String getFridayMeat() { return fridayMeat; }
    public void setFridayMeat(String fridayMeat) { this.fridayMeat = fridayMeat; }

    public String getFridayVeg() { return fridayVeg; }
    public void setFridayVeg(String fridayVeg) { this.fridayVeg = fridayVeg; }

    public String getMondayMeatPrice() { return mondayMeatPrice; }
    public void setMondayMeatPrice(String mondayMeatPrice) { this.mondayMeatPrice = mondayMeatPrice; }
    public String getMondayVegPrice() { return mondayVegPrice; }
    public void setMondayVegPrice(String mondayVegPrice) { this.mondayVegPrice = mondayVegPrice; }
    public String getTuesdayMeatPrice() { return tuesdayMeatPrice; }
    public void setTuesdayMeatPrice(String tuesdayMeatPrice) { this.tuesdayMeatPrice = tuesdayMeatPrice; }
    public String getTuesdayVegPrice() { return tuesdayVegPrice; }
    public void setTuesdayVegPrice(String tuesdayVegPrice) { this.tuesdayVegPrice = tuesdayVegPrice; }
    public String getWednesdayMeatPrice() { return wednesdayMeatPrice; }
    public void setWednesdayMeatPrice(String wednesdayMeatPrice) { this.wednesdayMeatPrice = wednesdayMeatPrice; }
    public String getWednesdayVegPrice() { return wednesdayVegPrice; }
    public void setWednesdayVegPrice(String wednesdayVegPrice) { this.wednesdayVegPrice = wednesdayVegPrice; }
    public String getThursdayMeatPrice() { return thursdayMeatPrice; }
    public void setThursdayMeatPrice(String thursdayMeatPrice) { this.thursdayMeatPrice = thursdayMeatPrice; }
    public String getThursdayVegPrice() { return thursdayVegPrice; }
    public void setThursdayVegPrice(String thursdayVegPrice) { this.thursdayVegPrice = thursdayVegPrice; }
    public String getFridayMeatPrice() { return fridayMeatPrice; }
    public void setFridayMeatPrice(String fridayMeatPrice) { this.fridayMeatPrice = fridayMeatPrice; }
    public String getFridayVegPrice() { return fridayVegPrice; }
    public void setFridayVegPrice(String fridayVegPrice) { this.fridayVegPrice = fridayVegPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusLabel() {
        try { return MenuStatus.valueOf(status).getLabel(); }
        catch (Exception e) { return status; }
    }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
