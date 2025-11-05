package com.local_review_deal_sys.service.impl;

import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.SeckillVoucher;
import com.local_review_deal_sys.entity.Voucher;
import com.local_review_deal_sys.mapper.VoucherMapper;
import com.local_review_deal_sys.service.ISeckillVoucherService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.local_review_deal_sys.utils.RedisConstants.SECKILL_STOCK_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link VoucherServiceImpl}
 */
class VoucherServiceImplTest {

    @InjectMocks
    private VoucherServiceImpl voucherService;

    @Mock
    private VoucherMapper voucherMapper;

    @Mock
    private ISeckillVoucherService seckillVoucherService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ----------------- queryVoucherOfShop -----------------

    @Test
    void testQueryVoucherOfShop_success() {
        Long shopId = 1L;
        Voucher v1 = new Voucher();
        v1.setId(100L);
        v1.setTitle("10% OFF");
        v1.setShopId(shopId);

        when(voucherMapper.queryVoucherOfShop(shopId)).thenReturn(Arrays.asList(v1));

        Result result = voucherService.queryVoucherOfShop(shopId);

        assertTrue(result.getSuccess());
        List<Voucher> vouchers = (List<Voucher>) result.getData();
        assertEquals(1, vouchers.size());
        assertEquals("10% OFF", vouchers.get(0).getTitle());

        verify(voucherMapper).queryVoucherOfShop(shopId);
    }

    @Test
    void testQueryVoucherOfShop_empty() {
        Long shopId = 2L;
        when(voucherMapper.queryVoucherOfShop(shopId)).thenReturn(Arrays.asList());

        Result result = voucherService.queryVoucherOfShop(shopId);

        assertTrue(result.getSuccess());
        assertTrue(((List<?>) result.getData()).isEmpty());
        verify(voucherMapper).queryVoucherOfShop(shopId);
    }

    // ----------------- addSeckillVoucher -----------------

    @Test
    void testAddSeckillVoucher_success() {
        Voucher voucher = new Voucher();
        voucher.setId(200L);
        voucher.setStock(100);
        voucher.setBeginTime(LocalDateTime.now());
        voucher.setEndTime(LocalDateTime.now().plusDays(1));

        // 模拟数据库 save 行为
        when(seckillVoucherService.save(any(SeckillVoucher.class))).thenReturn(true);
        doNothing().when(valueOperations).set(anyString(), anyString());


        voucherService.addSeckillVoucher(voucher);

        // 验证交互
        verify(seckillVoucherService, times(1)).save(any(SeckillVoucher.class));
        verify(valueOperations).set(eq(SECKILL_STOCK_KEY + voucher.getId()), eq("100"));
    }

    @Test
    void testAddSeckillVoucher_redisFailure_doesNotThrow() {
        Voucher voucher = new Voucher();
        voucher.setId(300L);
        voucher.setStock(50);
        voucher.setBeginTime(LocalDateTime.now());
        voucher.setEndTime(LocalDateTime.now().plusHours(3));

        when(seckillVoucherService.save(any(SeckillVoucher.class))).thenReturn(true);
        doThrow(new RuntimeException("Redis down")).when(valueOperations)
                .set(anyString(), anyString());

        assertDoesNotThrow(() -> voucherService.addSeckillVoucher(voucher));

        verify(seckillVoucherService).save(any(SeckillVoucher.class));
    }
}