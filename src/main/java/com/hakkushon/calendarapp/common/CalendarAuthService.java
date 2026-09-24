package com.hakkushon.calendarapp.common;

/**
 * カレンダーに対する権限判定を集約するサービス。
 *
 * 「このユーザーはこのカレンダーのownerか」「メンバーか」という判定は
 * 各ドメイン(B・C・E)がそれぞれ実装するのではなく、必ずここを呼ぶこと。
 * 判定基準がバラつくと統合時にバグの温床になる。
 *
 * 実装はB(カレンダー管理担当)が最初に用意する。
 * calendar_members テーブルの role / status を見て判定する想定。
 */
public interface CalendarAuthService {

    /** 指定ユーザーが、指定カレンダーのownerかどうか */
    boolean isOwner(Long userId, Long calendarId);

    /** 指定ユーザーが、指定カレンダーの参加済み(accepted)メンバーかどうか */
    boolean isMember(Long userId, Long calendarId);
}
