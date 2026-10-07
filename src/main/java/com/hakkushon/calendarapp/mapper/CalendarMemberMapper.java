package com.hakkushon.calendarapp.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.CalendarMember;

@Mapper
public interface CalendarMemberMapper {

    void insert(CalendarMember member);

    boolean isOwner(@Param("calendarId") Long calendarId, @Param("userId") Long userId);

    boolean isMember(@Param("calendarId") Long calendarId, @Param("userId") Long userId);

    int countAcceptedMembers(@Param("calendarId") Long calendarId);

    List<Long> findMemberUserIds(@Param("calendarId") Long calendarId);
}