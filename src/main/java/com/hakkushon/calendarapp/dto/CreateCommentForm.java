package com.hakkushon.calendarapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateCommentForm {

    @NotBlank(message = "コメントを入力してください")
    @Size(max = 1000, message = "コメントは1000文字以内で入力してください")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
