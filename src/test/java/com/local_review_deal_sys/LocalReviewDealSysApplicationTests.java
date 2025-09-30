package com.local_review_deal_sys;

import com.local_review_deal_sys.entity.Shop;
import com.local_review_deal_sys.service.impl.ShopServiceImpl;
import com.local_review_deal_sys.utils.CacheClient;
import com.local_review_deal_sys.utils.UserHolder;
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
        Long id= UserHolder.getUser().getId();
        System.out.println(id);
    }

}
