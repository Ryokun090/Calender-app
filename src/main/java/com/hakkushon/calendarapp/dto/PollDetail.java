package com.hakkushon.calendarapp.dto;

import java.util.List;

import com.hakkushon.calendarapp.domain.SchedulePoll;

/** 候補調整画面(/polls/{id})に必要な情報をまとめた表示用オブジェクト。 */
public class PollDetail {

    private final SchedulePoll poll;
    private final List<CandidateSummary> candidates;
    private final int totalMembers;
    private final boolean canConfirm;

    public PollDetail(SchedulePoll poll, List<CandidateSummary> candidates, int totalMembers, boolean canConfirm) {
        this.poll = poll;
        this.candidates = candidates;
        this.totalMembers = totalMembers;
        this.canConfirm = canConfirm;
    }

    public SchedulePoll getPoll() {
        return poll;
    }

    public List<CandidateSummary> getCandidates() {
        return candidates;
    }

    public int getTotalMembers() {
        return totalMembers;
    }

    /** 確定ボタンを出してよいか(作成者またはカレンダーのオーナー)。 */
    public boolean isCanConfirm() {
        return canConfirm;
    }
}
