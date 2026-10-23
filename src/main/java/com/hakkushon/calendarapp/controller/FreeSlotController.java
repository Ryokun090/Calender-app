package com.hakkushon.calendarapp.controller;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hakkushon.calendarapp.common.CalendarAuthService;
import com.hakkushon.calendarapp.common.LoginUser;
import com.hakkushon.calendarapp.dto.CreatePollForm;
import com.hakkushon.calendarapp.dto.FreeSlot;
import com.hakkushon.calendarapp.dto.ScheduleEntry;
import com.hakkushon.calendarapp.service.CalendarService;
import com.hakkushon.calendarapp.service.FreeSlotService;
import com.hakkushon.calendarapp.service.PollService;
import com.hakkushon.calendarapp.service.ScheduleService;

import jakarta.servlet.http.HttpSession;

/**
 * 空き日程の自動計算の画面(Figma「空き日程を探す」)。
 * ○×で決める従来の調整(PollController)とは別のURL(/free-slots/auto)にしてあり、両方使える。
 */
@Controller
public class FreeSlotController {

    private static final int DEFAULT_DAYS = 14;
    private static final int DEFAULT_START_HOUR = 9;
    private static final int DEFAULT_END_HOUR = 22;
    private static final int DEFAULT_DURATION_MIN = 120;

    private final FreeSlotService freeSlotService;
    private final CalendarService calendarService;
    private final CalendarAuthService calendarAuthService;
    private final ScheduleService scheduleService;
    private final PollService pollService;

