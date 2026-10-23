package com.hakkushon.calendarapp.dto;

/**
 * 「過去の予定から選ぶ」用の雛形。日付・時刻は含めず、タイトル・場所・説明だけを使い回す。
 */
public class ScheduleTemplate {

    private String title;
    private String location;
    private String description;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** セレクトボックスに表示する文字列(例: サークル飲み会(居酒屋))。 */
    public String getLabel() {
        if (location == null || location.isBlank()) {
            return title;
        }
        return title + "(" + location + ")";
    }
}
