package com.hakkushon.calendarapp.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.Schedule;

@Mapper
public interface ScheduleMapper {

    /**
     * 月表示に必要な範囲検索。[rangeStart, rangeEnd) と重なる予定を全部取る
     * (「範囲重なり」方式。日をまたぐ予定も拾える)。
     *
     * C(予定登録)がこのテーブルへの作成・編集・削除を実装する。
     * ここでは月表示のための読み取りのみ。
     */
    List<Schedule> findByCalendarAndRange(@Param("calendarId") Long calendarId,
                                           @Param("rangeStart") LocalDateTime rangeStart,
                                           @Param("rangeEnd") LocalDateTime rangeEnd);
}
