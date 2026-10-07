package com.hakkushon.calendarapp.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.SchedulePoll;

@Mapper
public interface SchedulePollMapper {

    int insert(SchedulePoll poll);

    Optional<SchedulePoll> findById(@Param("id") Long id);

    List<SchedulePoll> findByCalendarId(@Param("calendarId") Long calendarId);

    /** 確定処理。statusを'confirmed'にし、作成されたschedulesの行IDをセットする。 */
    int confirm(@Param("id") Long id, @Param("confirmedScheduleId") Long confirmedScheduleId);
}
