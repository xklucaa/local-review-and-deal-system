package com.local_review_deal_sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.local_review_deal_sys.dto.LoginFormDTO;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.User;

import javax.servlet.http.HttpSession;

/**
 * <p>
 *  服务类
 * </p>
 */
public interface IUserService extends IService<User> {
    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);

    Result sign();

    Result signCount();

}
