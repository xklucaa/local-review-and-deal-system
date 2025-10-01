package com.local_review_deal_sys.service.impl;

import cn.hutool.json.JSONUtil;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.Shop;
import com.local_review_deal_sys.mapper.ShopMapper;
import com.local_review_deal_sys.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.local_review_deal_sys.utils.CacheClient;
import com.local_review_deal_sys.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.local_review_deal_sys.utils.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

//    @Resource
//    private DataSource dataSource;

    @Resource
    private CacheClient cacheClient;


    @Override
    public Result queryById(Long id) {
//        Resolve cache penetration
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_TTL, TimeUnit.MINUTES, CACHE_SHOP_KEY, id, Shop.class,this::getById);

//
//        Use logic expire to solve the problem of cache penetration
        Shop shop = cacheClient
                .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if (shop == null) {
//            Use mutex to solve the problem of cache penetration
            shop = cacheClient.queryWithMutex(CACHE_SHOP_KEY, id, Shop.class,this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
            if (shop == null) {
                return Result.fail("Error: Shop not found !");
            }
        }
//        7.Return
        return Result.ok(shop);

    }


    public void saveShopToRedis(Long id, long expireSeconds) {
//        1.Search shop data
        Shop shop = getById(id);
//        Thread.sleep(200);
//        2.Package the expiring time
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//        3.Write into Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("Invalid id: Shop id cannot be null");
        }
//        1.Update database
        updateById(shop);
//        2.Delete cache
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

}
