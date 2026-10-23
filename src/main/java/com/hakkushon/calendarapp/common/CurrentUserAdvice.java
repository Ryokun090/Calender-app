package com.hakkushon.calendarapp.common;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

import com.hakkushon.calendarapp.dto.MenuCalendar;
import com.hakkushon.calendarapp.mapper.MenuCalendarMapper;
import com.hakkushon.calendarapp.service.NotificationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * 全画面のモデルに、共通ヘッダー(fragments/header.html)が使う値を入れる。
 *   loginEmail  : ログイン中のメールアドレス(未ログインならnull)
 *   unreadCount : 未読の通知の件数(未ログインなら0)
 *   menuCalendars: ☰メニューの「カレンダー」欄に並べる、参加中のカレンダー一覧
 *   lastCalendarId: 最後に開いたカレンダーのid(無ければnull)
 *   navActive   : ヘッダーのメニューで強調する項目
 *                 ("home" / "calendar" / "freeslots" / "schedule-add" / "members" / "notifications" / "settings" / "search")
 */
@ControllerAdvice
public class CurrentUserAdvice {

    /** 「最後に開いたカレンダー」をセッションに覚えておくときのキー。 */
    public static final String LAST_CALENDAR_ID = "LAST_CALENDAR_ID";

    private final NotificationService notificationService;
    private final MenuCalendarMapper menuCalendarMapper;

    public CurrentUserAdvice(NotificationService notificationService, MenuCalendarMapper menuCalendarMapper) {
        this.notificationService = notificationService;
        this.menuCalendarMapper = menuCalendarMapper;
    }

    @ModelAttribute("loginEmail")
    public String loginEmail(@AuthenticationPrincipal LoginUser loginUser) {
        return loginUser == null ? null : loginUser.getEmailAddress();
    }

    @ModelAttribute("unreadCount")
    public int unreadCount(@AuthenticationPrincipal LoginUser loginUser) {
        return loginUser == null ? 0 : notificationService.countUnread(loginUser.getId());
    }

    @ModelAttribute("menuCalendars")
    public List<MenuCalendar> menuCalendars(@AuthenticationPrincipal LoginUser loginUser) {
        return loginUser == null ? List.of() : menuCalendarMapper.findByUserId(loginUser.getId());
    }

    /**
     * 最後に開いていたカレンダーのid。通知・設定のように、カレンダーに属さない画面でも、
     * ロゴやメニューが「いま使っているカレンダー」に戻れるようにするために使う。
     * 今も参加しているカレンダーのときだけ返す(外されたあとは null)。
     */
    @ModelAttribute("lastCalendarId")
    public Long lastCalendarId(@AuthenticationPrincipal LoginUser loginUser, HttpServletRequest request) {
        if (loginUser == null) {
            return null;
        }
        HttpSession session = request.getSession(false);
        Object saved = session == null ? null : session.getAttribute(LAST_CALENDAR_ID);
        if (!(saved instanceof Long)) {
            return null;
        }
        Long last = (Long) saved;
        boolean stillMember = menuCalendarMapper.findByUserId(loginUser.getId()).stream()
                .anyMatch(c -> last.equals(c.getId()));
        return stillMember ? last : null;
    }

    @ModelAttribute("navActive")
    public String navActive(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.startsWith("/notifications")) {
            return "notifications";
        }
        if (path.startsWith("/settings")) {
            return "settings";
        }
        if (path.startsWith("/search")) {
            return "search";
        }
        if (path.contains("/free-slots") || path.startsWith("/polls")) {
            return "freeslots";
        }
        if (path.endsWith("/members")) {
            return "members";
        }
        if (path.contains("/schedules/add")) {
            return "schedule-add";
        }
        if (path.equals("/calendars") || path.equals("/calendars/") || path.equals("/calendars/add")) {
            return "home";
        }
        return "calendar";
    }
}
