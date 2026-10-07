package com.hakkushon.calendarapp.dto;

import jakarta.validation.constraints.NotBlank;

public class JoinCalendarForm {

    @NotBlank(message = "招待コードを入力してください")
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
