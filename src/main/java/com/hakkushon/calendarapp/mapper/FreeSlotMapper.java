package com.hakkushon.calendarapp.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.dto.BusyInterval;

/**
 * 空き日程の自動計算用。あるカレンダーの参加メンバー全員について、
 * 「そのメンバーが参加している全カレンダー(個人用・他の共有カレンダーを含む)」の予定を、期間で絞って返す。
 * 予定のタイトルなどは取らない(「埋まっているかどうか」だけを使う)。
 */
@Mapper
public interface FreeSlotMapper {

    List<BusyInterval> findBusyIntervals(
            @Param("calendarId") Long calendarId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd);
}
