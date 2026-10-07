package com.hakkushon.calendarapp.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.Calendar;
import com.hakkushon.calendarapp.domain.CalendarSummary;

@Mapper
public interface CalendarMapper {

    void insert(Calendar calendar);

    Optional<Calendar> findById(@Param("id") Long id);

    /**
     * ログイン中ユーザーが参加している(status=accepted)カレンダー一覧。
     * 個人用カレンダーが先頭に来るよう is_personal 降順でソートしている。
     */
    List<CalendarSummary> findCalendarsForUser(@Param("userId") Long userId);
}
