package com.hakkushon.calendarapp.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.Comment;

@Mapper
public interface CommentMapper {

    int insert(Comment comment);

    /** 投稿者名(users.name)をJOINして取得する。古い順に並べる。 */
    List<Comment> findByScheduleId(@Param("scheduleId") Long scheduleId);
}
