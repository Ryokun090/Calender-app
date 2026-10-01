package com.hakkushon.calendarapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCalendarForm {

    @NotBlank(message = "カレンダー名を入力してください")
    @Size(max = 100, message = "カレンダー名は100文字以内で入力してください")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
