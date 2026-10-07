package com.hakkushon.calendarapp.domain;

public class CalendarSummary {

    private Long id;
    private String name;
    private boolean isPersonal;
    private String role;
    private int memberCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isPersonal() { return isPersonal; }
    public void setPersonal(boolean personal) { isPersonal = personal; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public int getMemberCount() { return memberCount; }
    public void setMemberCount(int memberCount) { this.memberCount = memberCount; }
}
