package com.hakkushon.calendarapp.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** カレンダーそのものの削除用。(CalendarMapper を触らずに済むよう、別のマッパーにしている) */
@Mapper
public interface CalendarAdminMapper {

    /**
     * 共有カレンダーを削除する。個人用(is_personal = TRUE)は条件で弾く。削除した件数を返す。
     * 予定・コメント・空き日程調整・メンバー・招待コードは、DBの ON DELETE CASCADE でまとめて消える。
     * 通知は残り、予定への紐づきだけが外れる(ON DELETE SET NULL)。
     */
    int deleteSharedCalendar(@Param("calendarId") Long calendarId);
}
