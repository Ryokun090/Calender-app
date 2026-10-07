package com.hakkushon.calendarapp.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * 空き日程調整(調整さん型)の候補作成フォーム。
 * 一括登録(ScheduleEntry)と同じ「日付+時刻」の行を複数持てる構造を流用する。
 */
public class CreatePollForm {

    @NotBlank(message = "タイトルを入力してください")
    @Size(max = 100, message = "タイトルは100文字以内で入力してください")
    private String title;

    @Size(max = 2000, message = "説明は2000文字以内で入力してください")
    private String description;

    @Size(max = 200, message = "場所は200文字以内で入力してください")
    private String location;

    @NotEmpty(message = "候補日程を1件以上入力してください")
    @Valid
    private List<ScheduleEntry> candidates = new ArrayList<>();

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

    public List<ScheduleEntry> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<ScheduleEntry> candidates) {
        this.candidates = candidates;
    }
}
