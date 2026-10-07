package com.hakkushon.calendarapp.domain;

import java.time.LocalDateTime;

public class InviteCode {

    private Long id;
    private Long calendarId;
    private String code;
    private Long createdBy;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCalendarId() { return calendarId; }
    public void setCalendarId(Long calendarId) { this.calendarId = calendarId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
