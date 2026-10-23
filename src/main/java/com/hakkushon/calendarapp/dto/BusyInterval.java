package com.hakkushon.calendarapp.dto;

import java.time.LocalDateTime;

/** 空き日程の計算用。「このユーザーが、この時間は予定で埋まっている」という1件分。 */
public class BusyInterval {

    private Long userId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
