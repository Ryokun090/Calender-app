package com.hakkushon.calendarapp.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.hakkushon.calendarapp.domain.User;

@Mapper
public interface UserMapper {

    Optional<User> findByEmail(@Param("email") String email);

    Optional<User> findById(@Param("id") Long id);

    void insert(User user);
}
