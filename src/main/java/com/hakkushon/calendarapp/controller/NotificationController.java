package com.hakkushon.calendarapp.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.hakkushon.calendarapp.common.LoginUser;
import com.hakkushon.calendarapp.domain.Notification;
import com.hakkushon.calendarapp.service.NotificationService;

@Controller
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/notifications")
    public String list(@AuthenticationPrincipal LoginUser loginUser, Model model) {
        model.addAttribute("notifications", notificationService.listForUser(loginUser.getId()));
        return "notifications/list";
    }

    /** 通知を開く=既読にしてから、紐づく予定があればそこへ、無ければ通知一覧に戻す。 */
    @GetMapping("/notifications/{id}/open")
    public String open(@PathVariable Long id, @AuthenticationPrincipal LoginUser loginUser) {
        Notification notification = notificationService.markAsReadAndGet(id, loginUser.getId());
        if (notification.getScheduleId() != null) {
            return "redirect:/schedules/" + notification.getScheduleId();
        }
        return "redirect:/notifications";
    }
}
