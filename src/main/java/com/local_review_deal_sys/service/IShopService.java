package com.local_review_deal_sys.service;

import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IShopService extends IService<Shop> {
    Result queryById(Long id);

    Result update(Shop shop);
}
