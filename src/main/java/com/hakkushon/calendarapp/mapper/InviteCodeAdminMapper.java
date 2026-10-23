package com.hakkushon.calendarapp.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 招待コードの「無効化」「現在有効なコードの取得」用。
 * 既存の InviteCodeMapper(insert / findValidByCode)は変更せず、別ファイルに分けている。
 * 有効判定は findValidByCode と同じ「expires_at > NOW()」(DB側の時刻)に揃えている。
 */
@Mapper
public interface InviteCodeAdminMapper {

    /** そのカレンダーの有効なコードをすべて期限切れにする。 */
    int expireAllByCalendar(@Param("calendarId") Long calendarId);

    /** そのカレンダーで現在有効なコード(最新の1件)。無ければnull。 */
    String findActiveCode(@Param("calendarId") Long calendarId);
}
