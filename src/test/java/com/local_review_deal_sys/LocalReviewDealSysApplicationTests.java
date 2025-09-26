package com.local_review_deal_sys;

import com.local_review_deal_sys.service.impl.ShopServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class LocalReviewDealSysApplicationTests {
    @Resource
    private ShopServiceImpl shopService;
    @Test
    void testSaveShop() throws InterruptedException {
        shopService.saveShopToRedis(1L, 10L);
    }

}
