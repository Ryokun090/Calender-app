package com.hakkushon.calendarapp.controller;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

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

import jakarta.validation.Valid;

import com.hakkushon.calendarapp.common.LoginUser;
import com.hakkushon.calendarapp.domain.Calendar;
import com.hakkushon.calendarapp.domain.SchedulePoll;
import com.hakkushon.calendarapp.dto.CreatePollForm;
import com.hakkushon.calendarapp.dto.PollDetail;
import com.hakkushon.calendarapp.dto.RespondForm;
import com.hakkushon.calendarapp.dto.ScheduleEntry;
import com.hakkushon.calendarapp.mapper.CalendarMapper;
import com.hakkushon.calendarapp.service.PollService;

/**
 * 空き日程調整(調整さん型)。URLはガイドライン7-(4)の /calendars/{id}/free-slots をベースに、
 * 個々の候補調整は /polls/{id} で扱う。
 */
@Controller
public class PollController {

    private final PollService pollService;
    private final CalendarMapper calendarMapper;

    public PollController(PollService pollService, CalendarMapper calendarMapper) {
        this.pollService = pollService;
        this.calendarMapper = calendarMapper;
    }

    @GetMapping("/calendars/{calendarId}/free-slots")
    public String list(@PathVariable Long calendarId,
                        @AuthenticationPrincipal LoginUser loginUser,
                        Model model) {
        Calendar calendar = calendarMapper.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("カレンダーが見つかりません: id=" + calendarId));
        List<SchedulePoll> polls = pollService.listPollsForCalendar(calendarId, loginUser.getId());

        model.addAttribute("calendar", calendar);
        model.addAttribute("polls", polls);
        return "freeslots/list";
    }

    @GetMapping("/calendars/{calendarId}/free-slots/add")
    public String addForm(@PathVariable Long calendarId, Model model) {
        Calendar calendar = calendarMapper.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("カレンダーが見つかりません: id=" + calendarId));

        CreatePollForm form = new CreatePollForm();
        form.setCandidates(new ArrayList<>());
        ScheduleEntry firstEntry = new ScheduleEntry();
        firstEntry.setStartTime(LocalTime.of(19, 0));
        firstEntry.setEndTime(LocalTime.of(21, 0));
        form.getCandidates().add(firstEntry);

        model.addAttribute("calendar", calendar);
        model.addAttribute("createPollForm", form);
        return "freeslots/add";
    }

    @PostMapping("/calendars/{calendarId}/free-slots")
    public String create(@PathVariable Long calendarId,
                          @Valid @ModelAttribute("createPollForm") CreatePollForm form,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal LoginUser loginUser,
                          Model model) {

        if (bindingResult.hasErrors()) {
            Calendar calendar = calendarMapper.findById(calendarId)
                    .orElseThrow(() -> new IllegalArgumentException("カレンダーが見つかりません: id=" + calendarId));
            model.addAttribute("calendar", calendar);
            return "freeslots/add";
        }

        Long pollId = pollService.createPoll(calendarId, loginUser.getId(), form);
        return "redirect:/polls/" + pollId;
    }

    @GetMapping("/polls/{id}")
    public String detail(@PathVariable Long id,
                          @AuthenticationPrincipal LoginUser loginUser,
                          Model model) {
        PollDetail detail = pollService.getPollDetail(id, loginUser.getId());
        model.addAttribute("poll", detail.getPoll());
        model.addAttribute("candidates", detail.getCandidates());
        model.addAttribute("totalMembers", detail.getTotalMembers());
        model.addAttribute("isOwner", detail.isOwner());
        return "polls/detail";
    }

    @PostMapping("/polls/{id}/responses")
    public String respond(@PathVariable Long id,
                           @ModelAttribute RespondForm form,
                           @AuthenticationPrincipal LoginUser loginUser,
                           RedirectAttributes redirectAttributes) {
        pollService.submitResponses(id, loginUser.getId(), form.getResponses());
        redirectAttributes.addFlashAttribute("message", "回答を保存しました");
        return "redirect:/polls/" + id;
    }

    @PostMapping("/polls/{id}/confirm")
    public String confirm(@PathVariable Long id,
                           @RequestParam Long candidateId,
                           @AuthenticationPrincipal LoginUser loginUser,
                           RedirectAttributes redirectAttributes) {
        Long scheduleId = pollService.confirmCandidate(id, loginUser.getId(), candidateId);
        redirectAttributes.addFlashAttribute("message", "予定を確定しました");
        return "redirect:/schedules/" + scheduleId;
    }
}
