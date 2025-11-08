package com.local_review_deal_sys.cache.strategy.impl;

import com.local_review_deal_sys.cache.strategy.CacheStrategy;
import com.local_review_deal_sys.utils.CacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存穿透策略（空值缓存防止反复查询）
 */
@Component("passThroughStrategy")
public class PassThroughStrategy implements CacheStrategy {

    @Autowired
    private CacheClient cacheClient;

    @Override
    public <R, ID> R query(String keyPrefix, ID id, Class<R> type,
                           Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        return cacheClient.queryWithPassThrough(time, unit, keyPrefix, id, type, dbFallback);
    }
}