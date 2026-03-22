package de.elyseeevents.portal.model;

public class User {
    private Long id;
    private String email;
    private String passwordHash;
    private String role;
    private boolean active;
    private boolean forcePwChange;
    private boolean twoFaEnabled;
    private String twoFaCode;
    private String twoFaExpires;
    private String createdAt;
    private String lastLogin;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isForcePwChange() { return forcePwChange; }
    public void setForcePwChange(boolean forcePwChange) { this.forcePwChange = forcePwChange; }

    public boolean isTwoFaEnabled() { return twoFaEnabled; }
    public void setTwoFaEnabled(boolean twoFaEnabled) { this.twoFaEnabled = twoFaEnabled; }

    public String getTwoFaCode() { return twoFaCode; }
    public void setTwoFaCode(String twoFaCode) { this.twoFaCode = twoFaCode; }

    public String getTwoFaExpires() { return twoFaExpires; }
    public void setTwoFaExpires(String twoFaExpires) { this.twoFaExpires = twoFaExpires; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getLastLogin() { return lastLogin; }
    public void setLastLogin(String lastLogin) { this.lastLogin = lastLogin; }
}
