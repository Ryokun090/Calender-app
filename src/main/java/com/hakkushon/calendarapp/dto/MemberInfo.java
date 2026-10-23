package com.hakkushon.calendarapp.dto;

import java.time.LocalDateTime;

/** メンバー一覧画面用。calendar_members と users を結合した表示用オブジェクト。 */
public class MemberInfo {

    private Long userId;
    private String name;
    private String role; // "owner" or "member"
    private LocalDateTime joinedAt;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    public boolean isOwner() {
        return "owner".equals(role);
    }
}
