package com.hakkushon.calendarapp.controller;

import java.time.LocalTime;
import java.util.ArrayList;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import com.hakkushon.calendarapp.common.LoginUser;
import com.hakkushon.calendarapp.domain.Calendar;
import com.hakkushon.calendarapp.domain.Schedule;
import com.hakkushon.calendarapp.dto.CreateCommentForm;
import com.hakkushon.calendarapp.dto.CreateScheduleForm;
import com.hakkushon.calendarapp.dto.EditScheduleForm;
import com.hakkushon.calendarapp.dto.ScheduleEntry;
import com.hakkushon.calendarapp.mapper.CalendarMapper;
import com.hakkushon.calendarapp.service.CommentService;
import com.hakkushon.calendarapp.service.ScheduleService;

/**
 * 予定登録(一括登録)・予定詳細・編集・削除。URLの割り振りは開発ガイドライン7-(4)の通り。
 *   /calendars/{id}/schedules → 一覧からの登録
 *   /schedules/{id}           → 予定本体(詳細・編集・削除・コメント一覧の表示)
 */
@Controller
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final CalendarMapper calendarMapper;
    private final CommentService commentService;

    public ScheduleController(ScheduleService scheduleService, CalendarMapper calendarMapper,
                               CommentService commentService) {
        this.scheduleService = scheduleService;
        this.calendarMapper = calendarMapper;
        this.commentService = commentService;
    }

    @GetMapping("/calendars/{calendarId}/schedules/add")
    public String addForm(@PathVariable Long calendarId, Model model) {
        Calendar calendar = calendarMapper.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("カレンダーが見つかりません: id=" + calendarId));

        CreateScheduleForm form = new CreateScheduleForm();
        form.setEntries(new ArrayList<>());
        ScheduleEntry firstEntry = new ScheduleEntry();
        firstEntry.setStartTime(LocalTime.of(19, 0));
        firstEntry.setEndTime(LocalTime.of(21, 0));
        form.getEntries().add(firstEntry);

        model.addAttribute("calendar", calendar);
        model.addAttribute("createScheduleForm", form);
        return "schedules/add";
    }

    @PostMapping("/calendars/{calendarId}/schedules")
    public String create(@PathVariable Long calendarId,
                          @Valid @ModelAttribute("createScheduleForm") CreateScheduleForm form,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal LoginUser loginUser,
                          Model model,
                          RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            Calendar calendar = calendarMapper.findById(calendarId)
                    .orElseThrow(() -> new IllegalArgumentException("カレンダーが見つかりません: id=" + calendarId));
            model.addAttribute("calendar", calendar);
            return "schedules/add";
        }

        try {
            scheduleService.createSchedules(calendarId, loginUser.getId(), form);
        } catch (AccessDeniedException e) {
            bindingResult.addError(new FieldError("createScheduleForm", "title", e.getMessage()));
            Calendar calendar = calendarMapper.findById(calendarId)
                    .orElseThrow(() -> new IllegalArgumentException("カレンダーが見つかりません: id=" + calendarId));
            model.addAttribute("calendar", calendar);
            return "schedules/add";
        }

        redirectAttributes.addFlashAttribute("message", "予定を登録しました");
        return "redirect:/calendars/" + calendarId;
    }

    @GetMapping("/schedules/{id}")
    public String detail(@PathVariable Long id,
                          @AuthenticationPrincipal LoginUser loginUser,
                          Model model) {
        Schedule schedule = scheduleService.getScheduleForMember(id, loginUser.getId());
        model.addAttribute("schedule", schedule);
        model.addAttribute("comments", commentService.listForSchedule(id, loginUser.getId()));
        model.addAttribute("createCommentForm", new CreateCommentForm());
        return "schedules/detail";
    }

    @GetMapping("/schedules/{id}/edit")
    public String editForm(@PathVariable Long id,
                            @AuthenticationPrincipal LoginUser loginUser,
                            Model model) {
        Schedule schedule = scheduleService.getScheduleForMember(id, loginUser.getId());

        EditScheduleForm form = new EditScheduleForm();
        form.setTitle(schedule.getTitle());
        form.setDescription(schedule.getDescription());
        form.setLocation(schedule.getLocation());
        form.setDate(schedule.getStartTime().toLocalDate());
        form.setStartTime(schedule.getStartTime().toLocalTime());
        form.setEndTime(schedule.getEndTime().toLocalTime());

        model.addAttribute("schedule", schedule);
        model.addAttribute("editScheduleForm", form);
        return "schedules/edit";
    }

    @PostMapping("/schedules/{id}")
    public String update(@PathVariable Long id,
                          @Valid @ModelAttribute("editScheduleForm") EditScheduleForm form,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal LoginUser loginUser,
                          Model model,
                          RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            Schedule schedule = scheduleService.getScheduleForMember(id, loginUser.getId());
            model.addAttribute("schedule", schedule);
            return "schedules/edit";
        }

        scheduleService.updateSchedule(id, loginUser.getId(), form);
        redirectAttributes.addFlashAttribute("message", "予定を更新しました");
        return "redirect:/schedules/" + id;
    }

    @PostMapping("/schedules/{id}/delete")
    public String delete(@PathVariable Long id,
                          @AuthenticationPrincipal LoginUser loginUser,
                          RedirectAttributes redirectAttributes) {
        Schedule schedule = scheduleService.getScheduleForMember(id, loginUser.getId());
        Long calendarId = schedule.getCalendarId();

        scheduleService.deleteSchedule(id, loginUser.getId());

        redirectAttributes.addFlashAttribute("message", "予定を削除しました");
        return "redirect:/calendars/" + calendarId;
    }
}
