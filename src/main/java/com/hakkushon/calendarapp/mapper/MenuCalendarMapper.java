package com.hakkushon.calendarapp.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.dto.MenuCalendar;

@Mapper
public interface MenuCalendarMapper {

    /** ユーザーが参加している(accepted)カレンダー。個人用が先頭、あとはID順。 */
    List<MenuCalendar> findByUserId(@Param("userId") Long userId);
}
