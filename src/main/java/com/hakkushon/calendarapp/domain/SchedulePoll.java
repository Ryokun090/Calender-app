package com.hakkushon.calendarapp.domain;

import java.time.LocalDateTime;

/**
 * 空き日程調整(調整さん型)の候補セット本体(schedule_pollsテーブル)。
 * statusが"confirmed"になると、confirmedScheduleIdにschedulesの行IDが入る。
 */
public class SchedulePoll {

    public static final String STATUS_OPEN = "open";
    public static final String STATUS_CONFIRMED = "confirmed";

    private Long id;
    private Long calendarId;
    private String title;
    private String description;
    private String location;
    private String status; // "open" or "confirmed"
    private Long confirmedScheduleId;
    private Long createdBy;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(Long calendarId) {
        this.calendarId = calendarId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isOpen() {
        return STATUS_OPEN.equals(status);
    }

    public Long getConfirmedScheduleId() {
        return confirmedScheduleId;
    }

    public void setConfirmedScheduleId(Long confirmedScheduleId) {
        this.confirmedScheduleId = confirmedScheduleId;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
