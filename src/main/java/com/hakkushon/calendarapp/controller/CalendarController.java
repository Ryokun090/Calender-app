package com.hakkushon.calendarapp.controller;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hakkushon.calendarapp.common.CalendarAuthService;
import com.hakkushon.calendarapp.common.CurrentUserAdvice;
import com.hakkushon.calendarapp.common.LoginUser;
import com.hakkushon.calendarapp.domain.Calendar;
import com.hakkushon.calendarapp.domain.Schedule;
import com.hakkushon.calendarapp.dto.CreateCalendarForm;
import com.hakkushon.calendarapp.dto.JoinCalendarForm;
import com.hakkushon.calendarapp.dto.MemberInfo;
import com.hakkushon.calendarapp.dto.MenuCalendar;
import com.hakkushon.calendarapp.mapper.MenuCalendarMapper;
import com.hakkushon.calendarapp.mapper.ScheduleMapper;
import com.hakkushon.calendarapp.service.CalendarService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
public class CalendarController {

    private final CalendarService calendarService;
    private final CalendarAuthService calendarAuthService;
    private final ScheduleMapper scheduleMapper;
    private final MenuCalendarMapper menuCalendarMapper;

    public CalendarController(CalendarService calendarService, CalendarAuthService calendarAuthService,
                               ScheduleMapper scheduleMapper, MenuCalendarMapper menuCalendarMapper) {
        this.calendarService = calendarService;
        this.calendarAuthService = calendarAuthService;
        this.scheduleMapper = scheduleMapper;
        this.menuCalendarMapper = menuCalendarMapper;
    }

    /**
     * 「カレンダー一覧」の画面は持たない(カレンダーの切り替え・追加は☰メニューで行う)。
     * ログイン直後などでここに来たら、最初のカレンダー(個人用)へ移動する。
     */
    @GetMapping("/calendars")
    public String list(@AuthenticationPrincipal LoginUser loginUser, HttpSession session) {
        // 最後に開いていたカレンダーがあれば、そこへ戻る(今も参加している場合だけ)
        Object saved = session.getAttribute(CurrentUserAdvice.LAST_CALENDAR_ID);
        if (saved instanceof Long && calendarAuthService.isMember(loginUser.getId(), (Long) saved)) {
            return "redirect:/calendars/" + saved;
        }
        Long landingCalendarId = calendarService.findLandingCalendarId(loginUser.getId());
        return landingCalendarId != null ? "redirect:/calendars/" + landingCalendarId : "redirect:/calendars/add";
    }

    @GetMapping("/calendars/add")
    public String addForm(Model model) {
        model.addAttribute("createCalendarForm", new CreateCalendarForm());
        model.addAttribute("joinCalendarForm", new JoinCalendarForm());
        return "calendars/add";
    }

