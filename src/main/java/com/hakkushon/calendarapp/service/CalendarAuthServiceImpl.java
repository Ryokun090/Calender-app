package com.hakkushon.calendarapp.service;

import org.springframework.stereotype.Service;

import com.hakkushon.calendarapp.common.CalendarAuthService;
import com.hakkushon.calendarapp.mapper.CalendarMemberMapper;

/**
 * common.CalendarAuthService の実装。
 * 全ドメイン(B・C・E)はこのクラス経由で権限判定を行うこと。
 */
@Service
public class CalendarAuthServiceImpl implements CalendarAuthService {

    private final CalendarMemberMapper calendarMemberMapper;

    public CalendarAuthServiceImpl(CalendarMemberMapper calendarMemberMapper) {
        this.calendarMemberMapper = calendarMemberMapper;
    }

    @Override
    public boolean isOwner(Long userId, Long calendarId) {
        return calendarMemberMapper.isOwner(calendarId, userId);
    }

    @Override
    public boolean isMember(Long userId, Long calendarId) {
        return calendarMemberMapper.isMember(calendarId, userId);
    }
}
