package com.hakkushon.calendarapp.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * 空き日程の計算結果(1日につき1件)。時刻は「0時からの分」で持つ(24:00も表せるように)。
 */
public class FreeSlot {

    private static final String[] WEEK = {"月", "火", "水", "木", "金", "土", "日"};

    private LocalDate date;
    private int startMin;      // おすすめ開始
    private int endMin;        // おすすめ終了(開始 + 所要時間)
    private int rangeEndMin;   // 同じメンバーで集まれる時間帯の終わり(おすすめ終了以降)
    private int availableCount;
    private int totalCount;
    private List<String> unavailableNames;

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getStartMin() {
        return startMin;
    }

    public void setStartMin(int startMin) {
        this.startMin = startMin;
    }

    public int getEndMin() {
        return endMin;
    }

    public void setEndMin(int endMin) {
        this.endMin = endMin;
    }

    public int getRangeEndMin() {
        return rangeEndMin;
    }

    public void setRangeEndMin(int rangeEndMin) {
        this.rangeEndMin = rangeEndMin;
    }

    public int getAvailableCount() {
        return availableCount;
    }

    public void setAvailableCount(int availableCount) {
        this.availableCount = availableCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<String> getUnavailableNames() {
        return unavailableNames;
    }

    public void setUnavailableNames(List<String> unavailableNames) {
        this.unavailableNames = unavailableNames;
    }

    public boolean isEveryone() {
        return availableCount == totalCount;
    }

    /** 例: 「土」 */
    public String getWeekLabel() {
        return WEEK[date.getDayOfWeek().getValue() - 1];
    }

    /** 例: 「19:00〜21:00」 */
    public String getTimeLabel() {
        return label(startMin) + "〜" + label(endMin);
    }

    /** 例: 「19:00〜22:00」。おすすめより広く集まれる時間帯があるときだけ画面に出す。 */
    public String getRangeLabel() {
        return label(startMin) + "〜" + label(rangeEndMin);
    }

    public boolean isHasWiderRange() {
        return rangeEndMin > endMin;
    }

    private static String label(int minutes) {
        return String.format("%02d:%02d", minutes / 60, minutes % 60);
    }
}
