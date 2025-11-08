package com.local_review_deal_sys.cache.strategy.impl;

import com.local_review_deal_sys.cache.strategy.CacheStrategy;
import com.local_review_deal_sys.utils.CacheClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 逻辑过期策略（热点数据异步重建）
 */
@Component("logicalExpireStrategy")
public class LogicalExpireStrategy implements CacheStrategy {

    @Autowired
    private CacheClient cacheClient;

    @Override
    public <R, ID> R query(String keyPrefix, ID id, Class<R> type,
                           Function<ID, R> dbFallback, Long time, TimeUnit unit) {
        return cacheClient.queryWithLogicalExpire(keyPrefix, id, type, dbFallback, time, unit);
    }
}