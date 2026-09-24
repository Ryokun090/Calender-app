package com.hakkushon.calendarapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CalendarAppApplicationTests {

    @Test
    void contextLoads() {
        // Spring Bootのコンテキストが正常に起動すればテストは成功する。
        // DB接続を伴うため、application-local.yml を用意していないと失敗する点に注意。
    }
}
