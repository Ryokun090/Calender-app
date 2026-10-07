package com.hakkushon.calendarapp.domain;

import java.time.LocalDateTime;

/**
 * 通知(notificationsテーブル)。
 * scheduleIdは元の予定が削除されるとNULLになる(ON DELETE SET NULL)ので、通知文言(message)に
 * 必要な情報をあらかじめ埋め込んでおき、予定が消えても意味が通るようにしてある。
 */
public class Notification {

    private Long id;
    private Long userId;
    private Long scheduleId; // nullable
    private String type;     // "comment" / "invite_joined" / "poll_confirmed" など
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
