package com.hakkushon.calendarapp.service;

public enum OtpVerifyResult {
    SUCCESS,
    INVALID_CODE,
    EXPIRED,
    LOCKED,       // 誤入力の上限に達した
    NOT_FOUND     // そもそも有効なコードが発行されていない
}
