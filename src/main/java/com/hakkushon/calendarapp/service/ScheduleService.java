package com.hakkushon.calendarapp.service;

import java.time.LocalDateTime;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hakkushon.calendarapp.common.CalendarAuthService;
import com.hakkushon.calendarapp.domain.Schedule;
import com.hakkushon.calendarapp.dto.CreateScheduleForm;
import com.hakkushon.calendarapp.dto.EditScheduleForm;
import com.hakkushon.calendarapp.dto.ScheduleEntry;
import com.hakkushon.calendarapp.mapper.ScheduleMapper;

@Service
public class ScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final CalendarAuthService calendarAuthService;

    public ScheduleService(ScheduleMapper scheduleMapper, CalendarAuthService calendarAuthService) {
        this.scheduleMapper = scheduleMapper;
        this.calendarAuthService = calendarAuthService;
    }

    /**
     * 一括登録。1件でも失敗したら全件ロールバックする(開発ガイドライン8-1)。
     */
    @Transactional
    public void createSchedules(Long calendarId, Long userId, CreateScheduleForm form) {
        if (!calendarAuthService.isMember(userId, calendarId)) {
            throw new AccessDeniedException("このカレンダーに予定を登録する権限がありません");
        }

        for (ScheduleEntry entry : form.getEntries()) {
            Schedule schedule = new Schedule();
            schedule.setCalendarId(calendarId);
            schedule.setTitle(form.getTitle());
            schedule.setDescription(form.getDescription());
            schedule.setLocation(form.getLocation());
            schedule.setStartTime(LocalDateTime.of(entry.getDate(), entry.getStartTime()));
            schedule.setEndTime(LocalDateTime.of(entry.getDate(), entry.getEndTime()));
            schedule.setCreatedBy(userId);

            int inserted = scheduleMapper.insert(schedule);
            if (inserted != 1) {
                // ここで例外を投げれば@Transactionalにより既に登録した分も含めて全件ロールバックされる
                throw new IllegalStateException("予定の登録に失敗しました");
            }
        }
    }

    /** 予定詳細。カレンダーのメンバーでなければ参照不可。 */
    public Schedule getScheduleForMember(Long scheduleId, Long userId) {
        Schedule schedule = findOrThrow(scheduleId);
        if (!calendarAuthService.isMember(userId, schedule.getCalendarId())) {
            throw new AccessDeniedException("この予定を見る権限がありません");
        }
        return schedule;
    }

    @Transactional
    public void updateSchedule(Long scheduleId, Long userId, EditScheduleForm form) {
        Schedule schedule = findOrThrow(scheduleId);
        if (!calendarAuthService.isMember(userId, schedule.getCalendarId())) {
            throw new AccessDeniedException("この予定を編集する権限がありません");
        }

        schedule.setTitle(form.getTitle());
        schedule.setDescription(form.getDescription());
        schedule.setLocation(form.getLocation());
        schedule.setStartTime(LocalDateTime.of(form.getDate(), form.getStartTime()));
        schedule.setEndTime(LocalDateTime.of(form.getDate(), form.getEndTime()));

        scheduleMapper.update(schedule);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId, Long userId) {
        Schedule schedule = findOrThrow(scheduleId);
        if (!calendarAuthService.isMember(userId, schedule.getCalendarId())) {
            throw new AccessDeniedException("この予定を削除する権限がありません");
        }
        scheduleMapper.delete(scheduleId);
    }

    private Schedule findOrThrow(Long scheduleId) {
        return scheduleMapper.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("予定が見つかりません: id=" + scheduleId));
    }
}
