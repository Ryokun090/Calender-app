package com.hakkushon.calendarapp.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hakkushon.calendarapp.common.CalendarAuthService;
import com.hakkushon.calendarapp.domain.Schedule;
import com.hakkushon.calendarapp.dto.CreateScheduleForm;
import com.hakkushon.calendarapp.dto.EditScheduleForm;
import com.hakkushon.calendarapp.dto.ScheduleEntry;
import com.hakkushon.calendarapp.dto.ScheduleTemplate;
import com.hakkushon.calendarapp.mapper.ScheduleMapper;
import com.hakkushon.calendarapp.mapper.UserMapper;

@Service
public class ScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final CalendarAuthService calendarAuthService;
    private final NotificationService notificationService;
    private final UserMapper userMapper;

    public ScheduleService(ScheduleMapper scheduleMapper, CalendarAuthService calendarAuthService,
                            NotificationService notificationService, UserMapper userMapper) {
        this.scheduleMapper = scheduleMapper;
        this.calendarAuthService = calendarAuthService;
        this.notificationService = notificationService;
        this.userMapper = userMapper;
    }

    /**
     * 一括登録。1件でも失敗したら全件ロールバックする(開発ガイドライン8-1)。
     */
    @Transactional
    public void createSchedules(Long calendarId, Long userId, CreateScheduleForm form) {
        if (!calendarAuthService.isMember(userId, calendarId)) {
            throw new AccessDeniedException("このカレンダーに予定を登録する権限がありません");
        }

        List<Long> createdIds = new ArrayList<>();
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
            createdIds.add(schedule.getId());
        }

        // 他のメンバーへ通知(個人用カレンダーはメンバーが本人だけなので、誰にも飛ばない)
        if (!createdIds.isEmpty()) {
            String who = displayName(userId);
            String title = shorten(form.getTitle());
            String message = createdIds.size() == 1
                    ? who + "さんが「" + title + "」を追加しました"
                    : who + "さんが「" + title + "」を" + createdIds.size() + "日分追加しました";
            notificationService.notifyCalendarMembersExcept(calendarId, userId, createdIds.get(0),
                    "schedule_created", message);
        }
    }

    /**
     * 1件だけ予定を登録する(空き日程の自動計算から「この日で決める」を押したとき用)。
     * 登録したら、他のメンバーへ通知する。
     *
     * @return 作った予定のid
     */
    @Transactional
    public Long createSingle(Long calendarId, Long userId, String title, LocalDateTime start, LocalDateTime end) {
        if (!calendarAuthService.isMember(userId, calendarId)) {
            throw new AccessDeniedException("このカレンダーに予定を登録する権限がありません");
        }
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("予定のタイトルを入力してください");
        }
        if (trimmed.codePointCount(0, trimmed.length()) > 100) {
            throw new IllegalArgumentException("タイトルは100文字以内にしてください");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("終了は開始より後にしてください");
        }

        Schedule schedule = new Schedule();
        schedule.setCalendarId(calendarId);
        schedule.setTitle(trimmed);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        schedule.setCreatedBy(userId);
        if (scheduleMapper.insert(schedule) != 1) {
            throw new IllegalStateException("予定の登録に失敗しました");
        }

        notificationService.notifyCalendarMembersExcept(calendarId, userId, schedule.getId(), "schedule_created",
                displayName(userId) + "さんが「" + shorten(trimmed) + "」を追加しました");
        return schedule.getId();
    }

    /** 予定追加画面の「過去の予定から選ぶ」候補。メンバーのみ。 */
    public List<ScheduleTemplate> listTemplates(Long calendarId, Long userId) {
        if (!calendarAuthService.isMember(userId, calendarId)) {
            throw new AccessDeniedException("このカレンダーを見る権限がありません");
        }
        return scheduleMapper.findRecentTemplates(calendarId, 20);
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

        LocalDateTime newStart = LocalDateTime.of(form.getDate(), form.getStartTime());
        LocalDateTime newEnd = LocalDateTime.of(form.getDate(), form.getEndTime());
        boolean changed = !Objects.equals(schedule.getTitle(), form.getTitle())
                || !Objects.equals(blankToEmpty(schedule.getDescription()), blankToEmpty(form.getDescription()))
                || !Objects.equals(blankToEmpty(schedule.getLocation()), blankToEmpty(form.getLocation()))
                || !Objects.equals(schedule.getStartTime(), newStart)
                || !Objects.equals(schedule.getEndTime(), newEnd);

        schedule.setTitle(form.getTitle());
        schedule.setDescription(form.getDescription());
        schedule.setLocation(form.getLocation());
        schedule.setStartTime(newStart);
        schedule.setEndTime(newEnd);

        scheduleMapper.update(schedule);

        if (changed) {
            notificationService.notifyCalendarMembersExcept(schedule.getCalendarId(), userId, schedule.getId(),
                    "schedule_updated", displayName(userId) + "さんが「" + shorten(form.getTitle()) + "」を変更しました");
        }
    }

    @Transactional
    public void deleteSchedule(Long scheduleId, Long userId) {
        Schedule schedule = findOrThrow(scheduleId);
        if (!calendarAuthService.isMember(userId, schedule.getCalendarId())) {
            throw new AccessDeniedException("この予定を削除する権限がありません");
        }
        scheduleMapper.delete(scheduleId);

        // 予定そのものはもう無いので、通知には予定のidを付けない(押しても一覧に戻るだけ)
        notificationService.notifyCalendarMembersExcept(schedule.getCalendarId(), userId, null,
                "schedule_deleted", displayName(userId) + "さんが「" + shorten(schedule.getTitle()) + "」を削除しました");
    }

    private String displayName(Long userId) {
        return userMapper.findById(userId).map(u -> u.getName()).orElse("メンバー");
    }

    /** 通知メッセージ(最大255文字)に収まるよう、長いタイトルは短くする。 */
    private static String shorten(String title) {
        if (title == null) {
            return "";
        }
        int max = 40;
        if (title.codePointCount(0, title.length()) <= max) {
            return title;
        }
        return title.substring(0, title.offsetByCodePoints(0, max)) + "…";
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }

    private Schedule findOrThrow(Long scheduleId) {
        return scheduleMapper.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("予定が見つかりません: id=" + scheduleId));
    }
}
