package com.local_review_deal_sys.cache.strategy;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存策略统一接口
 * 让不同策略类通过统一方法对接 CacheClient
 */
public interface CacheStrategy {

    /**
     * 通用缓存查询接口
     *
     * @param keyPrefix  Redis key 前缀（例如 CACHE_SHOP_KEY）
     * @param id         实体 ID
     * @param type       返回对象类型
     * @param dbFallback 当缓存未命中时执行的数据库查询逻辑
     * @param time       缓存过期时间
     * @param unit       时间单位
     * @return 查询结果
     */
    <R, ID> R query(String keyPrefix, ID id, Class<R> type,
                    Function<ID, R> dbFallback, Long time, TimeUnit unit);
}