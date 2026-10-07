package com.hakkushon.calendarapp.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

import com.hakkushon.calendarapp.common.LoginUser;
import com.hakkushon.calendarapp.dto.CreateCommentForm;
import com.hakkushon.calendarapp.service.CommentService;

@Controller
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/schedules/{id}/comments")
    public String create(@PathVariable Long id,
                          @Valid @ModelAttribute CreateCommentForm form,
                          BindingResult bindingResult,
                          @AuthenticationPrincipal LoginUser loginUser,
                          RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            // 空コメントなどは単純に元の画面へ戻す(フォームの入力内容は保持しない簡易対応)
            redirectAttributes.addFlashAttribute("commentError", "コメントを入力してください");
            return "redirect:/schedules/" + id;
        }

        commentService.addComment(id, loginUser.getId(), form.getContent());
        return "redirect:/schedules/" + id;
    }
}
