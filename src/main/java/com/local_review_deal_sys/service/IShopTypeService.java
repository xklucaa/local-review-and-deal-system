package com.local_review_deal_sys.service;

import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 */
public interface IShopTypeService extends IService<ShopType> {
    Result queryList();
}