    public FreeSlotController(FreeSlotService freeSlotService, CalendarService calendarService,
                               CalendarAuthService calendarAuthService, ScheduleService scheduleService,
                               PollService pollService) {
        this.pollService = pollService;
        this.freeSlotService = freeSlotService;
        this.calendarService = calendarService;
        this.calendarAuthService = calendarAuthService;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/calendars/{id}/free-slots/auto")
    public String auto(@PathVariable Long id,
                        @RequestParam(required = false) String from,
                        @RequestParam(required = false) String to,
                        @RequestParam(required = false) Integer startHour,
                        @RequestParam(required = false) Integer endHour,
                        @RequestParam(required = false) Integer minutes,
                        @AuthenticationPrincipal LoginUser loginUser,
                        HttpSession session,
                        Model model) {
        if (!calendarAuthService.isMember(loginUser.getId(), id)) {
            throw new AccessDeniedException("このカレンダーにアクセスする権限がありません");
        }
        session.setAttribute(com.hakkushon.calendarapp.common.CurrentUserAdvice.LAST_CALENDAR_ID, id);

        List<String> errors = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate fromDate = parseDate(from, today, "期間の始まり", errors);
        LocalDate toDate = parseDate(to, fromDate.plusDays(DEFAULT_DAYS - 1), "期間の終わり", errors);
        int sh = startHour == null ? DEFAULT_START_HOUR : startHour;
        int eh = endHour == null ? DEFAULT_END_HOUR : endHour;
        int dur = minutes == null ? DEFAULT_DURATION_MIN : minutes;

        List<FreeSlot> slots = new ArrayList<>();
        if (errors.isEmpty()) {
            try {
                slots = freeSlotService.find(id, loginUser.getId(), fromDate, toDate,
                        sh * 60, eh * 60, dur);
            } catch (IllegalArgumentException e) {
                errors.add(e.getMessage());
            }
        }

        model.addAttribute("calendar", calendarService.getCalendar(id));
        model.addAttribute("slots", slots);
        model.addAttribute("totalMembers", calendarService.countMembers(id));
        model.addAttribute("errors", errors);
        model.addAttribute("from", fromDate);
        model.addAttribute("to", toDate);
        model.addAttribute("startHour", sh);
        model.addAttribute("endHour", eh);
        model.addAttribute("minutes", dur);
        // 「この日で決める」のあと、同じ条件の画面へ戻るための検索条件
        model.addAttribute("backQuery", "?from=" + fromDate + "&to=" + toDate
                + "&startHour=" + sh + "&endHour=" + eh + "&minutes=" + dur);
        return "freeslots/auto";
    }

    /** 「この日で決める」: 選んだ日時で予定を1件作り、他のメンバーへ通知する。 */
    @PostMapping("/calendars/{id}/free-slots/auto/confirm")
    public String confirm(@PathVariable Long id,
                           @RequestParam String date,
                           @RequestParam int startMin,
                           @RequestParam int endMin,
                           @RequestParam String title,
                           @RequestParam(required = false) String back,
                           @AuthenticationPrincipal LoginUser loginUser,
                           RedirectAttributes redirectAttributes) {
        String backUrl = "redirect:/calendars/" + id + "/free-slots/auto" + safeQuery(back);
        try {
            LocalDate day = LocalDate.parse(date);
            if (startMin < 0 || endMin > 24 * 60 || startMin >= endMin) {
                throw new IllegalArgumentException("時間の指定が正しくありません");
            }
            LocalDateTime start = day.atStartOfDay().plusMinutes(startMin);
            LocalDateTime end = day.atStartOfDay().plusMinutes(endMin);
            Long scheduleId = scheduleService.createSingle(id, loginUser.getId(), title, start, end);
            redirectAttributes.addFlashAttribute("message", "予定を作りました。メンバーに通知しました");
            return "redirect:/schedules/" + scheduleId;
        } catch (DateTimeException e) {
            redirectAttributes.addFlashAttribute("error", "日付の指定が正しくありません");
            return backUrl;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return backUrl;
        }
    }

    /**
     * 「選んだ日で投票を作る」: 自動で出た候補のうちチェックした日を、○×投票の候補日程にする。
     * 作ったあとは、既存の投票画面(/polls/{id})でメンバーが回答し、確定する。
     */
    @PostMapping("/calendars/{id}/free-slots/auto/poll")
    public String createPoll(@PathVariable Long id,
                              @RequestParam(name = "slot", required = false) List<String> slots,
                              @RequestParam String title,
                              @RequestParam(required = false) String back,
                              @AuthenticationPrincipal LoginUser loginUser,
                              RedirectAttributes redirectAttributes) {
        String backUrl = "redirect:/calendars/" + id + "/free-slots/auto" + safeQuery(back);
        if (!calendarAuthService.isMember(loginUser.getId(), id)) {
            throw new AccessDeniedException("このカレンダーにアクセスする権限がありません");
        }
        String trimmed = title == null ? "" : title.trim();
        if (trimmed.isEmpty() || trimmed.codePointCount(0, trimmed.length()) > 100) {
            redirectAttributes.addFlashAttribute("error", "投票のタイトルは1〜100文字で入力してください");
            return backUrl;
        }
        if (slots == null || slots.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "候補にする日をチェックしてください");
            return backUrl;
        }
        if (slots.size() > 10) {
            redirectAttributes.addFlashAttribute("error", "候補は10個までです");
            return backUrl;
        }

        List<ScheduleEntry> candidates = new ArrayList<>();
        try {
            for (String slot : slots) {
                String[] parts = slot.split("\\|");
                if (parts.length != 3) {
                    throw new IllegalArgumentException("候補の指定が正しくありません");
                }
                int startMin = Integer.parseInt(parts[1]);
                int endMin = Integer.parseInt(parts[2]);
                if (startMin < 0 || startMin >= endMin) {
                    throw new IllegalArgumentException("候補の時間が正しくありません");
                }
                if (endMin >= 24 * 60) {
                    throw new IllegalArgumentException("24:00まで続く候補は投票にできません。時間帯の終わりを23時までにして探し直してください");
                }
                ScheduleEntry entry = new ScheduleEntry();
                entry.setDate(LocalDate.parse(parts[0]));
                entry.setStartTime(LocalTime.of(startMin / 60, startMin % 60));
                entry.setEndTime(LocalTime.of(endMin / 60, endMin % 60));
                candidates.add(entry);
            }
        } catch (NumberFormatException | DateTimeException e) {
            redirectAttributes.addFlashAttribute("error", "候補の指定が正しくありません");
            return backUrl;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return backUrl;
        }

        CreatePollForm form = new CreatePollForm();
        form.setTitle(trimmed);
        form.setCandidates(candidates);
        Long pollId = pollService.createPoll(id, loginUser.getId(), form);
        return "redirect:/polls/" + pollId;
    }

    private LocalDate parseDate(String value, LocalDate fallback, String label, List<String> errors) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeException e) {
            errors.add(label + "の日付が正しくありません");
            return fallback;
        }
    }

    /** 戻り先の検索条件(?from=...&to=... の形)だけを通す。それ以外の文字列は捨てる。 */
    private String safeQuery(String back) {
        if (back != null && back.matches("^\\?[A-Za-z0-9=&\\-]{0,200}$")) {
            return back;
        }
        return "";
    }
}
