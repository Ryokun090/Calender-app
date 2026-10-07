package com.hakkushon.calendarapp.controller;

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

import com.hakkushon.calendarapp.common.CalendarAuthService;
import com.hakkushon.calendarapp.common.LoginUser;
import com.hakkushon.calendarapp.domain.Calendar;
import com.hakkushon.calendarapp.domain.Schedule;
import com.hakkushon.calendarapp.dto.CreateCalendarForm;
import com.hakkushon.calendarapp.dto.JoinCalendarForm;
import com.hakkushon.calendarapp.mapper.ScheduleMapper;
import com.hakkushon.calendarapp.service.CalendarService;

import jakarta.validation.Valid;

@Controller
public class CalendarController {

    private final CalendarService calendarService;
    private final CalendarAuthService calendarAuthService;
    private final ScheduleMapper scheduleMapper;

    public CalendarController(CalendarService calendarService, CalendarAuthService calendarAuthService,
                               ScheduleMapper scheduleMapper) {
        this.calendarService = calendarService;
        this.calendarAuthService = calendarAuthService;
        this.scheduleMapper = scheduleMapper;
    }

    @GetMapping("/calendars")
    public String list(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("calendars", calendarService.listCalendarsForUser(loginUser.getId()));
        return "calendars/list";
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
        String code = calendarService.issueInviteCode(id, loginUser.getId());
        return "redirect:/calendars/" + id + "?invite=" + code;
    }

    @GetMapping("/calendars/{id}")
    public String detail(@PathVariable Long id,
                          @RequestParam(required = false) Integer year,
                          @RequestParam(required = false) Integer month,
                          @RequestParam(required = false) String invite,
                          @AuthenticationPrincipal LoginUser loginUser,
                          Model model) {
        if (!calendarAuthService.isMember(loginUser.getId(), id)) {
            throw new AccessDeniedException("このカレンダーにアクセスする権限がありません");
        }

        boolean isOwner = calendarAuthService.isOwner(loginUser.getId(), id);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("invite", invite);

        YearMonth yearMonth = (year != null && month != null) ? YearMonth.of(year, month) : YearMonth.now();
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

        model.addAttribute("calendar", calendar);
        model.addAttribute("memberCount", memberCount);
        model.addAttribute("yearMonth", yearMonth);
        model.addAttribute("cells", cells);
        model.addAttribute("schedulesByDay", schedulesByDay);
        return "calendars/detail";
    }
}
