package com.local_review_deal_sys.cache.strategy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class StrategySelector {
    @Resource
    private Map<String, CacheStrategy> cacheStrategies;

    @Value("${cache.strategy:logicalExpireStrategy}")
    private String defaultStrategy;

    public CacheStrategy select(String strategyName) {
        return cacheStrategies.getOrDefault(strategyName, cacheStrategies.get(defaultStrategy));
    }
}
