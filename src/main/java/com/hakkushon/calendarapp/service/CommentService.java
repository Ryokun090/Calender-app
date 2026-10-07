package com.hakkushon.calendarapp.service;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hakkushon.calendarapp.common.CalendarAuthService;
import com.hakkushon.calendarapp.domain.Comment;
import com.hakkushon.calendarapp.domain.Schedule;
import com.hakkushon.calendarapp.mapper.CommentMapper;
import com.hakkushon.calendarapp.mapper.ScheduleMapper;

@Service
public class CommentService {

    private final CommentMapper commentMapper;
    private final ScheduleMapper scheduleMapper;
    private final CalendarAuthService calendarAuthService;
    private final NotificationService notificationService;

    public CommentService(CommentMapper commentMapper,
                           ScheduleMapper scheduleMapper,
                           CalendarAuthService calendarAuthService,
                           NotificationService notificationService) {
        this.commentMapper = commentMapper;
        this.scheduleMapper = scheduleMapper;
        this.calendarAuthService = calendarAuthService;
        this.notificationService = notificationService;
    }

    public List<Comment> listForSchedule(Long scheduleId, Long userId) {
        Schedule schedule = findScheduleOrThrow(scheduleId);
        if (!calendarAuthService.isMember(userId, schedule.getCalendarId())) {
            throw new AccessDeniedException("このコメントを見る権限がありません");
        }
        return commentMapper.findByScheduleId(scheduleId);
    }

    @Transactional
    public void addComment(Long scheduleId, Long userId, String content) {
        Schedule schedule = findScheduleOrThrow(scheduleId);
        if (!calendarAuthService.isMember(userId, schedule.getCalendarId())) {
            throw new AccessDeniedException("このコメントを投稿する権限がありません");
        }

        Comment comment = new Comment();
        comment.setScheduleId(scheduleId);
        comment.setUserId(userId);
        comment.setContent(content);
        commentMapper.insert(comment);

        String message = "「" + schedule.getTitle() + "」にコメントが投稿されました";
        notificationService.notifyCalendarMembersExcept(
                schedule.getCalendarId(), userId, scheduleId, "comment", message);
    }

    private Schedule findScheduleOrThrow(Long scheduleId) {
        return scheduleMapper.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("予定が見つかりません: id=" + scheduleId));
    }
}
