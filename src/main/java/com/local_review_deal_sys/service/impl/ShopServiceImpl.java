package com.local_review_deal_sys.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
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
import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.local_review_deal_sys.utils.RedisConstants.*;

@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private DataSource dataSource;

    @Resource
    private CacheClient cacheClient;

//    public ShopServiceImpl(StringRedisTemplate stringRedisTemplate, DataSource dataSource) {
//        this.stringRedisTemplate = stringRedisTemplate;
//        this.dataSource = dataSource;
//    }

    @Override
    public Result queryById(Long id) {
//        Resolve cache penetration
//        Shop shop = cacheClient.queryWithPassThrough(CACHE_SHOP_TTL, TimeUnit.MINUTES, CACHE_SHOP_KEY, id, Shop.class,this::getById);

//        Use mutex to solve the problem of cache penetration
//        Shop shop = queryWithMutex(id);

//        Use logic expire to solve the problem of cache penetration
        Shop shop = cacheClient
                .queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class,this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        if (shop == null){
            return Result.fail("Error: Shop not found !");
        }
//        7.Return
            return Result.ok(shop);

    }

//    build Tread pool
    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public Shop queryWithLogicalExpire(Long id){
        String shopKey = CACHE_SHOP_KEY + id;
//        1.Search shop cache from Redis
        String shopJson= stringRedisTemplate.opsForValue().get(shopKey);
//        2.Judge if exist in the cache
        if (StrUtil.isBlank(shopJson)){
            //        3.If exists, return
            return null;
        }

        //        4. If targeted, the JSON needs to be deserialized into an object.
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        //        5. Judge if expired
        if (expireTime.isAfter(LocalDateTime.now())) {


            //        5.1 If not expired, return shop info
            return shop;
        }
        //        5.2 If expired, cache rebuild is needed
        //        6 Cache rebuild
        //        6.1 Get mutex
        String lockKey = LOCK_SHOP_KEY + id;
        boolean isLock = tryLock(lockKey);

        //        6.2 Judge if the lock is successfully obtained
        if (isLock){
        //        6.3 If successfully obtained, create independent thread and build cache
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    this.saveShopToRedis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        //        6.4 If unsuccessful, return expired shop info
        return shop;
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
//    public Shop queryWithPassThrough(Long id){
//        String shopKey = CACHE_SHOP_KEY + id;
////        1.Search shop cache from Redis
//        String shopJson= stringRedisTemplate.opsForValue().get(shopKey);
////        2.Judge if exist in the cache
//        if (StrUtil.isNotBlank(shopJson)){
//            //        3.If exists, return
//            return JSONUtil.toBean(shopJson, Shop.class);
//        }
////        Judge if the target is null
//        if (shopJson != null){
////            Return error message
//            return null;
//        }
//        //        4.If not exist, search from database by id
//        Shop shop = getById(id);
//        //        5.If still not exists, return false
//        if (shop == null) {
////            Write the null into Redis
//            stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
////            Return error message
//            return null;
//        }
//        //        6.If exists in database, return data and write into Redis
//        stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);
//
//
////        7.Return
//        return shop;
//    }



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

    public void saveShopToRedis(Long id , long expireSeconds) throws InterruptedException {
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
