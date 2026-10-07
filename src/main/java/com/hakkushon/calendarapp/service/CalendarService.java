package com.hakkushon.calendarapp.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hakkushon.calendarapp.domain.Calendar;
import com.hakkushon.calendarapp.domain.CalendarMember;
import com.hakkushon.calendarapp.domain.CalendarSummary;
import com.hakkushon.calendarapp.domain.InviteCode;
import com.hakkushon.calendarapp.mapper.CalendarMapper;
import com.hakkushon.calendarapp.mapper.CalendarMemberMapper;
import com.hakkushon.calendarapp.mapper.InviteCodeMapper;

@Service
public class CalendarService {

    // 紛らわしい文字(0/O, 1/I/lなど)を除いた文字セット
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int INVITE_EXPIRY_DAYS = 7;

    private final CalendarMapper calendarMapper;
    private final CalendarMemberMapper calendarMemberMapper;
    private final InviteCodeMapper inviteCodeMapper;
    private final SecureRandom random = new SecureRandom();

    public CalendarService(CalendarMapper calendarMapper, CalendarMemberMapper calendarMemberMapper,
                            InviteCodeMapper inviteCodeMapper) {
        this.calendarMapper = calendarMapper;
        this.calendarMemberMapper = calendarMemberMapper;
        this.inviteCodeMapper = inviteCodeMapper;
    }

    /**
     * 新規登録時に自動で呼ばれる。本人だけが所属する個人用カレンダーを1つ作る。
     */
    @Transactional
    public void createPersonalCalendarForUser(Long userId) {
        Calendar calendar = new Calendar();
        calendar.setName("個人用");
        calendar.setOwnerId(userId);
        calendar.setPersonal(true);
        calendarMapper.insert(calendar);

        addOwnerMembership(calendar.getId(), userId);
    }

    public List<CalendarSummary> listCalendarsForUser(Long userId) {
        return calendarMapper.findCalendarsForUser(userId);
    }

    public Calendar getCalendar(Long calendarId) {
        return calendarMapper.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("カレンダーが見つかりません"));
    }

    public int countMembers(Long calendarId) {
        return calendarMemberMapper.countAcceptedMembers(calendarId);
    }

    /**
     * 共有カレンダーを新規作成し、作成者をownerとして登録、招待コードも発行する。
     */
    @Transactional
    public Long createCalendar(Long userId, String name) {
        Calendar calendar = new Calendar();
        calendar.setName(name);
        calendar.setOwnerId(userId);
        calendar.setPersonal(false);
        calendarMapper.insert(calendar);

        addOwnerMembership(calendar.getId(), userId);
        issueInviteCode(calendar.getId(), userId);

        return calendar.getId();
    }

    @Transactional
    public String issueInviteCode(Long calendarId, Long userId) {
        String code = generateCode();
        InviteCode invite = new InviteCode();
        invite.setCalendarId(calendarId);
        invite.setCode(code);
        invite.setCreatedBy(userId);
        invite.setExpiresAt(LocalDateTime.now().plusDays(INVITE_EXPIRY_DAYS));
        inviteCodeMapper.insert(invite);
        return code;
    }

    public enum JoinResult {
        SUCCESS, INVALID_CODE, ALREADY_MEMBER
    }

    /**
     * 招待コードで参加する。本来の設計では「招待された側が承認する」フローだが、
     * 学校課題の期間を考え、まずは「有効なコードなら即参加」というシンプルな形で実装している。
     * 承認フローが必要になったら calendar_members.status を 'pending' から始める形に変更する。
     */
    @Transactional
    public JoinResult joinByInviteCode(Long userId, String rawCode) {
        String code = rawCode.trim().toUpperCase();
        Optional<InviteCode> invite = inviteCodeMapper.findValidByCode(code);
        if (invite.isEmpty()) {
            return JoinResult.INVALID_CODE;
        }

        Long calendarId = invite.get().getCalendarId();
        if (calendarMemberMapper.isMember(calendarId, userId)) {
            return JoinResult.ALREADY_MEMBER;
        }

        CalendarMember member = new CalendarMember();
        member.setCalendarId(calendarId);
        member.setUserId(userId);
        member.setRole("member");
        member.setStatus("accepted");
        calendarMemberMapper.insert(member);

        return JoinResult.SUCCESS;
    }

    private void addOwnerMembership(Long calendarId, Long userId) {
        CalendarMember member = new CalendarMember();
        member.setCalendarId(calendarId);
        member.setUserId(userId);
        member.setRole("owner");
        member.setStatus("accepted");
        calendarMemberMapper.insert(member);
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(random.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
