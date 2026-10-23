package com.hakkushon.calendarapp.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hakkushon.calendarapp.domain.Calendar;
import com.hakkushon.calendarapp.domain.CalendarMember;
import com.hakkushon.calendarapp.domain.CalendarSummary;
import com.hakkushon.calendarapp.domain.InviteCode;
import com.hakkushon.calendarapp.dto.MemberInfo;
import com.hakkushon.calendarapp.dto.MenuCalendar;
import com.hakkushon.calendarapp.mapper.CalendarAdminMapper;
import com.hakkushon.calendarapp.mapper.CalendarMapper;
import com.hakkushon.calendarapp.mapper.CalendarMemberMapper;
import com.hakkushon.calendarapp.mapper.InviteCodeAdminMapper;
import com.hakkushon.calendarapp.mapper.InviteCodeMapper;
import com.hakkushon.calendarapp.mapper.MenuCalendarMapper;
import com.hakkushon.calendarapp.mapper.PollResponseMapper;

@Service
public class CalendarService {

    // 紛らわしい文字(0/O, 1/I/lなど)を除いた文字セット
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int INVITE_EXPIRY_DAYS = 7;

    private final CalendarMapper calendarMapper;
    private final CalendarMemberMapper calendarMemberMapper;
    private final InviteCodeMapper inviteCodeMapper;
    private final InviteCodeAdminMapper inviteCodeAdminMapper;
    private final PollResponseMapper pollResponseMapper;
    private final MenuCalendarMapper menuCalendarMapper;
    private final CalendarAdminMapper calendarAdminMapper;
    private final SecureRandom random = new SecureRandom();

