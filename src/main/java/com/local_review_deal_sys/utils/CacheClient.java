package com.local_review_deal_sys.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.local_review_deal_sys.utils.RedisConstants.*;

@Slf4j
@Component
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;
    //    build Tread pool
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void set(String key, Object value, Long time, TimeUnit unit) {
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
//        set logic expire
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(time));
//        write into Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
    }

    public <R, ID> R queryWithPassThrough(Long time, TimeUnit unit,
                                          String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback) {
        String key = keyPrefix + id;
//        1.Search shop cache from Redis
        String json = stringRedisTemplate.opsForValue().get(key);
//        2.Judge if exist in the cache
        if (StrUtil.isNotBlank(json)) {
            //        3.If exists, return
            return JSONUtil.toBean(json, type);
        }
//        Judge if the target is null
        if (json != null) {
//            Return error message
            return null;
        }
        //        4.If not exist, search from database by id
        R r = dbFallback.apply(id);
        //        5.If still not exists, return false
        if (r == null) {
//            Write the null into Redis
            stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//            Return error message
            return null;
        }
        //        6.If exists in database, return data and write into Redis
        this.set(key, r, time, unit);


//        7.Return
        return r;
    }


    public <R, ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
//        1.Search shop cache from Redis
        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        2.Judge if exist in the cache
        if (StrUtil.isBlank(shopJson)) {
            //        3.If exists, return
            return null;
        }

        //        4. If targeted, the JSON needs to be deserialized into an object.
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        R r = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //        5. Judge if expired
        if (expireTime == null) {
            return null;
        } else if (expireTime.isAfter(LocalDateTime.now())) {
            return r;
        }
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            //        5.1 If not expired, return shop info
//            return r;
//        } else if (expireTime == null) {
//            return null;
//        }
        //        5.2 If expired, cache rebuild is needed
        //        6 Cache rebuild
        //        6.1 Get mutex
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);

        //        6.2 Judge if the lock is successfully obtained
        if (isLock) {
            //        6.3 If successfully obtained, create independent thread and build cache
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
//                    search in the DB
                    R r1 = dbFallback.apply(id);
//                    write into Redis
                    this.setWithLogicalExpire(key, r1, time, unit);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        //        6.4 If unsuccessful, return expired shop info
        return r;
    }


    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        // 1.Search shop cache from Redis
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 2.Judge if exist in the cache
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.If exists, return
            return JSONUtil.toBean(shopJson, type);
        }
        // Judge if the target is null
        if (shopJson != null) {
            // Return error message
            return null;
        }

        // 4.Implement cache rebuilding
        // 4.1.Get mutex
        String lockKey = LOCK_SHOP_KEY + id;
        R r = null;
        try {
            boolean isLock = tryLock(lockKey);
            // 4.2.Judge if the lock is successfully obtained
            if (!isLock) {
                // 4.3.If unsuccessful, return error message
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }
            // 4.4.If successful, create independent thread and build cache
            r = dbFallback.apply(id);
            // 5.If not exists, return error message
            if (r == null) {
                // Write the null into Redis
                stringRedisTemplate.opsForValue().set(key, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
                // Return error message
                return null;
            }
            // 6.If exists in database, return data and write into Redis
            this.set(key, r, time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // 7.Release mutex
            unLock(lockKey);
        }
        // 8.return
        return r;
    }

    //    Turn on Lock
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        Ensure the flag is not null
        return BooleanUtil.isTrue(flag);
    }

    //    Unlock
    private void unLock(String key) {
//        Delete key
        stringRedisTemplate.delete(key);
    }
}



