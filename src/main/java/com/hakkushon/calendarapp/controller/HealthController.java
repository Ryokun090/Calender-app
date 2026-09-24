package com.hakkushon.calendarapp.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 起動確認用。/health にアクセスして "OK" が返れば
 * 環境構築が正しく完了している。
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}
