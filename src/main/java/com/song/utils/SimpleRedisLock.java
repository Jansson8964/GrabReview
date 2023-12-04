package com.song.utils;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.BooleanUtil;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {
    private static final String LOCK_PREFIX = "lock:";
    private String name; // lock name
    private StringRedisTemplate stringRedisTemplate;
    // private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSeconds) {
        // get current thread id
        // String threadId = ID_PREFIX + Thread.currentThread().getId();
        long threadId = Thread.currentThread().getId();
        // get lock
        /**
         * Unboxing happens when an object of a wrapper class (like Integer, Boolean, etc.)
         * is converted back to its corresponding primitive type (like int, boolean, etc.).
         * If the wrapper object is null, trying to unbox it will throw a NullPointerException.
         */
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(LOCK_PREFIX + name, threadId+"", timeoutSeconds, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(success);
    }

    @Override
    public void unlock() {
        // 1. get thread id
        // String threadId = ID_PREFIX + Thread.currentThread().getId();
        // long threadId = Thread.currentThread().getId();
        //  2.check if the id in redis is equal to the current thread id
        //  String id = stringRedisTemplate.opsForValue().get(LOCK_PREFIX + name);
        // 3. if equal, delete the lock
//        if () {
//            stringRedisTemplate.delete(LOCK_PREFIX + name);
//        }
        stringRedisTemplate.delete(LOCK_PREFIX + name);
    }
}
