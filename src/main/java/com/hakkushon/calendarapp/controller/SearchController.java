package com.hakkushon.calendarapp.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hakkushon.calendarapp.common.LoginUser;
import com.hakkushon.calendarapp.dto.MemberSearchResult;
import com.hakkushon.calendarapp.dto.ScheduleSearchResult;
import com.hakkushon.calendarapp.mapper.SearchMapper;

/**
 * 検索(予定・メンバー)。自分が参加しているカレンダーの中だけを探す。
 * ヘッダーの検索ボックスと、/search の画面から使う。
 */
@Controller
public class SearchController {

    private static final int MAX_QUERY_LENGTH = 50;
    private static final int LIMIT = 50;

    private final SearchMapper searchMapper;

    public SearchController(SearchMapper searchMapper) {
        this.searchMapper = searchMapper;
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                          @AuthenticationPrincipal LoginUser loginUser,
                          Model model) {
        String query = q == null ? "" : q.trim();
        if (query.codePointCount(0, query.length()) > MAX_QUERY_LENGTH) {
            query = query.substring(0, query.offsetByCodePoints(0, MAX_QUERY_LENGTH));
        }

        List<ScheduleSearchResult> schedules = new ArrayList<>();
        List<MemberSearchResult> members = new ArrayList<>();
        if (!query.isEmpty()) {
            String pattern = escapeLike(query);
            schedules = searchMapper.searchSchedules(loginUser.getId(), pattern, LIMIT);
            members = searchMapper.searchMembers(loginUser.getId(), pattern, LIMIT);
        }

        model.addAttribute("searchQuery", query);
        model.addAttribute("searched", !query.isEmpty());
        model.addAttribute("scheduleResults", schedules);
        model.addAttribute("memberResults", members);
        model.addAttribute("limit", LIMIT);
        return "search/results";
    }

    /** LIKE の特殊文字(% _)と、エスケープ文字そのもの(!)を、文字として探せるようにする。 */
    static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
