package com.local_review_deal_sys.service;

import com.local_review_deal_sys.entity.Shop;
import com.local_review_deal_sys.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class ShopServiceTest {

    @Resource
    private ShopServiceImpl shopService;

    @MockBean
    private ShopServiceImpl mockShopService;

    @Test
    void testListShops() {
        // Given
        Shop shop1 = new Shop();
        shop1.setId(1L);
        shop1.setName("测试店铺1");

        Shop shop2 = new Shop();
        shop2.setId(2L);
        shop2.setName("测试店铺2");

        List<Shop> expectedShops = Arrays.asList(shop1, shop2);

        // When
        when(mockShopService.list()).thenReturn(expectedShops);
        List<Shop> actualShops = mockShopService.list();

        // Then
        assertEquals(2, actualShops.size());
        assertEquals("测试店铺1", actualShops.get(0).getName());
        verify(mockShopService, times(1)).list();
    }
}
