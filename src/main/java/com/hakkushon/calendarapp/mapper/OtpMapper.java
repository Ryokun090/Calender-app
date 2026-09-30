package com.hakkushon.calendarapp.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.OtpCode;

@Mapper
public interface OtpMapper {

    void insert(OtpCode otpCode);

    Optional<OtpCode> findLatestUnusedByUserId(@Param("userId") Long userId);

    void markUsed(@Param("id") Long id);
}
