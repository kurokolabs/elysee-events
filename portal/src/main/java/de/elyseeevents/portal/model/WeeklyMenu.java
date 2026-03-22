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

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isSent() { return sent; }
    public void setSent(boolean sent) { this.sent = sent; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
