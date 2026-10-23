package com.hakkushon.calendarapp.dto;

/** 検索結果(メンバー)1件分。「同じカレンダーに入っている人」だけが対象なので、メールアドレスは含めない。 */
public class MemberSearchResult {

    private Long userId;
    private String name;
    private Long calendarId;
    private String calendarName;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(Long calendarId) {
        this.calendarId = calendarId;
    }

    public String getCalendarName() {
        return calendarName;
    }

    public void setCalendarName(String calendarName) {
        this.calendarName = calendarName;
    }
}
