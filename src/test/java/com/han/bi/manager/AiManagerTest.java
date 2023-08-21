package com.han.bi.manager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class AiManagerTest {

    @Resource
    private AiManager aiManager;

    @Test
    void doChar() {
        String message = "分析需求：\n" +
                "分析网站用户趋势\n" +
                "数据：\n" +
                "日期,用户数\n" +
                "1号,15\n" +
                "2号,20\n" +
                "3号,25\n" +
                "4号,25";
        System.out.println(aiManager.doChar(1688102204835201025L, message));
    }
}