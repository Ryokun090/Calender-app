package com.hakkushon.calendarapp.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.hakkushon.calendarapp.common.CalendarAuthService;
import com.hakkushon.calendarapp.dto.BusyInterval;
import com.hakkushon.calendarapp.dto.FreeSlot;
import com.hakkushon.calendarapp.dto.MemberInfo;
import com.hakkushon.calendarapp.mapper.CalendarMemberMapper;
import com.hakkushon.calendarapp.mapper.FreeSlotMapper;

/**
 * 空き日程の自動計算。
 * 指定した期間の各日について、「開始〜終了の時間帯のうち、所要時間ぶん連続して空いている人が一番多い時間」を
 * 30分刻みで探し、集まれる人数が多い日から順に返す。
 */
@Service
public class FreeSlotService {

    public static final int STEP_MIN = 30;
    public static final int MAX_DAYS = 62;
    private static final int MAX_RESULTS = 30;

    private final FreeSlotMapper freeSlotMapper;
    private final CalendarMemberMapper calendarMemberMapper;
    private final CalendarAuthService calendarAuthService;

    public FreeSlotService(FreeSlotMapper freeSlotMapper, CalendarMemberMapper calendarMemberMapper,
                            CalendarAuthService calendarAuthService) {
        this.freeSlotMapper = freeSlotMapper;
        this.calendarMemberMapper = calendarMemberMapper;
        this.calendarAuthService = calendarAuthService;
    }

    /**
     * @param startMin    調べる時間帯の開始(0時からの分。30の倍数)
     * @param endMin      調べる時間帯の終了(同上。最大1440)
     * @param durationMin 必要な連続時間(分。30の倍数)
     * @throws IllegalArgumentException 条件が正しくないとき(メッセージは画面にそのまま出せる)
     */
    public List<FreeSlot> find(Long calendarId, Long requesterId, LocalDate from, LocalDate to,
                                int startMin, int endMin, int durationMin) {
        if (!calendarAuthService.isMember(requesterId, calendarId)) {
            throw new AccessDeniedException("このカレンダーのメンバーではありません");
        }
        validate(from, to, startMin, endMin, durationMin);

        List<MemberInfo> members = calendarMemberMapper.findMembersWithUser(calendarId);
        if (members.isEmpty()) {
            return new ArrayList<>();
        }

        LocalDateTime rangeStart = from.atStartOfDay();
        LocalDateTime rangeEnd = to.plusDays(1).atStartOfDay();
        Map<Long, List<BusyInterval>> busyByUser = new HashMap<>();
        for (BusyInterval b : freeSlotMapper.findBusyIntervals(calendarId, rangeStart, rangeEnd)) {
            busyByUser.computeIfAbsent(b.getUserId(), k -> new ArrayList<>()).add(b);
        }

        LocalDateTime now = LocalDateTime.now();
        List<FreeSlot> result = new ArrayList<>();
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        for (long i = 0; i < days; i++) {
            LocalDate date = from.plusDays(i);
            FreeSlot slot = bestSlotOfDay(date, members, busyByUser, startMin, endMin, durationMin, now);
            if (slot != null) {
                result.add(slot);
            }
        }

        // 集まれる人数が多い順、同じなら日付が早い順
        result.sort(Comparator.comparingInt(FreeSlot::getAvailableCount).reversed()
                .thenComparing(FreeSlot::getDate));
        return result.size() > MAX_RESULTS ? new ArrayList<>(result.subList(0, MAX_RESULTS)) : result;
    }

    private void validate(LocalDate from, LocalDate to, int startMin, int endMin, int durationMin) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("期間を入力してください");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("期間の終わりは、始まりより後の日にしてください");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new IllegalArgumentException("期間は最大" + MAX_DAYS + "日までです");
        }
        if (startMin < 0 || endMin > 24 * 60 || startMin >= endMin
                || startMin % STEP_MIN != 0 || endMin % STEP_MIN != 0) {
            throw new IllegalArgumentException("時間帯の指定が正しくありません");
        }
        if (durationMin < STEP_MIN || durationMin % STEP_MIN != 0) {
            throw new IllegalArgumentException("所要時間の指定が正しくありません");
        }
        if (durationMin > endMin - startMin) {
            throw new IllegalArgumentException("所要時間が、調べる時間帯より長くなっています");
        }
    }

    /** その日の最良の時間帯(集まれる人数が最大で、いちばん早いもの)。1人も集まれない日は null。 */
    private FreeSlot bestSlotOfDay(LocalDate date, List<MemberInfo> members, Map<Long, List<BusyInterval>> busyByUser,
                                    int startMin, int endMin, int durationMin, LocalDateTime now) {
        LocalDateTime dayBase = date.atStartOfDay();

        List<Integer> starts = new ArrayList<>();
        List<Set<Long>> freeSets = new ArrayList<>();
        for (int t = startMin; t + durationMin <= endMin; t += STEP_MIN) {
            LocalDateTime s = dayBase.plusMinutes(t);
            LocalDateTime e = s.plusMinutes(durationMin);
            if (s.isBefore(now)) {
                continue; // 過去の時間はおすすめしない
            }
            Set<Long> free = new HashSet<>();
            for (MemberInfo m : members) {
                if (isFree(busyByUser.get(m.getUserId()), s, e)) {
                    free.add(m.getUserId());
                }
            }
            starts.add(t);
            freeSets.add(free);
        }
        if (starts.isEmpty()) {
            return null;
        }

        int best = 0;
        for (int i = 1; i < starts.size(); i++) {
            if (freeSets.get(i).size() > freeSets.get(best).size()) {
                best = i; // 同数なら早い方を残す
            }
        }
        Set<Long> bestFree = freeSets.get(best);
        if (bestFree.isEmpty()) {
            return null;
        }

        // 同じメンバーで集まれる時間が続く限り、時間帯を広げて表示する
        int last = best;
        while (last + 1 < starts.size()
                && starts.get(last + 1) == starts.get(last) + STEP_MIN
                && freeSets.get(last + 1).equals(bestFree)) {
            last++;
        }

        List<String> unavailable = new ArrayList<>();
        for (MemberInfo m : members) {
            if (!bestFree.contains(m.getUserId())) {
                unavailable.add(m.getName());
            }
        }

        FreeSlot slot = new FreeSlot();
        slot.setDate(date);
        slot.setStartMin(starts.get(best));
        slot.setEndMin(starts.get(best) + durationMin);
        slot.setRangeEndMin(starts.get(last) + durationMin);
        slot.setAvailableCount(bestFree.size());
        slot.setTotalCount(members.size());
        slot.setUnavailableNames(unavailable);
        return slot;
    }

    /** [s, e) と少しでも重なる予定が無ければ「空いている」。 */
    private boolean isFree(List<BusyInterval> busy, LocalDateTime s, LocalDateTime e) {
        if (busy == null) {
            return true;
        }
        for (BusyInterval b : busy) {
            if (b.getStartTime().isBefore(e) && b.getEndTime().isAfter(s)) {
                return false;
            }
        }
        return true;
    }
}
