package com.local_review_deal_sys.service.impl;

import com.local_review_deal_sys.entity.BlogComments;
import com.local_review_deal_sys.mapper.BlogCommentsMapper;
import com.local_review_deal_sys.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}