    public CalendarService(CalendarMapper calendarMapper, CalendarMemberMapper calendarMemberMapper,
                            InviteCodeMapper inviteCodeMapper, InviteCodeAdminMapper inviteCodeAdminMapper,
                            PollResponseMapper pollResponseMapper, MenuCalendarMapper menuCalendarMapper,
                            CalendarAdminMapper calendarAdminMapper) {
        this.calendarMapper = calendarMapper;
        this.calendarMemberMapper = calendarMemberMapper;
        this.inviteCodeMapper = inviteCodeMapper;
        this.inviteCodeAdminMapper = inviteCodeAdminMapper;
        this.pollResponseMapper = pollResponseMapper;
        this.menuCalendarMapper = menuCalendarMapper;
        this.calendarAdminMapper = calendarAdminMapper;
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

    /**
     * ログイン直後などに最初に開くカレンダー(個人用が先頭)。参加しているカレンダーが無ければnull。
     */
    public Long findLandingCalendarId(Long userId) {
        List<MenuCalendar> calendars = menuCalendarMapper.findByUserId(userId);
        return calendars.isEmpty() ? null : calendars.get(0).getId();
    }

    public Calendar getCalendar(Long calendarId) {
        return calendarMapper.findById(calendarId)
                .orElseThrow(() -> new IllegalArgumentException("カレンダーが見つかりません"));
    }

    public int countMembers(Long calendarId) {
        return calendarMemberMapper.countAcceptedMembers(calendarId);
    }

    /** メンバー一覧。カレンダーのメンバーのみ参照できる。 */
    public List<MemberInfo> listMembers(Long calendarId, Long requesterId) {
        if (!calendarMemberMapper.isMember(calendarId, requesterId)) {
            throw new AccessDeniedException("このカレンダーのメンバーではありません");
        }
        return calendarMemberMapper.findMembersWithUser(calendarId);
    }

    /**
     * メンバーを追放する。オーナーのみ実行でき、自分自身とオーナーは外せない。
     * 外したメンバーが空き日程調整に出していた○×回答は、集計に残らないように消す。
     * (そのメンバーが作った予定・コメントは残る)
     */
    @Transactional
    public void removeMember(Long calendarId, Long operatorId, Long targetUserId) {
        if (!calendarMemberMapper.isOwner(calendarId, operatorId)) {
            throw new AccessDeniedException("メンバーを外せるのはオーナーのみです");
        }
        if (operatorId.equals(targetUserId)) {
            throw new IllegalArgumentException("自分自身は外せません");
        }
        int deleted = calendarMemberMapper.deleteMember(calendarId, targetUserId);
        if (deleted == 0) {
            // メンバーでない、またはオーナー(deleteMemberはrole='member'だけを消す)
            throw new IllegalArgumentException("このユーザーは外せません");
        }
        pollResponseMapper.deleteByUserInCalendar(calendarId, targetUserId);
    }

    /**
     * 自分からカレンダーを抜ける。オーナーと個人用カレンダーは抜けられない
     * (オーナーは、カレンダーを削除するか、他の人にオーナーを渡す機能ができるまで抜けられない)。
     * 抜けた人が出していた○×回答は集計に残らないよう消す。その人が作った予定・コメントは残る。
     */
    @Transactional
    public void leaveCalendar(Long calendarId, Long userId) {
        getCalendar(calendarId); // 存在しないidならここで例外
        if (!calendarMemberMapper.isMember(calendarId, userId)) {
            throw new AccessDeniedException("このカレンダーのメンバーではありません");
        }
        if (isPersonalCalendar(calendarId, userId)) {
            throw new IllegalArgumentException("個人用のカレンダーからは抜けられません");
        }
        if (calendarMemberMapper.isOwner(calendarId, userId)) {
            throw new IllegalArgumentException("オーナーはカレンダーから抜けられません。不要なら、カレンダーを削除してください");
        }
        int deleted = calendarMemberMapper.deleteMember(calendarId, userId);
        if (deleted == 0) {
            throw new IllegalStateException("カレンダーから抜けられませんでした");
        }
        pollResponseMapper.deleteByUserInCalendar(calendarId, userId);
    }

    /**
     * 共有カレンダーを削除する。オーナーのみ。個人用カレンダーは削除できない。
     * 中の予定・コメント・空き日程調整・メンバー・招待コードもすべて消え、元に戻せない。
     */
    @Transactional
    public void deleteCalendar(Long calendarId, Long userId) {
        getCalendar(calendarId);
        if (!calendarMemberMapper.isOwner(calendarId, userId)) {
            throw new AccessDeniedException("カレンダーを削除できるのはオーナーのみです");
        }
        if (isPersonalCalendar(calendarId, userId)) {
            throw new IllegalArgumentException("個人用のカレンダーは削除できません");
        }
        // SQL側でも is_personal = FALSE を条件にしているので、万一ここで個人用が来ても消えない
        if (calendarAdminMapper.deleteSharedCalendar(calendarId) != 1) {
            throw new IllegalStateException("カレンダーを削除できませんでした");
        }
    }

    private boolean isPersonalCalendar(Long calendarId, Long userId) {
        return menuCalendarMapper.findByUserId(userId).stream()
                .anyMatch(c -> calendarId.equals(c.getId()) && c.isPersonal());
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

    /**
     * 招待コードを新しく発行する。そのカレンダーで有効だった古いコードは、先に期限切れにして使えなくする。
     */
    @Transactional
    public String issueInviteCode(Long calendarId, Long userId) {
        inviteCodeAdminMapper.expireAllByCalendar(calendarId);

        String code = generateCode();
        InviteCode invite = new InviteCode();
        invite.setCalendarId(calendarId);
        invite.setCode(code);
        invite.setCreatedBy(userId);
        invite.setExpiresAt(LocalDateTime.now().plusDays(INVITE_EXPIRY_DAYS));
        inviteCodeMapper.insert(invite);
        return code;
    }

    /** 今そのカレンダーで有効な招待コード。無ければnull。 */
    public String findActiveInviteCode(Long calendarId) {
        return inviteCodeAdminMapper.findActiveCode(calendarId);
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
