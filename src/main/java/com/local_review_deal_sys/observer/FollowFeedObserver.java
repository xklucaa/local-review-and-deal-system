package com.local_review_deal_sys.observer;

import com.local_review_deal_sys.entity.Blog;
import com.local_review_deal_sys.entity.Follow;
import com.local_review_deal_sys.service.IFollowService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;


@Component
public class FollowFeedObserver implements BlogObserver {

    @Resource
    private IFollowService followService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void onBlogPublished(Blog blog) {
        // 查询关注该用户的所有粉丝
        List<Follow> follows = followService.query()
                .eq("follow_user_id", blog.getUserId())
                .list();

        // 将新博客推送给每个粉丝
        for (Follow follow : follows) {
            Long userId = follow.getUserId();
            String key = "feed:" + userId;
            stringRedisTemplate.opsForZSet().add(key, blog.getId().toString(), System.currentTimeMillis());
        }
    }
}

