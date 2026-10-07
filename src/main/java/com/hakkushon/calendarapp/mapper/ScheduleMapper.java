package com.hakkushon.calendarapp.mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.Schedule;

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
}
