package com.local_review_deal_sys.service.impl;

import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.Shop;
import com.local_review_deal_sys.mapper.ShopMapper;
import com.local_review_deal_sys.utils.CacheClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShopServiceImplTest {

    @InjectMocks  // 自动注入 mocks 到这个实例中
    private ShopServiceImpl shopService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private CacheClient cacheClient;

    @Mock
    private ShopMapper shopMapper; // MyBatis-Plus Mapper 也会被 mock

    @Mock
    private ValueOperations<String, String> valueOps;

    @BeforeEach
    void setUp() {
        // 模拟 stringRedisTemplate.opsForValue() 返回 valueOps
//        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ==================== 测试 queryById ====================

    @Test
    void queryById_ShouldReturnShopFromCacheClient_WhenFound() {
        // Given
        Long shopId = 1L;
        Shop expectedShop = new Shop();
        expectedShop.setId(shopId);
        expectedShop.setName("测试店铺");

        // 假设 cacheClient.queryWithLogicalExpire 成功返回店铺
        when(cacheClient.queryWithLogicalExpire(
                anyString(), eq(shopId), eq(Shop.class), any(), anyLong(), any()))
                .thenReturn(expectedShop);

        // When
        Result result = shopService.queryById(shopId);

        // Then
        assertTrue(result.getSuccess());
        assertEquals(expectedShop, result.getData());
        verify(cacheClient, times(1)).queryWithLogicalExpire(
                eq("cache:shop:"), eq(shopId), eq(Shop.class), any(), eq(30L), eq(java.util.concurrent.TimeUnit.MINUTES));
    }

    @Test
    void queryById_ShouldFallbackToMutex_WhenLogicalExpireReturnsNull() {
        // Given
        Long shopId = 1L;

        // 第一次调用返回 null（逻辑过期失效）
        when(cacheClient.queryWithLogicalExpire(
                anyString(), eq(shopId), eq(Shop.class), any(), anyLong(), any()))
                .thenReturn(null);

        // 回退到互斥锁模式，假设这次找到了
        Shop fallbackShop = new Shop();
        fallbackShop.setId(shopId);
        fallbackShop.setName("回退店铺");
        when(cacheClient.queryWithMutex(
                anyString(), eq(shopId), eq(Shop.class), any(), anyLong(), any()))
                .thenReturn(fallbackShop);

        // When
        Result result = shopService.queryById(shopId);

        // Then
        assertTrue(result.getSuccess());
        assertEquals(fallbackShop, result.getData());
        verify(cacheClient).queryWithMutex(
                eq("cache:shop:"), eq(shopId), eq(Shop.class), any(), eq(30L), eq(java.util.concurrent.TimeUnit.MINUTES));
    }

    @Test
    void queryById_ShouldReturnFailResult_WhenBothCacheStrategiesReturnNull() {
        // Given
        Long shopId = 1L;

        when(cacheClient.queryWithLogicalExpire(
                anyString(), eq(shopId), eq(Shop.class), any(), anyLong(), any()))
                .thenReturn(null);

        when(cacheClient.queryWithMutex(
                anyString(), eq(shopId), eq(Shop.class), any(), anyLong(), any()))
                .thenReturn(null);

        // When
        Result result = shopService.queryById(shopId);

        // Then
        assertFalse(result.getSuccess());
        assertEquals("Error: Shop not found !", result.getErrorMsg());
    }

    // ==================== 测试 update ====================

    @Test
    void update_ShouldUpdateDBAndDeleteCache_WhenValidShop() {
        // Given
        Shop shop = new Shop();
        shop.setId(1L);
        shop.setName("更新后的店铺");

        // When
        Result result = shopService.update(shop);

        // Then
        assertTrue(result.getSuccess());
        // 验证 updateById 被调用了一次
        verify(shopMapper, times(1)).updateById(shop);
        // 验证 Redis 删除缓存被调用
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
        // 确保没有调用数据库或 Redis
        verify(shopMapper, never()).updateById(any());
        verify(stringRedisTemplate, never()).delete(anyString());
    }

}