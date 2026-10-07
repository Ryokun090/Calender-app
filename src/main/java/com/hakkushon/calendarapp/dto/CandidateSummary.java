package com.hakkushon.calendarapp.dto;

import java.time.LocalDateTime;

/**
 * 候補日程1件分の画面表示用データ(DBの行そのものではなく、集計結果)。
 * 候補一覧画面で「○3人 / ×1人」のような表示と、自分の回答の初期選択に使う。
 */
public class CandidateSummary {

    private final Long candidateId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final long yesCount;
    private final long noCount;
    private final String myResponse; // "yes" / "no" / null(未回答)

    public CandidateSummary(Long candidateId, LocalDateTime startTime, LocalDateTime endTime,
                             long yesCount, long noCount, String myResponse) {
        this.candidateId = candidateId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.yesCount = yesCount;
        this.noCount = noCount;
        this.myResponse = myResponse;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public long getYesCount() {
        return yesCount;
    }

    public long getNoCount() {
        return noCount;
    }

    public String getMyResponse() {
        return myResponse;
    }

    public boolean isMyResponseYes() {
        return "yes".equals(myResponse);
    }

    public boolean isMyResponseNo() {
        return "no".equals(myResponse);
    }
}
