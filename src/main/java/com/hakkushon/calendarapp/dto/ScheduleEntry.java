package com.hakkushon.calendarapp.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * 一括登録フォームの「日程1」「日程2」...に対応する1行分の入力。
 * 画面側の<input type="date">と<input type="time">を別々に受け取る方式(開発ガイドライン7-(3))。
 */
public class ScheduleEntry {

    @NotNull(message = "日付を入力してください")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull(message = "開始時刻を入力してください")
    private LocalTime startTime;

    @NotNull(message = "終了時刻を入力してください")
    private LocalTime endTime;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    @AssertTrue(message = "終了時刻は開始時刻より後にしてください")
    public boolean isTimeRangeValid() {
        if (startTime == null || endTime == null) {
            return true;
        }
        return endTime.isAfter(startTime);
    }
}