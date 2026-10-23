package com.hakkushon.calendarapp.dto;

/** ☰メニューの「カレンダー」欄に並べる、自分が参加しているカレンダー。 */
public class MenuCalendar {

    // Figmaのカレンダーの色(青・緑・紫・オレンジ)。個人用は青、共有カレンダーはIDで残りの色を順に割り当てる。
    private static final String PERSONAL_COLOR = "#1f73f2";
    private static final String[] SHARED_COLORS = {"#29a66b", "#7a5cdb", "#eb8c2e"};

    private Long id;
    private String name;
    private boolean personal;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPersonal() {
        return personal;
    }

    public void setPersonal(boolean personal) {
        this.personal = personal;
    }

    /** 名前の横の●の色。 */
    public String getColor() {
        if (personal || id == null) {
            return PERSONAL_COLOR;
        }
        return SHARED_COLORS[(int) (id % SHARED_COLORS.length)];
    }
}
