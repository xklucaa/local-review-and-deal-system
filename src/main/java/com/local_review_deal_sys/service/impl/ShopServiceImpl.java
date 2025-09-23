package com.local_review_deal_sys.service.impl;

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
        String shopKey = CACHE_SHOP_KEY + id;
//        1.Search shop cache from Redis
        String shopJson= stringRedisTemplate.opsForValue().get(shopKey);
//        2.Judge if exist in the cache
        if (StrUtil.isNotBlank(shopJson)){
            //        3.If exists, return
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
//        Judge if the target is null
        if (shopJson != null){
//            Return error message
            return Result.fail("Shop not found");
        }
            //        4.If not exist, search from database by id
            Shop shop = getById(id);
            //        5.If still not exists, return false
            if (shop == null) {
//            Write the null into Redis
                stringRedisTemplate.opsForValue().set(shopKey, "", CACHE_NULL_TTL, TimeUnit.MINUTES);
//            Return error message
                return Result.fail("Shop not found");
            }
                //        6.If exists in database, return data and write into Redis
                stringRedisTemplate.opsForValue().set(shopKey, JSONUtil.toJsonStr(shop), CACHE_SHOP_TTL, TimeUnit.MINUTES);


//        7.Return
            return Result.ok(shop);

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
