package com.local_review_deal_sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.User;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IUserService extends IService<User> {

    Result sign();

    Result signCount();
}
