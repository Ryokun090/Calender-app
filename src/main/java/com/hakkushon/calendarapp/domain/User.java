package com.hakkushon.calendarapp.domain;

import java.time.LocalDateTime;

public class User {

    private Long id;
    private String name;
    private String email;
    private String passwordHash;
    private boolean is2faEnabled;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public boolean isIs2faEnabled() { return is2faEnabled; }
    public void setIs2faEnabled(boolean is2faEnabled) { this.is2faEnabled = is2faEnabled; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
