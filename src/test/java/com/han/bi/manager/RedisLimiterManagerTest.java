package com.han.bi.manager;

import com.han.bi.exception.ThrowUtils;
import org.junit.jupiter.api.Test;
import org.redisson.api.RateIntervalUnit;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class RedisLimiterManagerTest {

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Test
    void doLimit() throws InterruptedException {
        for (int i = 0; i < 2; i++) {
            String userKey = "1";
            redisLimiterManager.doLimit(userKey, 2, 1, RateIntervalUnit.SECONDS);
            System.out.println("获取到令牌");
        }
//        Thread.sleep(1000);
//        System.out.println("1秒后");
//        for (int i = 0; i < 5; i++) {
//            String userKey = "1";
//            redisLimiterManager.doLimit(userKey, 2, 1, RateIntervalUnit.SECONDS);
//            System.out.println("获取到令牌");
//        }
    }
}