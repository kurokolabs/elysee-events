package de.elyseeevents.portal.model;

public class NewsletterSubscriber {
    private Long id;
    private String email;
    private String name;
    private boolean active;
    private String token;
    private String createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
