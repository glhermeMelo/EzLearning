package com.ezlearning.config;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingInterceptor {

    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
    private static final long MAX_REQUESTS = 5;
    private static final long TIME_WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;

    public RateLimitingInterceptor(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isRateLimited(String userId) {
        var key = RATE_LIMIT_PREFIX + userId;
        var count = redisTemplate.opsForValue().get(key);
        if (count == null) {
            return false;
        }
        return Long.parseLong(count) >= MAX_REQUESTS;
    }

    public void incrementCounter(String userId) {
        var key = RATE_LIMIT_PREFIX + userId;
        redisTemplate.opsForValue().increment(key, 1);
        redisTemplate.expire(key, TIME_WINDOW_SECONDS, TimeUnit.SECONDS);
    }
}
