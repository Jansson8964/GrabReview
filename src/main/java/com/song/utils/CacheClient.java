package com.song.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.song.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.song.utils.RedisConstants.*;

@Component
@Slf4j
public class CacheClient {

    // 4.Query the cache with a specified key and deserialize it into a specific type. It's necessary to use a logical expiration approach to resolve cache penetration issues
    private final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    // 1.Serialize any Java object into JSON, store it in a String type key, and set a TTL (Time To Live) expiration time.
    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    // 2.Serialize any Java object into JSON and store it under a key of String type, with the ability to set a logical expiration time to address cache penetration issues.
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        // set logical expiration time
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        // write to redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    // 3.Query the cache using a specified key and deserialize it into a designated type, utilizing the caching of null values to solve the issue of cache penetration.
    public <R, ID> R queryWithCachePenetration(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        // cache Penetration 缓存穿透
        // hotspot invalid 缓存击穿
        String key = keyPrefix + id;
        // 1.check if id is existed in redis
        String json = stringRedisTemplate.opsForValue().get(key);
        // 2, if existed, return data directly
        if (StrUtil.isNotBlank(json)) {
            // deserialize json string to shop object
            return JSONUtil.toBean(json, type);
        }
        //  3.To check if the value retrieved from Redis is empty
        // If the retrieved value is an empty string, then it means that the data is cached empty.
        if (" ".equals(json)) {
            return null;
        }
        // 4.if not existed, query from database
        R r = dbFallback.apply(id);
        // 5.if not existed in database, If the value is not found, then write an empty string to Redis.
        if (r == null) {
            stringRedisTemplate.opsForValue().set(key, " ", CACHE_NULL_TTL, TimeUnit.MINUTES);
            return null;
        }
        // deserialize shop object to json string
        // 5. if existed in database, save to redis , set expire time
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(r), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 6. return data, data is a shop object
        return r;
    }

    public <R, ID> R queryWithMutex(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String LockKey = LOCK_SHOP_KEY + id;

        String Json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(Json)) {
            return JSONUtil.toBean(Json, type);
        }
        if (" ".equals(Json)) {
            return null;
        }
        boolean tryLock = tryLock(LockKey);
        // check if lock success
        try {
            if (tryLock) {
                // if lock success, query from database
                R r = dbFallback.apply(id);
                // if r is null, set empty string to redis
                if (r == null) {
                    stringRedisTemplate.opsForValue().set(key, " ", CACHE_NULL_TTL, TimeUnit.MINUTES);
                    return null;
                }
                // if shop is not null, set shop to redis
                stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(r), CACHE_SHOP_TTL, TimeUnit.MINUTES);
                return r;
            } else {
                // if lock failed, sleep 100ms, then retry
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // unlock
            unlock(LockKey);
        }
    }
    //  lock
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return flag != null && flag;
    }
    // unlock
    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
