package com.hakkushon.calendarapp.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.InviteCode;

@Mapper
public interface InviteCodeMapper {

    void insert(InviteCode inviteCode);

    /** 有効期限内のコードのみ返す */
    Optional<InviteCode> findValidByCode(@Param("code") String code);
}
