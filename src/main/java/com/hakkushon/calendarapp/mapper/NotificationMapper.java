package com.hakkushon.calendarapp.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.Notification;

@Mapper
public interface NotificationMapper {

    int insert(Notification notification);

    Optional<Notification> findById(@Param("id") Long id);

    /** 新しい順。未読が上に来るようにis_readの昇順を優先してから新しい順に並べる。 */
    List<Notification> findByUserId(@Param("userId") Long userId);

    int countUnread(@Param("userId") Long userId);

    int markAsRead(@Param("id") Long id);
}
