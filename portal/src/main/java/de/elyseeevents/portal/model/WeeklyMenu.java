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

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
