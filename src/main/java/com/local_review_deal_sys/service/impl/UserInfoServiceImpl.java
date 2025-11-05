package com.local_review_deal_sys.service.impl;

import com.local_review_deal_sys.entity.UserInfo;
import com.local_review_deal_sys.mapper.UserInfoMapper;
import com.local_review_deal_sys.service.IUserInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements IUserInfoService {

}
