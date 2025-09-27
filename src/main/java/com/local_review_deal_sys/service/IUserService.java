package com.local_review_deal_sys.service;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.IService;
import com.local_review_deal_sys.dto.LoginFormDTO;
import com.local_review_deal_sys.dto.Result;
import com.local_review_deal_sys.entity.User;
import com.local_review_deal_sys.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

@Service

public interface IUserService extends IService<User> {


    Result sendCode(String phone, HttpSession session);


}
