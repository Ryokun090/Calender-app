package com.hakkushon.calendarapp.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.Schedule;
import com.hakkushon.calendarapp.dto.ScheduleTemplate;

/**
 * schedulesテーブルのマッパー。
 * findByCalendarAndRangeはB(カレンダー管理)が作成済み。
 * Cではinsert/findById/update/deleteを追加する。
 */
@Mapper
public interface ScheduleMapper {

    /** 指定カレンダー内で、指定期間と重なる予定を取得する(月表示で使用)。 */
    List<Schedule> findByCalendarAndRange(
            @Param("calendarId") Long calendarId,
            @Param("rangeStart") LocalDateTime rangeStart,
            @Param("rangeEnd") LocalDateTime rangeEnd);

    /** 予定を1件登録する。保存後、schedule.idに生成されたIDが入る。 */
    int insert(Schedule schedule);

    Optional<Schedule> findById(@Param("id") Long id);

    int update(Schedule schedule);

    int delete(@Param("id") Long id);

    /**
     * 「過去の予定から選ぶ」用。指定カレンダーの予定から、タイトル・場所・説明が同じものを1つにまとめ、
     * 直近に登録された順に最大limit件返す。
     */
    List<ScheduleTemplate> findRecentTemplates(
            @Param("calendarId") Long calendarId,
            @Param("limit") int limit);
}