    @PostMapping("/calendars")
    public String create(@Valid @ModelAttribute("createCalendarForm") CreateCalendarForm form,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal LoginUser loginUser,
                          Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("joinCalendarForm", new JoinCalendarForm());
            return "calendars/add";
        }
        Long calendarId = calendarService.createCalendar(loginUser.getId(), form.getName());
        return "redirect:/calendars/" + calendarId;
    }

    @PostMapping("/calendars/join")
    public String join(@Valid @ModelAttribute("joinCalendarForm") JoinCalendarForm form,
                        BindingResult bindingResult,
                        @AuthenticationPrincipal LoginUser loginUser,
                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("createCalendarForm", new CreateCalendarForm());
            return "calendars/add";
        }

        CalendarService.JoinResult result = calendarService.joinByInviteCode(loginUser.getId(), form.getCode());
        switch (result) {
            case SUCCESS -> {
                return "redirect:/calendars";
            }
            case ALREADY_MEMBER -> model.addAttribute("joinError", "すでにこのカレンダーに参加しています");
            case INVALID_CODE -> model.addAttribute("joinError", "招待コードが無効か、有効期限が切れています");
        }
        model.addAttribute("createCalendarForm", new CreateCalendarForm());
        return "calendars/add";
    }

    @PostMapping("/calendars/{id}/invite")
    public String issueInvite(@PathVariable Long id,
                               @AuthenticationPrincipal LoginUser loginUser) {
        if (!calendarAuthService.isOwner(loginUser.getId(), id)) {
            throw new AccessDeniedException("この操作にはオーナー権限が必要です");
        }
        calendarService.issueInviteCode(id, loginUser.getId());
        // コードはURLに載せず、メンバー画面側で「今有効なコード」として表示する
        return "redirect:/calendars/" + id + "/members";
    }

    @GetMapping("/calendars/{id}/members")
    public String members(@PathVariable Long id,
                           @AuthenticationPrincipal LoginUser loginUser,
                           HttpSession session,
                           Model model) {
        List<MemberInfo> members = calendarService.listMembers(id, loginUser.getId());
        session.setAttribute(CurrentUserAdvice.LAST_CALENDAR_ID, id);
        model.addAttribute("calendar", calendarService.getCalendar(id));
        model.addAttribute("members", members);
        boolean isOwner = calendarAuthService.isOwner(loginUser.getId(), id);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("currentUserId", loginUser.getId());
        // オーナーにだけ、今有効な招待コードを見せる(無ければnull)
        model.addAttribute("invite", isOwner ? calendarService.findActiveInviteCode(id) : null);
        return "calendars/members";
    }

    @PostMapping("/calendars/{id}/members/{userId}/remove")
    public String removeMember(@PathVariable Long id,
                                @PathVariable Long userId,
                                @AuthenticationPrincipal LoginUser loginUser,
                                RedirectAttributes redirectAttributes) {
        calendarService.removeMember(id, loginUser.getId(), userId);
        redirectAttributes.addFlashAttribute("message",
                "メンバーを外しました。外した人が古い招待コードを持っていないか心配な場合は、招待コードを再発行してください。");
        return "redirect:/calendars/" + id + "/members";
    }

    @GetMapping("/calendars/{id}")
    public String detail(@PathVariable Long id,
                          @RequestParam(required = false) Integer year,
                          @RequestParam(required = false) Integer month,
                          @RequestParam(required = false) String ym,
                          @AuthenticationPrincipal LoginUser loginUser,
                          HttpSession session,
                          Model model) {
        if (!calendarAuthService.isMember(loginUser.getId(), id)) {
            throw new AccessDeniedException("このカレンダーにアクセスする権限がありません");
        }
        // 「いま使っているカレンダー」として覚えておく(通知・設定の画面からロゴで戻れるように)
        session.setAttribute(CurrentUserAdvice.LAST_CALENDAR_ID, id);

        YearMonth yearMonth = resolveYearMonth(ym, year, month);
        Calendar calendar = calendarService.getCalendar(id);
        int memberCount = calendarService.countMembers(id);

        LocalDateTime rangeStart = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime rangeEnd = yearMonth.plusMonths(1).atDay(1).atStartOfDay();
        List<Schedule> schedules = scheduleMapper.findByCalendarAndRange(id, rangeStart, rangeEnd);

        // 日ごとにグルーピング(月をまたぐ予定は開始日のセルにのみ表示する簡易版)
        Map<Integer, List<Schedule>> schedulesByDay = new HashMap<>();
        for (Schedule schedule : schedules) {
            LocalDate day = schedule.getStartTime().toLocalDate();
            if (YearMonth.from(day).equals(yearMonth)) {
                schedulesByDay.computeIfAbsent(day.getDayOfMonth(), k -> new ArrayList<>()).add(schedule);
            }
        }

        // 月曜はじまりの7列グリッド。0は空白セル
        int daysInMonth = yearMonth.lengthOfMonth();
        int leadingBlanks = yearMonth.atDay(1).getDayOfWeek().getValue() - 1;
        List<Integer> cells = new ArrayList<>();
        for (int i = 0; i < leadingBlanks; i++) {
            cells.add(0);
        }
        for (int day = 1; day <= daysInMonth; day++) {
            cells.add(day);
        }
        while (cells.size() % 7 != 0) {
            cells.add(0);
        }

        LocalDate today = LocalDate.now();
        model.addAttribute("todayDay", YearMonth.from(today).equals(yearMonth) ? today.getDayOfMonth() : 0);
        // 予定の色は、☰メニューの●と同じ色(そのカレンダーの色)にそろえる
        String calendarColor = menuCalendarMapper.findByUserId(loginUser.getId()).stream()
                .filter(m -> id.equals(m.getId()))
                .map(MenuCalendar::getColor)
                .findFirst()
                .orElse("#1f73f2");
        model.addAttribute("calendarColor", calendarColor);
        model.addAttribute("calendar", calendar);
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("yearMonth", yearMonth);
        model.addAttribute("cells", cells);
        model.addAttribute("schedulesByDay", schedulesByDay);
        return "calendars/detail";
    }

    /**
     * 表示する年月を決める。ym(例: 2026-10)を優先し、無ければyear+month、どちらも無効なら今月。
     * ?month=13 のような範囲外の値でエラーにならないようにする。
     */
    private YearMonth resolveYearMonth(String ym, Integer year, Integer month) {
        try {
            if (ym != null && !ym.isBlank()) {
                return YearMonth.parse(ym);
            }
            if (year != null && month != null) {
                return YearMonth.of(year, month);
            }
        } catch (DateTimeException e) {
            // 不正な値は今月にフォールバック
        }
        return YearMonth.now();
    }
}
