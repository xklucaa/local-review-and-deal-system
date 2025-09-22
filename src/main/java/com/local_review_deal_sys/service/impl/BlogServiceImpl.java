package com.local_review_deal_sys.service.impl;

import com.local_review_deal_sys.entity.Blog;
import com.local_review_deal_sys.mapper.BlogMapper;
import com.local_review_deal_sys.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

}
