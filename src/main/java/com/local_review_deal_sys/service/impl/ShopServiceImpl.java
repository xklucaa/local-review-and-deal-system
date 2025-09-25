package com.local_review_deal_sys.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.Shop;
import com.local_review_deal_sys.mapper.ShopMapper;
import com.local_review_deal_sys.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.local_review_deal_sys.utils.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    private final StringRedisTemplate stringRedisTemplate;

    public ShopServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Result queryById(Long id) {
//        If Use pass through, the code below with annotation is unnecessary
//        String shopKey = CACHE_SHOP_KEY + id;
////        1.Search shop cache from Redis
//        String shopJson= stringRedisTemplate.opsForValue().get(shopKey);
////        2.Judge if exist in the cache
//        if (StrUtil.isNotBlank(shopJson)){
//            //        3.If exists, return
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return Result.ok(shop);
//        }
////        Judge if the target is null
//        if (shopJson != null){
////            Return error message
//            return Result.fail("Shop not found");
//        }
//            //        4.If not exist, search from database by id
//            Shop shop = getById(id);
//            //        5.If still not exists, return false
//            if (shop == null) {
////            Write the null into Redis
//                stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
////            Return error message
//                return Result.fail("Shop not found");
//            }
//                //        6.If exists in database, return data and write into Redis
//                stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);

//        Cache penetration
//        Shop shop = queryWithPassThrough(id);

//        Use mutex to solve the problem of cache penetration
        Shop shop = queryWithMutex(id);
        if (shop == null){
            return Result.fail("Error: Shop not found !");
        }
//        7.Return
            return Result.ok(shop);

    }

//  Implement with mutex
    public Shop queryWithMutex(Long id){
        String shopKey = CACHE_SHOP_KEY + id;
//        1.Search shop cache from Redis
        String shopJson= stringRedisTemplate.opsForValue().get(shopKey);
//        2.Judge if exist in the cache
        if (StrUtil.isNotBlank(shopJson)){
            //        3.If exists, return
            return JSONUtil.toBean(shopJson, Shop.class);
        }
//        Judge if the target is null
        if (shopJson != null){
//            Return error message
            return null;
        }
        // 4.Implement cache rebuild
        // 4.1 Create a mutex
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);
        // 4.2 Judge if successful
        // 4.3 If unsuccessful, sleep and retry
        Shop shop = null;
        try {
            if (!isLock){
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 4.4 If successful, search in the database with id
            shop = getById(id);
            //Simulate rebuilding time
//            Thread.sleep(200);
            // 5.If still not exists, return false
            if (shop == null) {
    //            Write the null into Redis
                stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
    //            Return error message
                return null;
            }
            //        6.If exists in database, return data and write into Redis
            stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            //        7.Release the mutex
            unLock(lockKey);

        }
//        8.Return
        return shop;
    }

//  Implement with  Lock
    public Shop queryWithPassThrough(Long id){
        String shopKey = CACHE_SHOP_KEY + id;
//        1.Search shop cache from Redis
        String shopJson= stringRedisTemplate.opsForValue().get(shopKey);
//        2.Judge if exist in the cache
        if (StrUtil.isNotBlank(shopJson)){
            //        3.If exists, return
            return JSONUtil.toBean(shopJson, Shop.class);
        }
//        Judge if the target is null
        if (shopJson != null){
//            Return error message
            return null;
        }
        //        4.If not exist, search from database by id
        Shop shop = getById(id);
        //        5.If still not exists, return false
        if (shop == null) {
//            Write the null into Redis
            stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//            Return error message
            return null;
        }
        //        6.If exists in database, return data and write into Redis
        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);


//        7.Return
        return shop;
    }

//    Turn on Lock
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//        Ensure the flag is not null
        return BooleanUtil.isTrue(flag);
    }

//    Unlock
    private void unLock(String key) {
//        Delete key
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null){
            return Result.fail("Invalid id: Shop id cannot be null");
        }
//        1.Update database
        updateById(shop);
//        2.Delete cache
        stringRedisTemplate.delete(CACHE_SHOP_KEY + id);
        return Result.ok();
    }

}
