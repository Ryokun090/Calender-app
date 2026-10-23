package com.hakkushon.calendarapp.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.dto.MemberSearchResult;
import com.hakkushon.calendarapp.dto.ScheduleSearchResult;

/**
 * 検索用。どちらも「検索した本人が参加しているカレンダー」の中だけを対象にする。
 * pattern は LIKE 用に特殊文字をエスケープ済みの文字列(エスケープ文字は '!')を渡すこと。
 */
@Mapper
public interface SearchMapper {

    /** 予定のタイトル・場所・説明に含まれるものを、開始が新しい順に。 */
    List<ScheduleSearchResult> searchSchedules(@Param("userId") Long userId,
                                               @Param("pattern") String pattern,
                                               @Param("limit") int limit);

    /** 同じカレンダーに入っている人の名前に含まれるもの。 */
    List<MemberSearchResult> searchMembers(@Param("userId") Long userId,
                                           @Param("pattern") String pattern,
                                           @Param("limit") int limit);
}
