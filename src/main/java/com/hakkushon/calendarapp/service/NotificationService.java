package com.hakkushon.calendarapp.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hakkushon.calendarapp.domain.Notification;
import com.hakkushon.calendarapp.mapper.CalendarMemberMapper;
import com.hakkushon.calendarapp.mapper.NotificationMapper;

/**
 * 通知の作成・一覧・既読化。開発ガイドライン9-4に対応。
 * 「投稿・招待申請・予定確定などのタイミングでnotificationsにレコードを作成する」の
 * 作成部分をここに集約し、各ドメインのサービスから呼び出してもらう想定。
 */
@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final CalendarMemberMapper calendarMemberMapper;

    public NotificationService(NotificationMapper notificationMapper, CalendarMemberMapper calendarMemberMapper) {
        this.notificationMapper = notificationMapper;
        this.calendarMemberMapper = calendarMemberMapper;
    }

    public List<Notification> listForUser(Long userId) {
        return notificationMapper.findByUserId(userId);
    }

    public int countUnread(Long userId) {
        return notificationMapper.countUnread(userId);
    }

    /**
     * 通知を既読にして、対象のNotificationを返す(画面側で遷移先を決めるため)。
     * 他人の通知を既読にしようとした場合はAccessDeniedException。
     */
    @Transactional
    public Notification markAsReadAndGet(Long notificationId, Long userId) {
        Notification notification = notificationMapper.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("通知が見つかりません: id=" + notificationId));
        if (!notification.getUserId().equals(userId)) {
            throw new AccessDeniedException("他のユーザーの通知は操作できません");
        }
        if (!notification.isRead()) {
            notificationMapper.markAsRead(notificationId);
            notification.setRead(true);
        }
        return notification;
    }

    /**
     * カレンダーの参加メンバー全員(excludeUserIdを除く)に同じ通知を作成する。
     * コメント投稿・招待参加・予定確定などのタイミングで呼び出す想定。
     */
    @Transactional
    public void notifyCalendarMembersExcept(Long calendarId, Long excludeUserId,
                                             Long scheduleId, String type, String message) {
        List<Long> memberUserIds = calendarMemberMapper.findMemberUserIds(calendarId);
        for (Long memberUserId : memberUserIds) {
            if (memberUserId.equals(excludeUserId)) {
                continue;
            }
            Notification notification = new Notification();
            notification.setUserId(memberUserId);
            notification.setScheduleId(scheduleId);
            notification.setType(type);
            notification.setMessage(message);
            notificationMapper.insert(notification);
        }
    }
}
