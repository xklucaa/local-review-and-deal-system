package com.local_review_deal_sys.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.ShopType;
import com.local_review_deal_sys.mapper.ShopTypeMapper;
import com.local_review_deal_sys.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.local_review_deal_sys.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;


@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    private final StringRedisTemplate stringRedisTemplate;
//    private final IShopTypeService iShopTypeService;

    public ShopTypeServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;

    }

    @Override
    public Result queryList() {
        String typeKey = CACHE_SHOP_TYPE_KEY;
//     1.Search type cache from the Redis
        String typeJson = stringRedisTemplate.opsForValue().get(typeKey);
//     2.Judge if exists in cache
        if (StrUtil.isNotBlank(typeJson)) {

//     3.If exists, return
            List<ShopType> shopTypeList = JSONUtil.toList(JSONUtil.parseArray(typeJson), ShopType.class);
            return Result.ok(shopTypeList);
        }
//     4.If not exists, search from the database
            List<ShopType> shopTypeList = this.query().orderByAsc("sort").list();

//     5.If still not exists, return false
            if (shopTypeList.isEmpty()) {
                return Result.fail("Shop type list is not found");
            }
//     6.If exists in database, write into Redis
                stringRedisTemplate.opsForValue().set(typeKey, JSONUtil.toJsonStr(shopTypeList));

//     7.Return
            return Result.ok(shopTypeList);
        }


}
