package com.hakkushon.calendarapp.dto;

import java.util.HashMap;
import java.util.Map;

/**
 * 空き日程の○×回答フォーム。
 * キーは候補ID(poll_candidates.id)、値は"yes"または"no"。
 * 画面側は各候補ごとにラジオボタン name="responses[候補ID]" を並べる。
 */
public class RespondForm {

    private Map<Long, String> responses = new HashMap<>();

    public Map<Long, String> getResponses() {
        return responses;
    }

    public void setResponses(Map<Long, String> responses) {
        this.responses = responses;
    }
}
