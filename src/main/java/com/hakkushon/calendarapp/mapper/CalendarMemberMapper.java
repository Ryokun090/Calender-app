package com.hakkushon.calendarapp.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.CalendarMember;
import com.hakkushon.calendarapp.dto.MemberInfo;

@Mapper
public interface CalendarMemberMapper {

    int insert(CalendarMember member);

    boolean isOwner(@Param("calendarId") Long calendarId, @Param("userId") Long userId);

    boolean isMember(@Param("calendarId") Long calendarId, @Param("userId") Long userId);

    int countAcceptedMembers(@Param("calendarId") Long calendarId);

    /** 参加済み(accepted)メンバーのユーザーID一覧。通知の送信先などに使う。 */
    List<Long> findMemberUserIds(@Param("calendarId") Long calendarId);

    /** メンバー一覧画面用。オーナーが先頭、あとは参加が早い順。 */
    List<MemberInfo> findMembersWithUser(@Param("calendarId") Long calendarId);

    /** メンバー(role=member)を外す。オーナーは外せない。削除した件数を返す。 */
    int deleteMember(@Param("calendarId") Long calendarId, @Param("userId") Long userId);
}
