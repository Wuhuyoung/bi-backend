package com.han.bi.manager;

import com.han.bi.common.ErrorCode;
import com.han.bi.exception.BusinessException;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 提供 RedisLimiter 基础限流服务
 */
@Service
public class RedisLimiterManager {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 限流操作
     * @param key 区分不同的限流器，比如不同的用户 id 应该分别统计
     * @param rate
     * @param rateInterval
     * @param rateIntervalUnit
     */
    public void doLimit(String key, int rate, int rateInterval, RateIntervalUnit rateIntervalUnit) {
        // 创建限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);
        rateLimiter.trySetRate(RateType.OVERALL, rate, rateInterval, rateIntervalUnit);

        // 每当一个操作到来，请求一个令牌
        boolean success = rateLimiter.tryAcquire(1);
        if (!success) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST);
        }
    }
}
