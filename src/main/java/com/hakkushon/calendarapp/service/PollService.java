package com.hakkushon.calendarapp.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hakkushon.calendarapp.common.CalendarAuthService;
import com.hakkushon.calendarapp.domain.PollCandidate;
import com.hakkushon.calendarapp.domain.PollResponse;
import com.hakkushon.calendarapp.domain.Schedule;
import com.hakkushon.calendarapp.domain.SchedulePoll;
import com.hakkushon.calendarapp.dto.CandidateSummary;
import com.hakkushon.calendarapp.dto.CreatePollForm;
import com.hakkushon.calendarapp.dto.PollDetail;
import com.hakkushon.calendarapp.dto.ScheduleEntry;
import com.hakkushon.calendarapp.mapper.CalendarMemberMapper;
import com.hakkushon.calendarapp.mapper.PollCandidateMapper;
import com.hakkushon.calendarapp.mapper.PollResponseMapper;
import com.hakkushon.calendarapp.mapper.ScheduleMapper;
import com.hakkushon.calendarapp.mapper.SchedulePollMapper;

/**
 * 空き日程調整(調整さん型)。開発ガイドライン9-3「パターンB」に対応する。
 * 1. 候補作成(createPoll) 2. メンバーの○×回答(submitResponses) 3. 幹事による確定(confirmCandidate)
 */
@Service
public class PollService {

    private final SchedulePollMapper pollMapper;
    private final PollCandidateMapper candidateMapper;
    private final PollResponseMapper responseMapper;
    private final ScheduleMapper scheduleMapper;
    private final CalendarMemberMapper calendarMemberMapper;
    private final CalendarAuthService calendarAuthService;

    public PollService(SchedulePollMapper pollMapper,
                        PollCandidateMapper candidateMapper,
                        PollResponseMapper responseMapper,
                        ScheduleMapper scheduleMapper,
                        CalendarMemberMapper calendarMemberMapper,
                        CalendarAuthService calendarAuthService) {
        this.pollMapper = pollMapper;
        this.candidateMapper = candidateMapper;
        this.responseMapper = responseMapper;
        this.scheduleMapper = scheduleMapper;
        this.calendarMemberMapper = calendarMemberMapper;
        this.calendarAuthService = calendarAuthService;
    }

    @Transactional
    public Long createPoll(Long calendarId, Long userId, CreatePollForm form) {
        if (!calendarAuthService.isMember(userId, calendarId)) {
            throw new AccessDeniedException("このカレンダーで候補日程を作る権限がありません");
        }

        SchedulePoll poll = new SchedulePoll();
        poll.setCalendarId(calendarId);
        poll.setTitle(form.getTitle());
        poll.setDescription(form.getDescription());
        poll.setLocation(form.getLocation());
        poll.setCreatedBy(userId);
        pollMapper.insert(poll);

        for (ScheduleEntry entry : form.getCandidates()) {
            PollCandidate candidate = new PollCandidate();
            candidate.setPollId(poll.getId());
            candidate.setStartTime(LocalDateTime.of(entry.getDate(), entry.getStartTime()));
            candidate.setEndTime(LocalDateTime.of(entry.getDate(), entry.getEndTime()));
            candidateMapper.insert(candidate);
        }

        return poll.getId();
    }

    public List<SchedulePoll> listPollsForCalendar(Long calendarId, Long userId) {
        if (!calendarAuthService.isMember(userId, calendarId)) {
            throw new AccessDeniedException("このカレンダーを見る権限がありません");
        }
        return pollMapper.findByCalendarId(calendarId);
    }

    public PollDetail getPollDetail(Long pollId, Long userId) {
        SchedulePoll poll = findPollOrThrow(pollId);
        if (!calendarAuthService.isMember(userId, poll.getCalendarId())) {
            throw new AccessDeniedException("この候補日程を見る権限がありません");
        }

        List<PollCandidate> candidates = candidateMapper.findByPollId(pollId);
        List<Long> candidateIds = candidates.stream().map(PollCandidate::getId).toList();
        List<PollResponse> responses = candidateIds.isEmpty()
                ? List.of()
                : responseMapper.findByCandidateIds(candidateIds);

        List<CandidateSummary> summaries = new ArrayList<>();
        for (PollCandidate candidate : candidates) {
            long yesCount = 0;
            long noCount = 0;
            String myResponse = null;
            for (PollResponse response : responses) {
                if (!response.getCandidateId().equals(candidate.getId())) {
                    continue;
                }
                if ("yes".equals(response.getResponse())) {
                    yesCount++;
                } else if ("no".equals(response.getResponse())) {
                    noCount++;
                }
                if (response.getUserId().equals(userId)) {
                    myResponse = response.getResponse();
                }
            }
            summaries.add(new CandidateSummary(
                    candidate.getId(), candidate.getStartTime(), candidate.getEndTime(),
                    yesCount, noCount, myResponse));
        }

        int totalMembers = calendarMemberMapper.countAcceptedMembers(poll.getCalendarId());
        boolean isOwner = calendarAuthService.isOwner(userId, poll.getCalendarId());

        return new PollDetail(poll, summaries, totalMembers, isOwner);
    }

    @Transactional
    public void submitResponses(Long pollId, Long userId, Map<Long, String> responses) {
        SchedulePoll poll = findPollOrThrow(pollId);
        if (!calendarAuthService.isMember(userId, poll.getCalendarId())) {
            throw new AccessDeniedException("この候補日程に回答する権限がありません");
        }
        if (!poll.isOpen()) {
            throw new IllegalStateException("この候補日程はすでに確定しています");
        }

        for (Map.Entry<Long, String> entry : responses.entrySet()) {
            String value = entry.getValue();
            if (!"yes".equals(value) && !"no".equals(value)) {
                continue; // 未選択はスキップ(回答なしのまま)
            }
            responseMapper.upsert(entry.getKey(), userId, value);
        }
    }

    @Transactional
    public Long confirmCandidate(Long pollId, Long userId, Long candidateId) {
        SchedulePoll poll = findPollOrThrow(pollId);
        if (!calendarAuthService.isOwner(userId, poll.getCalendarId())) {
            throw new AccessDeniedException("候補日程を確定できるのはカレンダーの管理者のみです");
        }
        if (!poll.isOpen()) {
            throw new IllegalStateException("この候補日程はすでに確定しています");
        }

        PollCandidate candidate = candidateMapper.findById(candidateId)
                .orElseThrow(() -> new IllegalArgumentException("候補が見つかりません: id=" + candidateId));
        if (!candidate.getPollId().equals(pollId)) {
            throw new IllegalArgumentException("この候補はこの調整とは別のものです");
        }

        Schedule schedule = new Schedule();
        schedule.setCalendarId(poll.getCalendarId());
        schedule.setTitle(poll.getTitle());
        schedule.setDescription(poll.getDescription());
        schedule.setLocation(poll.getLocation());
        schedule.setStartTime(candidate.getStartTime());
        schedule.setEndTime(candidate.getEndTime());
        schedule.setCreatedBy(userId);
        scheduleMapper.insert(schedule);

        pollMapper.confirm(pollId, schedule.getId());

        return schedule.getId();
    }

    private SchedulePoll findPollOrThrow(Long pollId) {
        return pollMapper.findById(pollId)
                .orElseThrow(() -> new IllegalArgumentException("候補日程が見つかりません: id=" + pollId));
    }
}
