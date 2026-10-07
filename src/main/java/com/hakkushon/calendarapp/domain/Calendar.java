package com.hakkushon.calendarapp.domain;

import java.time.LocalDateTime;

public class Calendar {

    private Long id;
    private String name;
    private Long ownerId;
    private boolean isPersonal;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public boolean isPersonal() { return isPersonal; }
    public void setPersonal(boolean personal) { isPersonal = personal; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
