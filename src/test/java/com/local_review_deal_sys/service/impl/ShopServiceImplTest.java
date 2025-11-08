package com.local_review_deal_sys.service.impl;

import com.local_review_deal_sys.cache.strategy.CacheStrategy;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.Shop;
import com.local_review_deal_sys.mapper.ShopMapper;
import com.local_review_deal_sys.utils.CacheClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @InjectMocks  // automatically injects mocks into this instance
    private ShopServiceImpl shopService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private CacheClient cacheClient;

    @Mock
    private ShopMapper shopMapper;

    // Added: mocks for cache strategies
    @Mock
    private CacheStrategy logicalExpireStrategy;

    @Mock
    private CacheStrategy mutexStrategy;

    @BeforeEach
    void setUp() {
        // Manually build a map of strategies and inject it into the ShopServiceImpl
        Map<String, CacheStrategy> strategyMap = new HashMap<>();
        strategyMap.put("logicalExpireStrategy", logicalExpireStrategy);
        strategyMap.put("mutexStrategy", mutexStrategy);

        ReflectionTestUtils.setField(shopService, "cacheStrategies", strategyMap);
    }

    // ==================== Tests for queryById ====================

    @Test
    void queryById_ShouldReturnShopFromLogicalExpireStrategy_WhenFound() {
        // Given
        Long shopId = 1L;
        Shop expectedShop = new Shop();
        expectedShop.setId(shopId);
        expectedShop.setName("Test Shop");

        // Assume the logicalExpireStrategy.query successfully returns the shop
        when(logicalExpireStrategy.query(
                eq("cache:shop:"), eq(shopId), eq(Shop.class), any(), eq(30L), eq(TimeUnit.MINUTES)
        )).thenReturn(expectedShop);

        // When
        Result result = shopService.queryById(shopId);

        // Then
        assertTrue(result.getSuccess());
        assertEquals(expectedShop, result.getData());

        // Verify only logicalExpireStrategy.query was called
        verify(logicalExpireStrategy, times(1)).query(
                eq("cache:shop:"), eq(shopId), eq(Shop.class), any(), eq(30L), eq(TimeUnit.MINUTES)
        );
        verify(mutexStrategy, never()).query(anyString(), any(), any(), any(), anyLong(), any());
    }

    @Test
    void queryById_ShouldFallbackToMutex_WhenLogicalExpireReturnsNull() {
        // Given
        Long shopId = 1L;

        // logicalExpireStrategy returns null first
        when(logicalExpireStrategy.query(
                eq("cache:shop:"), eq(shopId), eq(Shop.class), any(), eq(30L), eq(TimeUnit.MINUTES)
        )).thenReturn(null);

        // Then fallback to mutexStrategy, which returns a valid shop
        Shop fallbackShop = new Shop();
        fallbackShop.setId(shopId);
        fallbackShop.setName("Fallback Shop");
        when(mutexStrategy.query(
                eq("cache:shop:"), eq(shopId), eq(Shop.class), any(), eq(30L), eq(TimeUnit.MINUTES)
        )).thenReturn(fallbackShop);

        // When
        Result result = shopService.queryById(shopId);

        // Then
        assertTrue(result.getSuccess());
        assertEquals(fallbackShop, result.getData());

        // Verify both strategies were called in order
        verify(logicalExpireStrategy, times(1)).query(
                eq("cache:shop:"), eq(shopId), eq(Shop.class), any(), eq(30L), eq(TimeUnit.MINUTES)
        );
        verify(mutexStrategy, times(1)).query(
                eq("cache:shop:"), eq(shopId), eq(Shop.class), any(), eq(30L), eq(TimeUnit.MINUTES)
        );
    }

    @Test
    void queryById_ShouldReturnFailResult_WhenBothStrategiesReturnNull() {
        // Given
        Long shopId = 1L;

        when(logicalExpireStrategy.query(
                eq("cache:shop:"), eq(shopId), eq(Shop.class), any(), eq(30L), eq(TimeUnit.MINUTES)
        )).thenReturn(null);

        when(mutexStrategy.query(
                eq("cache:shop:"), eq(shopId), eq(Shop.class), any(), eq(30L), eq(TimeUnit.MINUTES)
        )).thenReturn(null);

        // When
        Result result = shopService.queryById(shopId);

        // Then
        assertFalse(result.getSuccess());
        assertEquals("Error: Shop not found !", result.getErrorMsg());

        verify(logicalExpireStrategy, times(1)).query(
                eq("cache:shop:"), eq(shopId), eq(Shop.class), any(), eq(30L), eq(TimeUnit.MINUTES)
        );
        verify(mutexStrategy, times(1)).query(
                eq("cache:shop:"), eq(shopId), eq(Shop.class), any(), eq(30L), eq(TimeUnit.MINUTES)
        );
    }

    // ==================== Tests for update ====================

    @Test
    void update_ShouldUpdateDBAndDeleteCache_WhenValidShop() {
        // Given
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setName("Updated Shop");

        // When
        Result result = shopService.update(shop);

        // Then
        assertTrue(result.getSuccess());
        // Verify database update called once
        verify(shopMapper, times(1)).updateById(shop);
        // Verify Redis cache deletion called once
        verify(stringRedisTemplate, times(1)).delete("cache:shop:1");
    }

    @Test
    void update_ShouldReturnFail_WhenShopIdIsNull() {
        // Given
        Shop shop = new Shop(); // id == null

        // When
        Result result = shopService.update(shop);

        // Then
        assertFalse(result.getSuccess());
        assertEquals("Invalid id: Shop id cannot be null", result.getErrorMsg());
        // Verify no DB or Redis operations executed
        verify(shopMapper, never()).updateById(any());
        verify(stringRedisTemplate, never()).delete(anyString());
    }
}