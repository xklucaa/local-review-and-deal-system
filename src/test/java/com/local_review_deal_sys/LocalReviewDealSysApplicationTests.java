package com.local_review_deal_sys;

import com.local_review_deal_sys.entity.Shop;
import com.local_review_deal_sys.service.impl.ShopServiceImpl;
import com.local_review_deal_sys.utils.CacheClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.local_review_deal_sys.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
class LocalReviewDealSysApplicationTests {

    @Resource
    private CacheClient cacheClient;

    @Resource
    private ShopServiceImpl shopService;
    @Test
    void testSaveShop() throws InterruptedException {
        shopService.saveShopToRedis(1L, 10L);
        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY +1L, shop, 10L, TimeUnit.SECONDS);
    }

}
