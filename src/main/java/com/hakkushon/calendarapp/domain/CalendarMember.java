package com.hakkushon.calendarapp.domain;

import java.time.LocalDateTime;

public class CalendarMember {

    private Long calendarId;
    private Long userId;
    private String role;     // "owner" or "member"
    private String status;   // "pending" or "accepted"
    private LocalDateTime joinedAt;

    public Long getCalendarId() { return calendarId; }
    public void setCalendarId(Long calendarId) { this.calendarId = calendarId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getJoinedAt() { return joinedAt; }
    public void setJoinedAt(LocalDateTime joinedAt) { this.joinedAt = joinedAt; }
}
